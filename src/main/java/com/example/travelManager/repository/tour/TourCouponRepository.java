package com.example.travelManager.repository.tour;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.travelManager.domain.tour.TourCoupon;

public interface TourCouponRepository extends JpaRepository<TourCoupon, Long> {

    Optional<TourCoupon> findByCode(String code);

    @Query("SELECT c FROM TourCoupon c WHERE c.code = :code AND c.active = true " +
           "AND c.startDate <= :today AND c.endDate >= :today " +
           "AND c.usedCount < c.usageLimit")
    Optional<TourCoupon> findValidCoupon(@Param("code") String code,
                                          @Param("today") LocalDate today);
}
