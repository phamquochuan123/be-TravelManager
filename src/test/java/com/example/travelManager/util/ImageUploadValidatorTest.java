package com.example.travelManager.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cho lớp kiểm tra file upload — trước đây không có whitelist nào,
 * bất kỳ tài khoản nào cũng upload được file tuỳ ý.
 */
class ImageUploadValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = { "image/jpeg", "image/png", "image/webp", "image/gif" })
    @DisplayName("Chấp nhận đúng các định dạng ảnh trong whitelist")
    void chapNhanAnhHopLe(String contentType) {
        String ext = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".gif";
        };
        assertThatCode(() -> ImageUploadValidator.validateContentTypeAndName(contentType, "anh" + ext))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application/pdf",
            "application/x-msdownload",
            "text/html",
            "application/octet-stream" })
    @DisplayName("Từ chối file không phải ảnh")
    void tuChoiFileKhongPhaiAnh(String contentType) {
        assertThatThrownBy(() -> ImageUploadValidator.validateContentTypeAndName(contentType, "file.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPG, PNG, WEBP hoặc GIF");
    }

    @Test
    @DisplayName("Content-type ảnh nhưng đuôi file lạ thì vẫn từ chối")
    void tuChoiDuoiFileLa() {
        assertThatThrownBy(() -> ImageUploadValidator.validateContentTypeAndName("image/png", "payload.exe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Đuôi file");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../../../etc/passwd.png",
            "..\\windows\\system32\\a.png",
            "thu/muc/anh.png" })
    @DisplayName("Từ chối tên file có ký tự đường dẫn (path traversal)")
    void tuChoiPathTraversal(String filename) {
        assertThatThrownBy(() -> ImageUploadValidator.validateContentTypeAndName("image/png", filename))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ký tự không hợp lệ");
    }

    @Test
    @DisplayName("Thiếu content-type thì từ chối, không đoán bừa")
    void tuChoiKhiThieuContentType() {
        assertThatThrownBy(() -> ImageUploadValidator.validateContentTypeAndName(null, "anh.png"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("So khớp không phân biệt hoa thường")
    void khongPhanBietHoaThuong() {
        assertThatCode(() -> ImageUploadValidator.validateContentTypeAndName("IMAGE/PNG", "ANH.PNG"))
                .doesNotThrowAnyException();
    }
}
