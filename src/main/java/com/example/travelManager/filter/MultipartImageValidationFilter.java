package com.example.travelManager.filter;

import java.io.IOException;
import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.travelManager.domain.response.ErrorResponse;
import com.example.travelManager.util.ImageUploadValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * Kiểm tra mọi file upload ở một chỗ duy nhất.
 *
 * Toàn bộ ~22 endpoint nhận MultipartFile trong dự án đều chỉ dùng để nhận ẢNH.
 * Đặt kiểm tra ở filter thay vì rải @Valid khắp 11 controller: không bỏ sót chỗ nào,
 * kể cả chỗ đọc file thủ công như AdminHotelController (req.getFile("roomImage_" + i)),
 * và endpoint thêm sau này cũng tự động được bảo vệ.
 *
 * Chạy sau RateLimitFilter, trước filter chain của Spring Security.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MultipartImageValidationFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String contentType = request.getContentType();
        if (contentType == null
                || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Tomcat cache lại các part đã parse, nên Spring MultipartResolver phía sau dùng lại
            // chứ không parse body lần hai.
            for (Part part : request.getParts()) {
                String filename = part.getSubmittedFileName();
                if (filename == null || filename.isBlank()) {
                    continue; // field thường, không phải file
                }
                validatePart(part, filename);
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            MAPPER.writeValue(response.getWriter(), new ErrorResponse(400, e.getMessage()));
            return;
        } catch (Exception e) {
            // Body multipart hỏng/không parse được — để Spring xử lý tiếp như trước.
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void validatePart(Part part, String filename) {
        if (part.getSize() <= 0) {
            return; // input file để trống, các endpoint đều coi ảnh là không bắt buộc
        }
        if (part.getSize() > ImageUploadValidator.MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Ảnh vượt quá dung lượng cho phép (tối đa 5MB)");
        }
        ImageUploadValidator.validateContentTypeAndName(part.getContentType(), filename);
    }
}
