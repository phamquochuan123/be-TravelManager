package com.example.travelManager.domain.request.tour;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourReviewRequest {

    @NotNull(message = "Vui lòng cung cấp mã booking")
    private Long bookingId;

    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    private int rating;

    private String comment;
}
