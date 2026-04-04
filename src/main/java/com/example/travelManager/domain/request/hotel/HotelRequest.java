package com.example.travelManager.domain.request.hotel;

import com.example.travelManager.domain.HotelType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HotelRequest {

    @NotBlank
    private String name;

    private String description;

    private String address;

    @NotBlank
    private String city;

    @Min(1) @Max(5)
    private int starRating;

    @NotNull
    private HotelType hotelType;

    private String amenities;

    private Double latitude;

    private Double longitude;

    private Boolean isActive;
}
