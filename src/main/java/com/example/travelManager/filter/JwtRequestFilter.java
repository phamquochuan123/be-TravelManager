package com.example.travelManager.filter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.travelManager.domain.response.ErrorResponse;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtRequestFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    private static final List<String> PUBLIC_URLS = List.of("/login", "/register", "/send-reset-otp", "/reset-password",
            "/logout", "/verify-otp", "/send-otp", "/admin/setup", "/auth/google");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if (PUBLIC_URLS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = null;
        String email = null;

        // 1:: check the authorization header
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
        }

        // 2. if not found in header , check cookies
        if (jwt == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // 3. validate the token and set security context
        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                email = jwtUtil.extractEmail(jwt);

                if (email != null && !jwtUtil.isTokenExpired(jwt)) {
                    var userOpt = userRepository.findByEmail(email);

                    // Tài khoản đã bị xoá khỏi DB — token cũ không được phép dùng tiếp.
                    // (Trước đây nhánh này rơi thẳng xuống phần authenticate bên dưới.)
                    if (userOpt.isEmpty()) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Kiểm tra tài khoản có bị khoá không
                    if (Boolean.FALSE.equals(userOpt.get().getIsActive())) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        String reason = userOpt.get().getLockReason();
                        String message = "Tài khoản đã bị khoá" + (reason != null && !reason.isBlank() ? ": " + reason : "");
                        MAPPER.writeValue(response.getWriter(), new ErrorResponse(403, message));
                        return;
                    }

                    // Quyền lấy từ DB, KHÔNG lấy từ claim "roles" trong token: token sống 10 tiếng,
                    // nếu tin claim thì ADMIN hạ quyền một STAFF xong người đó vẫn giữ quyền cũ
                    // tới khi token hết hạn. User đã query sẵn ở trên nên không tốn thêm query nào.
                    var role = userOpt.get().getRole();
                    // Cùng quy ước với AppUserDetailsService: "ROLE_" + tên role viết hoa.
                    List<SimpleGrantedAuthority> authorities = (role == null || role.getName() == null)
                            ? List.of()
                            : List.of(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }

            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                // Covers: Expired, Malformed, Signature, Unsupported, blank/empty token
            }
        }

        filterChain.doFilter(request, response);
    }

}
