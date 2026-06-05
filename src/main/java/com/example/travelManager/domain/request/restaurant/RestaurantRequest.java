package com.example.travelManager.domain.request.restaurant;

import java.math.BigDecimal;

import com.example.travelManager.util.constant.restaurant.CuisineType;
import com.example.travelManager.util.constant.restaurant.PriceRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RestaurantRequest {

    @NotBlank
    private String name;

    private String description;

    private String address;

    @NotBlank
    private String city;

    @NotNull
    private CuisineType cuisineType;

    @NotNull
    private PriceRange priceRange;

    private Integer capacity;

    private String openingHours;

    private String amenities;

    private Boolean isActive;

    private BigDecimal pricePerPerson;
}
