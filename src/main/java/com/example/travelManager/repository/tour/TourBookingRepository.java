package com.example.travelManager.repository.tour;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelManager.util.constant.tour.BookingStatus;
import com.example.travelManager.domain.tour.TourBooking;

public interface TourBookingRepository extends JpaRepository<TourBooking, Long> {

    List<TourBooking> findByUserId(Long userId);

    List<TourBooking> findByTourId(Long tourId);

    List<TourBooking> findByDepartureId(Long departureId);

    boolean existsByTourIdAndStatusIn(Long tourId, List<BookingStatus> statuses);
}
