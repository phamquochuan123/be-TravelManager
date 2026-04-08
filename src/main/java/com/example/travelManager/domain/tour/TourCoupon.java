package com.example.travelManager.domain.tour;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.travelManager.util.constant.tour.CouponType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tour_coupons")
public class TourCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // ma giam gia, vd: "SUMMER2025"

    @Enumerated(EnumType.STRING)
    private CouponType couponType; // PERCENT / FIXED

    private BigDecimal discountValue;  // 20 (%) hoac 200000 (VND)
    private BigDecimal minOrderValue;  // don hang toi thieu de ap dung

    private int usageLimit; // so lan su dung toi da
    private int usedCount;  // so lan da su dung

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean active = true;
}
