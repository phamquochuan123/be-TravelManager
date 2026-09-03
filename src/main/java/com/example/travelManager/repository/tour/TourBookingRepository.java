package com.example.travelManager.repository.tour;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.travelManager.util.constant.tour.BookingStatus;
import com.example.travelManager.domain.tour.TourBooking;

public interface TourBookingRepository extends JpaRepository<TourBooking, Long> {

    List<TourBooking> findByUserId(Long userId);

    List<TourBooking> findByUserEmailOrderByCreatedAtDesc(String email);

    List<TourBooking> findByTourId(Long tourId);

    List<TourBooking> findByDepartureId(Long departureId);

    List<TourBooking> findAllByOrderByCreatedAtDesc();

    /** Bản phân trang ở DB của findAllByOrderByCreatedAtDesc, dùng cho màn quản trị. */
    Page<TourBooking> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<TourBooking> findByCreatedAtBetweenOrderByCreatedAtDesc(java.time.Instant from, java.time.Instant to);

    boolean existsByTourIdAndStatusIn(Long tourId, List<BookingStatus> statuses);

    boolean existsByDepartureIdAndStatusIn(Long departureId, List<BookingStatus> statuses);

    @Modifying
    @Query("UPDATE TourBooking b SET b.status = 'CANCELLED' WHERE b.id = :id AND b.status <> 'CANCELLED'")
    int cancelIfNotAlready(@Param("id") Long id);
}
