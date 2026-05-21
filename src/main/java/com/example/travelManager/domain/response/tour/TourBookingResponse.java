package com.example.travelManager.domain.response.tour;

import com.example.travelManager.util.constant.tour.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
public class TourBookingResponse {
    private Long id;
    private Long tourId;
    private String tourName;
    private String tourDestination;
    private Long departureId;
    private LocalDate departureDate;
    private int numAdults;
    private int numChildren;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private BookingStatus status;
    private String note;
    private Instant createdAt;
}
