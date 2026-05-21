package com.example.travelManager.repository.restaurant;

import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.util.constant.restaurant.CuisineType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByIsActiveTrue();
    List<Restaurant> findByCityIgnoreCaseAndIsActiveTrue(String city);
    List<Restaurant> findByCuisineTypeAndIsActiveTrue(CuisineType cuisineType);
}
