package com.example.travelManager.repository.restaurant;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelManager.domain.restaurant.RestaurantFavorite;

public interface RestaurantFavoriteRepository extends JpaRepository<RestaurantFavorite, Long> {
    boolean existsByUserIdAndRestaurantId(Long userId, Long restaurantId);
    Optional<RestaurantFavorite> findByUserIdAndRestaurantId(Long userId, Long restaurantId);
    List<RestaurantFavorite> findByUserId(Long userId);
}
