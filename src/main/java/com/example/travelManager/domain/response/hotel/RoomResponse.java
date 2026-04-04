package com.example.travelManager.domain.response.hotel;

import com.example.travelManager.domain.RoomStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;

@Data
@NoArgsConstructor
public class RoomResponse {

    private Long id;
    private String roomNumber;
    private String roomType;
    private BigDecimal roomPrice;
    private RoomStatus status;
    private int maxGuests;
    private int numBeds;
    private Double area;
    private String description;
    private String photo;
    private Long hotelId;
    private String hotelName;
    private List<BookingResponse> bookings;

    public RoomResponse(Long id, String roomType, BigDecimal roomPrice) {
        this.id = id;
        this.roomType = roomType;
        this.roomPrice = roomPrice;
    }

    public RoomResponse(Long id, String roomNumber, String roomType, BigDecimal roomPrice,
            RoomStatus status, int maxGuests, int numBeds, Double area, String description,
            boolean isBooked, byte[] photoBytes, Long hotelId, String hotelName,
            List<BookingResponse> bookings) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.roomPrice = roomPrice;
        this.status = status;
        this.maxGuests = maxGuests;
        this.numBeds = numBeds;
        this.area = area;
        this.description = description;
        this.photo = photoBytes != null ? Base64.getEncoder().encodeToString(photoBytes) : null;
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.bookings = bookings;
    }
}
