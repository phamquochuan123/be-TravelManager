package com.example.travelManager.domain.response.tour;

import lombok.Data;

import java.time.Instant;

@Data
public class TourReviewResponse {
    private Long id;
    private Long tourId;
    private String userName;
    private Long bookingId;
    private int rating;
    private String comment;
    private String adminReply;
    private boolean isHidden;
    private Instant createdAt;
}
