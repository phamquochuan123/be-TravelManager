package com.example.travelManager.domain.request.tour;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourBookingRequest {

    @NotNull
    private Long departureId;

    @Min(1)
    private int numAdults;

    @Min(0)
    private int numChildren;

    @NotBlank
    private String contactName;

    @NotBlank
    private String contactPhone;

    @NotBlank
    private String contactEmail;

    private String couponCode;

    private String note;
}
