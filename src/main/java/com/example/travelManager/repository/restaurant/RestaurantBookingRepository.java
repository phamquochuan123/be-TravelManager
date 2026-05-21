package com.example.travelManager.repository.restaurant;

import com.example.travelManager.domain.restaurant.RestaurantBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantBookingRepository extends JpaRepository<RestaurantBooking, Long> {
    List<RestaurantBooking> findByUserEmailOrderByCreatedAtDesc(String email);
    List<RestaurantBooking> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    Optional<RestaurantBooking> findByConfirmationCode(String confirmationCode);
    List<RestaurantBooking> findAllByOrderByCreatedAtDesc();
}
