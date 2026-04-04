package com.example.travelManager.domain.response.hotel;

import com.example.travelManager.domain.HotelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponse {

    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private int starRating;
    private HotelType hotelType;
    private String amenities;
    private Double latitude;
    private Double longitude;
    private boolean isActive;
    private int totalRooms;
    private byte[] photo;
}
