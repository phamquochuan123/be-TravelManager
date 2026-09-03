package com.example.travelManager.config;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.travelManager.domain.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Trả 403 khi user ĐÃ đăng nhập nhưng không đủ quyền.
 *
 * Trước đây SecurityConfig chỉ khai authenticationEntryPoint, nên mọi trường hợp
 * thiếu quyền cũng rơi vào đó và trả 401 "Bạn cần đăng nhập để tiếp tục" — sai nghĩa:
 * frontend không phân biệt được "hết phiên đăng nhập" với "không có quyền", nên
 * interceptor bắt 401 sẽ đăng xuất người dùng chỉ vì họ bấm vào chức năng không thuộc quyền.
 *
 * Giữ nguyên shape {statusCode, message} như CustomAuthenticationEntryPoint và ErrorResponse.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        MAPPER.writeValue(response.getWriter(),
                new ErrorResponse(403, "Bạn không có quyền thực hiện thao tác này"));
    }
}
