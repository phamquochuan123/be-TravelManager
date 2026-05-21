package com.example.travelManager.domain.response.restaurant;

import com.example.travelManager.util.constant.restaurant.CuisineType;
import com.example.travelManager.util.constant.restaurant.PriceRange;
import lombok.Data;

@Data
public class RestaurantResponse {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private CuisineType cuisineType;
    private PriceRange priceRange;
    private Integer capacity;
    private String openingHours;
    private String amenities;
    private boolean isActive;
    private Double averageRating;
    private byte[] photo;
}
