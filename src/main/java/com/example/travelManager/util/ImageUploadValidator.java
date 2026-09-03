package com.example.travelManager.util;

import java.util.List;
import java.util.Locale;

import org.springframework.web.multipart.MultipartFile;

/**
 * Kiểm tra file ảnh do người dùng upload.
 *
 * Trước đây không có chỗ nào whitelist kiểu file: bất kỳ ai đăng nhập được đều
 * upload được file tuỳ ý (script, tài liệu, file rác cỡ lớn) vào DB/thư mục ảnh.
 * Ném IllegalArgumentException — GlobalExceptionHandler đã map sẵn về HTTP 400.
 */
public final class ImageUploadValidator {

    /** Nhỏ hơn giới hạn multipart chung (10MB) để lỗi báo rõ ràng thay vì bị Tomcat cắt. */
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif");

    private ImageUploadValidator() {
    }

    /** Bỏ qua nếu file null/rỗng (dùng cho các field ảnh không bắt buộc). */
    public static void validateIfPresent(MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            validateImage(file);
        }
    }

    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được rỗng");
        }

        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Ảnh vượt quá dung lượng cho phép (tối đa 5MB)");
        }

        validateContentTypeAndName(file.getContentType(), file.getOriginalFilename());
    }

    /**
     * Phần kiểm tra dùng chung cho cả MultipartFile (Spring) và Part (Servlet API),
     * để MultipartImageValidationFilter tái sử dụng được đúng bộ quy tắc này.
     */
    public static void validateContentTypeAndName(String contentType, String originalName) {
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Chỉ chấp nhận ảnh định dạng JPG, PNG, WEBP hoặc GIF");
        }

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Tên file ảnh không hợp lệ");
        }

        // Chặn path traversal: tên file được dùng để đặt tên khi lưu ra đĩa.
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new IllegalArgumentException("Tên file ảnh chứa ký tự không hợp lệ");
        }

        String lowerName = originalName.toLowerCase(Locale.ROOT);
        boolean extensionAllowed = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!extensionAllowed) {
            throw new IllegalArgumentException(
                    "Đuôi file ảnh không hợp lệ. Chỉ chấp nhận .jpg, .jpeg, .png, .webp, .gif");
        }
    }
}
