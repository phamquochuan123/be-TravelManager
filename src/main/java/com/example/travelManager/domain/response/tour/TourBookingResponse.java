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
    private String tourImage;
    private Long departureId;
    private LocalDate departureDate;
    private int numAdults;
    private int numChildren;
    private String contactName;
    private String contactPhone;
    private String contactEmail;

    // Price breakdown
    private BigDecimal originalPrice;
    private BigDecimal packageHotelPrice;
    private BigDecimal packageRestaurantPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private Double packageDiscountPercent;

    // Hotel info (nullable)
    private Long bookedRoomId;
    private Long hotelId;
    private String hotelName;
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    // Restaurant info (nullable)
    private Long restaurantBookingId;
    private Long restaurantId;
    private String restaurantName;

    private BookingStatus status;
    private String note;
    private Instant createdAt;
}
