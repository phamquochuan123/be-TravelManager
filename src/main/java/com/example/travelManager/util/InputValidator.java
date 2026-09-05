package com.example.travelManager.util;

import java.math.BigDecimal;

/**
 * Kiểm tra dữ liệu nhập cho các endpoint quản trị.
 *
 * Vì sao cần: các controller công khai (HotelController, TourController...) nhận
 * DTO có @Valid nên ràng buộc chạy đầy đủ, nhưng nhóm Admin*Controller lại nhận
 * thẳng @RequestParam của form multipart nên KHÔNG có ràng buộc nào — mà giao diện
 * admin gọi đúng nhóm này. Hệ quả: tên toàn khoảng trắng, khách sạn 99 sao, nhà
 * hàng -50 bàn đều lưu được, còn tên dài quá 255 ký tự thì vỡ thành lỗi 500.
 *
 * Ném IllegalArgumentException vì GlobalExceptionHandler đã map sẵn nó thành
 * HTTP 400 kèm message — admin đọc được lý do thay vì nhận lỗi trống.
 */
public final class InputValidator {

    private InputValidator() {}

    /** Bằng độ dài cột varchar mặc định của JPA — dài hơn là DB ném lỗi 500. */
    public static final int DAI_TOI_DA_MAC_DINH = 255;
    public static final int DAI_TOI_DA_MO_TA = 5000;

    private static final int DAI_TOI_THIEU_TEN = 2;

    /**
     * Trường bắt buộc: bỏ khoảng trắng thừa, không cho rỗng, quá ngắn hay quá dài.
     * Trả về chuỗi đã trim để cái lưu xuống DB đúng bằng cái đã kiểm.
     */
    public static String batBuoc(String giaTri, String tenTruong, int doDaiToiDa) {
        String s = giaTri == null ? "" : giaTri.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException(tenTruong + " không được để trống");
        }
        if (s.length() < DAI_TOI_THIEU_TEN) {
            throw new IllegalArgumentException(
                    tenTruong + " phải có ít nhất " + DAI_TOI_THIEU_TEN + " ký tự");
        }
        if (s.length() > doDaiToiDa) {
            throw new IllegalArgumentException(
                    tenTruong + " không được dài quá " + doDaiToiDa + " ký tự (đang " + s.length() + ")");
        }
        return s;
    }

    public static String batBuoc(String giaTri, String tenTruong) {
        return batBuoc(giaTri, tenTruong, DAI_TOI_DA_MAC_DINH);
    }

    /** Trường không bắt buộc: cho phép rỗng, chỉ chặn vượt độ dài cột. */
    public static String tuyChon(String giaTri, String tenTruong, int doDaiToiDa) {
        String s = giaTri == null ? "" : giaTri.trim();
        if (s.length() > doDaiToiDa) {
            throw new IllegalArgumentException(
                    tenTruong + " không được dài quá " + doDaiToiDa + " ký tự (đang " + s.length() + ")");
        }
        return s;
    }

    public static String tuyChon(String giaTri, String tenTruong) {
        return tuyChon(giaTri, tenTruong, DAI_TOI_DA_MAC_DINH);
    }

    /**
     * Tên hiển thị: như batBuoc nhưng chặn thêm dấu ngoặc nhọn.
     *
     * React đã tự escape khi render nên đây không phải lỗ hổng XSS; chặn ở đây là
     * để giữ sạch dữ liệu — "&lt;script&gt;alert(1)&lt;/script&gt;" không phải tên
     * khách sạn, và nó sẽ trôi vào email xác nhận, hoá đơn, file export.
     */
    public static String ten(String giaTri, String tenTruong, int doDaiToiDa) {
        String s = batBuoc(giaTri, tenTruong, doDaiToiDa);
        if (s.contains("<") || s.contains(">")) {
            throw new IllegalArgumentException(tenTruong + " không được chứa ký tự < hoặc >");
        }
        return s;
    }

    public static String ten(String giaTri, String tenTruong) {
        return ten(giaTri, tenTruong, DAI_TOI_DA_MAC_DINH);
    }

    public static int trongKhoang(int giaTri, int min, int max, String tenTruong) {
        if (giaTri < min || giaTri > max) {
            throw new IllegalArgumentException(
                    tenTruong + " phải nằm trong khoảng " + min + " đến " + max + " (đang " + giaTri + ")");
        }
        return giaTri;
    }

    public static long khongAm(long giaTri, String tenTruong) {
        if (giaTri < 0) {
            throw new IllegalArgumentException(tenTruong + " không được là số âm (đang " + giaTri + ")");
        }
        return giaTri;
    }

    public static BigDecimal duong(BigDecimal giaTri, String tenTruong) {
        if (giaTri == null) {
            throw new IllegalArgumentException(tenTruong + " không được để trống");
        }
        if (giaTri.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(tenTruong + " phải lớn hơn 0 (đang " + giaTri.toPlainString() + ")");
        }
        return giaTri;
    }
}
