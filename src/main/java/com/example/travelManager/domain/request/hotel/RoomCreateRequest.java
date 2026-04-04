package com.example.travelManager.domain.request.hotel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomCreateRequest {

    @NotBlank
    private String roomType;

    @NotNull
    private BigDecimal roomPrice;

    private String roomNumber;

    @Min(1)
    private int maxGuests = 2;

    @Min(1)
    private int numBeds = 1;

    private Double area;

    private String description;
}
