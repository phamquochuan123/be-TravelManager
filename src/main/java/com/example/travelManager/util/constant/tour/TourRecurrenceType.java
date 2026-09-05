package com.example.travelManager.util.constant.tour;

/**
 * Mẫu lặp dùng để hệ thống TỰ SINH lịch khởi hành cho tour, thay vì admin
 * phải thêm tay từng chuyến rồi để lịch cạn dần theo thời gian.
 *
 * Ngày cụ thể nằm ở {@code Tour.recurrenceDays}, cách đọc tuỳ theo giá trị ở đây.
 */
public enum TourRecurrenceType {

    /** Không tự sinh — admin tự thêm lịch ở trang Quản lý lịch. */
    NONE,

    /** recurrenceDays là các thứ trong tuần, vd. "MON,FRI" hoặc "SAT". */
    WEEKLY,

    /** recurrenceDays là các ngày trong tháng, vd. "5,20". Ngày không tồn tại
     *  trong tháng đó (31/02) được bỏ qua chứ không dồn về ngày cuối tháng. */
    MONTHLY
}
