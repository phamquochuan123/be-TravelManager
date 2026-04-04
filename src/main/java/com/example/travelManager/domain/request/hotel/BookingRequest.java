package com.example.travelManager.domain.request.hotel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest {

    @NotNull
    private LocalDate checkInDate;

    @NotNull
    private LocalDate checkOutDate;

    @NotBlank
    private String guestFullName;

    @NotBlank
    private String guestEmail;

    @Min(1)
    private int numOfAdults;

    @Min(0)
    private int numOfChildren;
}
