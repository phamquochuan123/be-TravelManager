package com.example.travelManager.domain.request.tour;

import java.time.LocalTime;

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

    // Package options
    private Long roomId;         // bắt buộc nếu tour >= 2 đêm

    private Long restaurantId;   // tuỳ chọn

    private LocalTime restaurantBookingTime; // mặc định 12:00 nếu không truyền
}
