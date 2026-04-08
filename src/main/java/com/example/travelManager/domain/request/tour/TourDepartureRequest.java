package com.example.travelManager.domain.request.tour;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourDepartureRequest {

    @NotNull
    private LocalDate departureDate;

    @Min(1)
    private int availableSlots;
}
