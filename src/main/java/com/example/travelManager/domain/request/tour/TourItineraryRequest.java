package com.example.travelManager.domain.request.tour;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TourItineraryRequest {

    @Min(1)
    private int dayNumber;

    @NotBlank
    private String title;

    private String description;
    private String activities;
}
