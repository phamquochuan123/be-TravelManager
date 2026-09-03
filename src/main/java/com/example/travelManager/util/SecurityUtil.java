package com.example.travelManager.util;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SecurityUtil {

    private SecurityUtil() {
    }

    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    /**
     * Email của user đang đăng nhập, ném HTTP 401 nếu chưa đăng nhập.
     * Thay cho `orElseThrow(() -> new RuntimeException("Not authenticated"))` rải khắp
     * các controller — cách cũ rơi vào handler Exception chung và trả về HTTP 500,
     * khiến client không phân biệt được "chưa đăng nhập" với "server lỗi".
     */
    public static String getCurrentUserLoginOrThrow() {
        return getCurrentUserLogin()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof String principal) {
            return principal;
        }
        return null;
    }
}
