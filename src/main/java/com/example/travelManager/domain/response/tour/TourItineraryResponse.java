package com.example.travelManager.domain.response.tour;

import lombok.Data;

@Data
public class TourItineraryResponse {
    private Long id;
    private int dayNumber;
    private String title;
    private String description;
    private String activities;
}
