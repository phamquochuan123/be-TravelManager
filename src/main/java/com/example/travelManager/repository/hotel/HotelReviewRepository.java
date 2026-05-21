package com.example.travelManager.repository.hotel;

import com.example.travelManager.domain.hotel.HotelReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotelReviewRepository extends JpaRepository<HotelReview, Long> {

    List<HotelReview> findByHotelId(Long hotelId);

    boolean existsByBooking_BookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM HotelReview r WHERE r.hotel.id = :hotelId")
    Double findAverageRatingByHotelId(@Param("hotelId") Long hotelId);
}
