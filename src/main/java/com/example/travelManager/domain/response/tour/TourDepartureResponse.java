package com.example.travelManager.domain.response.tour;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.travelManager.util.constant.tour.TourDepartureStatus;

import lombok.Data;

@Data
public class TourDepartureResponse {
    private Long id;
    private LocalDate departureDate;
    private int availableSlots;
    private Long staffId;
    private String staffName;

    /**
     * Giá THỰC TẾ áp dụng cho chính chuyến này (đã tính giá mùa nếu ngày khởi hành
     * rơi vào mùa cao/thấp điểm). FE phải hiển thị giá này thay vì tour.priceAdult,
     * nếu không khách sẽ thấy một giá mà bị tính một giá khác.
     */
    private BigDecimal priceAdult;
    private BigDecimal priceChild;

    /**
     * Trạng thái chuyến, do TourDepartureScheduler cập nhật theo ngày. FE cần
     * nó để lọc ra những chuyến còn đặt được — số chỗ trống không nói lên điều
     * đó, vì chuyến đã huỷ hoặc đã đi vẫn còn nguyên availableSlots.
     */
    private TourDepartureStatus status;
}
