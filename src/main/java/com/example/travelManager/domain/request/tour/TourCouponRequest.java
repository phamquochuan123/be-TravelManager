package com.example.travelManager.domain.request.tour;

import com.example.travelManager.util.constant.tour.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TourCouponRequest {

    @NotBlank(message = "Mã coupon không được để trống")
    private String code;

    @NotNull(message = "Loại coupon không được để trống")
    private CouponType couponType;

    @NotNull(message = "Giá trị giảm không được để trống")
    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private int usageLimit = 100;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    private boolean active = true;
}
