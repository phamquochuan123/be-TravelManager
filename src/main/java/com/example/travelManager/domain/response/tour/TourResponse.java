package com.example.travelManager.domain.response.tour;

import java.math.BigDecimal;
import java.util.List;

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
    private List<String> images; // base64 strings, index 0 = ảnh chính
}
