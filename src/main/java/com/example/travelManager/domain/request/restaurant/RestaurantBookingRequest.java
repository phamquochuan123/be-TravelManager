package com.example.travelManager.domain.request.restaurant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RestaurantBookingRequest {

    @NotNull
    private LocalDate bookingDate;

    @NotNull
    private LocalTime bookingTime;

    @Min(1)
    private int guestCount;

    private String specialRequests;

    @NotBlank
    private String contactName;

    @NotBlank
    private String contactPhone;

    @NotBlank
    private String contactEmail;

    private Long tourBookingId;
}
