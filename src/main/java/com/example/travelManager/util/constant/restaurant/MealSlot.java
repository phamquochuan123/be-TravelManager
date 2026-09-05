package com.example.travelManager.util.constant.restaurant;

import java.time.LocalTime;

/**
 * Khung bữa ăn trong ngày.
 *
 * Dùng để chặn khách đặt hai nhà hàng vào cùng một bữa của cùng một ngày — không ai
 * ăn trưa được ở hai nơi cùng lúc. Đặt 12:00 ngày 21 rồi thì nhà hàng thứ hai phải
 * rơi vào khung khác (tối ngày 21) hoặc sang ngày khác trong thời gian tour.
 *
 * Ranh giới các khung là LUẬT NGHIỆP VỤ, không phải chi tiết kỹ thuật: sửa ở đây là
 * đổi hành vi đặt bàn của toàn hệ thống. Bản sao cho giao diện nằm ở BookingPage.tsx
 * (hằng MEAL_SLOTS) — sửa một bên mà quên bên kia thì giao diện cho bấm rồi server
 * mới chặn, khách thấy lỗi khó hiểu ở bước cuối.
 */
public enum MealSlot {
    SANG("Sáng", LocalTime.of(6, 0), LocalTime.of(10, 29)),
    TRUA("Trưa", LocalTime.of(10, 30), LocalTime.of(15, 59)),
    TOI("Tối", LocalTime.of(16, 0), LocalTime.of(23, 59));

    private final String nhan;
    private final LocalTime batDau;
    private final LocalTime ketThuc;

    MealSlot(String nhan, LocalTime batDau, LocalTime ketThuc) {
        this.nhan = nhan;
        this.batDau = batDau;
        this.ketThuc = ketThuc;
    }

    public String nhan() {
        return nhan;
    }

    public LocalTime batDau() {
        return batDau;
    }

    public LocalTime ketThuc() {
        return ketThuc;
    }

    /**
     * Khung chứa giờ đã cho, hoặc null nếu giờ nằm ngoài mọi khung (00:00–05:59).
     *
     * Trả null thay vì ném lỗi để nơi gọi tự quyết định thông báo — controller cần
     * câu tiếng Việt kèm tên nhà hàng, còn chỗ khác có thể chỉ cần bỏ qua.
     */
    public static MealSlot cuaGio(LocalTime gio) {
        if (gio == null) return null;
        for (MealSlot k : values()) {
            if (!gio.isBefore(k.batDau) && !gio.isAfter(k.ketThuc)) return k;
        }
        return null;
    }

    /** Mô tả khoảng giờ cho thông báo lỗi, vd. "Trưa (10:30–15:59)". */
    public String moTa() {
        return nhan + " (" + batDau + "–" + ketThuc + ")";
    }
}
