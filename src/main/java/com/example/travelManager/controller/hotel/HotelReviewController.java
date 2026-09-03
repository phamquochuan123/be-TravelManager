package com.example.travelManager.controller.hotel;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.hotel.HotelReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.service.hotel.HotelReviewService;
import com.example.travelManager.util.SecurityUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/hotels/{hotelId}/reviews")
@RequiredArgsConstructor
public class HotelReviewController {

    private final HotelReviewService reviewService;
    private final UserRepository userRepository;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable("hotelId") Long hotelId) {
        return ResponseEntity.ok(
                reviewService.getVisibleReviews(hotelId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<ReviewResponse>> getAllReviews(@PathVariable("hotelId") Long hotelId) {
        return ResponseEntity.ok(
                reviewService.getAllReviews(hotelId).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable("hotelId") Long hotelId,
            @Valid @RequestBody ReviewRequest request) {

        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        HotelReview review = reviewService.createReview(hotelId, request.getConfirmationCode(), email, user,
                request.getRating(), request.getComment(), request.getImages());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(review));
    }

    @PatchMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponse> replyReview(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("reviewId") Long reviewId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(toResponse(reviewService.reply(hotelId, reviewId, body.get("reply"))));
    }

    @PatchMapping("/{reviewId}/hide")
    public ResponseEntity<ReviewResponse> hideReview(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("reviewId") Long reviewId) {
        return ResponseEntity.ok(toResponse(reviewService.setHidden(hotelId, reviewId, true)));
    }

    @PatchMapping("/{reviewId}/unhide")
    public ResponseEntity<ReviewResponse> unhideReview(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("reviewId") Long reviewId) {
        return ResponseEntity.ok(toResponse(reviewService.setHidden(hotelId, reviewId, false)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("reviewId") Long reviewId) {
        reviewService.delete(hotelId, reviewId);
        return ResponseEntity.noContent().build();
    }

    private ReviewResponse toResponse(HotelReview r) {
        ReviewResponse res = new ReviewResponse();
        res.setId(r.getId());
        res.setHotelId(r.getHotel().getId());
        res.setUserName(r.getUser() != null ? r.getUser().getName() : "Ẩn danh");
        res.setBookingId(r.getBooking() != null ? r.getBooking().getBookingId() : null);
        res.setRating(r.getRating());
        res.setComment(r.getComment());
        if (r.getImages() != null) {
            try { res.setImages(MAPPER.readValue(r.getImages(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {})); }
            catch (Exception ignored) {}
        }
        res.setAdminReply(r.getAdminReply());
        res.setHidden(r.isHidden());
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }

    @Data
    public static class ReviewRequest {
        @NotBlank
        private String confirmationCode;
        @Min(1) @Max(5)
        private int rating;
        private String comment;
        private java.util.List<String> images;
    }

    @Data
    public static class ReviewResponse {
        private Long id;
        private Long hotelId;
        private String userName;
        private Long bookingId;
        private int rating;
        private String comment;
        private java.util.List<String> images;
        private String adminReply;
        private boolean isHidden;
        private Instant createdAt;
    }
}
