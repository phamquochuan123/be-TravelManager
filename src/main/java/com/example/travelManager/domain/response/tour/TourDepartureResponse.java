package com.example.travelManager.domain.response.tour;

import java.time.LocalDate;

import lombok.Data;

@Data
public class TourDepartureResponse {
    private Long id;
    private LocalDate departureDate;
    private int availableSlots;
    private Long staffId;
    private String staffName;
}
