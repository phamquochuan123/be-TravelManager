package com.example.travelManager.domain.response.tour;

import com.example.travelManager.util.constant.tour.CouponType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TourCouponResponse {
    private Long id;
    private String code;
    private CouponType couponType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private int usageLimit;
    private int usedCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
}
