package com.example.travelManager.repository.tour;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.travelManager.domain.tour.TourReview;

public interface TourReviewRepository extends JpaRepository<TourReview, Long> {

    List<TourReview> findByTourId(Long tourId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM TourReview r WHERE r.tour.id = :tourId")
    Double findAverageRatingByTourId(@Param("tourId") Long tourId);
}
