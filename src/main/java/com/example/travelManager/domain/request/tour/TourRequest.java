package com.example.travelManager.domain.request.tour;

import java.math.BigDecimal;

import com.example.travelManager.util.constant.tour.TourType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String destination;

    @NotBlank
    private String departure;

    @NotNull
    private TourType tourType;

    @NotNull
    @Min(0)
    private BigDecimal priceAdult;

    @Min(0)
    private BigDecimal priceChild;

    @Min(1)
    private int durationDays;

    @Min(1)
    private int maxSlots;

    private String cancellationPolicy;
    private String includedServices;
}
