package com.example.travelManager.domain.response.tour;

import com.example.travelManager.util.constant.tour.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    /** Các bữa ăn kèm, rỗng nếu khách không chọn nhà hàng nào. */
    private List<BuaAn> restaurants = new ArrayList<>();

    private BookingStatus status;
    private String note;
    private Instant createdAt;

    /** Một bữa trong đơn. Kèm nhãn bữa để giao diện khỏi tự suy ra từ giờ. */
    @Data
    public static class BuaAn {
        private Long restaurantBookingId;
        private Long restaurantId;
        private String restaurantName;
        private LocalDate bookingDate;
        private LocalTime bookingTime;
        private String mealSlot;      // SANG / TRUA / TOI
        private String mealSlotLabel; // "Sáng" / "Trưa" / "Tối"
        private int guestCount;
        private String confirmationCode;
    }
}
