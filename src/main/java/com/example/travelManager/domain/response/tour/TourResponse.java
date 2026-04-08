package com.example.travelManager.domain.response.tour;

import java.math.BigDecimal;

import com.example.travelManager.util.constant.tour.TourStatus;
import com.example.travelManager.util.constant.tour.TourType;

import lombok.Data;

// Dung cho danh sach tour (gon)
@Data
public class TourResponse {
    private Long id;
    private String name;
    private String destination;
    private String departure;
    private TourType tourType;
    private BigDecimal priceAdult;
    private BigDecimal priceChild;
    private int durationDays;
    private int maxSlots;
    private TourStatus status;
    private Double averageRating;
    private int totalDepartures;
}
