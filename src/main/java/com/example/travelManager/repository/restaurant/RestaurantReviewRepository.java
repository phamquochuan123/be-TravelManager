package com.example.travelManager.repository.restaurant;

import com.example.travelManager.domain.restaurant.RestaurantReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantReviewRepository extends JpaRepository<RestaurantReview, Long> {

    List<RestaurantReview> findByRestaurantId(Long restaurantId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM RestaurantReview r WHERE r.restaurant.id = :restaurantId")
    Double findAverageRatingByRestaurantId(@Param("restaurantId") Long restaurantId);
}
