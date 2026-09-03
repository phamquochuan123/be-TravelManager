package com.example.travelManager.filter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.travelManager.domain.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limit tối giản cho các endpoint public dễ bị lạm dụng.
 *
 * Vì sao cần: hệ thống đang chạy public trên Internet, và /send-otp + /send-reset-otp
 * gửi mail qua Gmail App Password thật — bị spam là khoá luôn tài khoản Gmail.
 * /login và /verify-otp thì bị brute-force OTP/mật khẩu không giới hạn.
 *
 * Chạy trước cả filter chain của Spring Security (HIGHEST_PRECEDENCE) để chặn từ sớm,
 * không tốn query DB. Là @Component nên Spring Boot tự đăng ký vào servlet filter chain —
 * KHÔNG thêm lại vào SecurityConfig, thêm 2 lần thì mỗi request bị đếm 2 lượt.
 * Lưu trong bộ nhớ tiến trình — đủ cho một instance; nếu sau này scale nhiều instance
 * thì thay bằng Redis.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Giới hạn theo từng nhóm endpoint: số request tối đa trong khoảng thời gian. */
    private record Limit(int maxRequests, long windowMillis) {}

    private static final Limit OTP_SEND_LIMIT = new Limit(3, 10 * 60 * 1000L);   // 3 lần / 10 phút
    private static final Limit AUTH_LIMIT     = new Limit(10, 5 * 60 * 1000L);   // 10 lần / 5 phút

    private static final Map<String, Limit> LIMITED_PATHS = Map.of(
            "/send-otp",        OTP_SEND_LIMIT,
            "/send-reset-otp",  OTP_SEND_LIMIT,
            "/login",           AUTH_LIMIT,
            "/verify-otp",      AUTH_LIMIT,
            "/reset-password",  AUTH_LIMIT);

    /** key = "path|định danh" (IP hoặc email) → mốc thời gian các request gần đây. */
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Dọn rác định kỳ để map không phình vô hạn khi chạy lâu ngày. */
    private volatile long lastCleanup = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL_MILLIS = 10 * 60 * 1000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Limit limit = LIMITED_PATHS.get(request.getServletPath());
        if (limit == null || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        cleanupIfDue();

        String path = request.getServletPath();
        if (isOverLimit(path + "|ip|" + clientIp(request), limit)) {
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * IP thật của client. Bắt buộc đọc X-Forwarded-For trước: ứng dụng chạy sau
     * nginx + Cloudflare nên getRemoteAddr() luôn là IP nội bộ của docker network,
     * dùng nó thì cả Internet chung một hạn mức.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isOverLimit(String key, Limit limit) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > limit.windowMillis()) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit.maxRequests()) {
                return true;
            }
            timestamps.addLast(now);
            return false;
        }
    }

    private void cleanupIfDue() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        lastCleanup = now;
        long maxWindow = OTP_SEND_LIMIT.windowMillis();
        hits.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                Long last = timestamps.peekLast();
                return last == null || now - last > maxWindow;
            }
        });
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        MAPPER.writeValue(response.getWriter(),
                new ErrorResponse(429, "Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút."));
    }
}
