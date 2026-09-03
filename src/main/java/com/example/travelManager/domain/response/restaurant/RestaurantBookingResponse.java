package com.example.travelManager.domain.response.restaurant;

import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RestaurantBookingResponse {
    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantCity;
    private String restaurantPhoto;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private int guestCount;
    private String specialRequests;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private RestaurantBookingStatus status;
    private String confirmationCode;
    private Instant createdAt;
}
