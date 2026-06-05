package com.example.travelManager.domain.response.tour;

import java.math.BigDecimal;
import java.util.List;

import com.example.travelManager.util.constant.tour.TourStatus;
import com.example.travelManager.util.constant.tour.TourType;

import lombok.Data;

// Dung cho trang chi tiet tour (day du)
@Data
public class TourDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String destination;
    private String departure;
    private TourType tourType;
    private BigDecimal priceAdult;
    private BigDecimal priceChild;
    private int durationDays;
    private int durationNights;
    private int maxSlots;
    private Double packageDiscountPercent;
    private TourStatus status;
    private String cancellationPolicy;
    private String includedServices;
    private Double averageRating;
    private List<TourImageResponse> images;
    private List<TourItineraryResponse> itineraries;
    private List<TourDepartureResponse> departures;
    private List<Integer> linkedHotels;
    private List<Integer> linkedRestaurants;
    private List<Integer> linkedDestinations;
}
