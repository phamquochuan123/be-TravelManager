package com.example.travelManager.controller.restaurant;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.restaurant.RestaurantReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.service.restaurant.RestaurantReviewService;
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
@RequestMapping("/restaurants/{restaurantId}/reviews")
@RequiredArgsConstructor
public class RestaurantReviewController {

    private final RestaurantReviewService reviewService;
    private final UserRepository userRepository;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable("restaurantId") Long restaurantId) {
        return ResponseEntity.ok(
                reviewService.getVisibleReviews(restaurantId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<ReviewResponse>> getAllReviews(@PathVariable("restaurantId") Long restaurantId) {
        return ResponseEntity.ok(
                reviewService.getAllReviews(restaurantId).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable("restaurantId") Long restaurantId,
            @Valid @RequestBody ReviewRequest request) {

        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RestaurantReview review = reviewService.createReview(restaurantId, request.getConfirmationCode(), user,
                request.getRating(), request.getComment(), request.getImages());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(review));
    }

    @PatchMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponse> replyReview(
            @PathVariable("restaurantId") Long restaurantId,
            @PathVariable("reviewId") Long reviewId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(toResponse(reviewService.reply(restaurantId, reviewId, body.get("reply"))));
    }

    @PatchMapping("/{reviewId}/hide")
    public ResponseEntity<ReviewResponse> hideReview(
            @PathVariable("restaurantId") Long restaurantId,
            @PathVariable("reviewId") Long reviewId) {
        return ResponseEntity.ok(toResponse(reviewService.setHidden(restaurantId, reviewId, true)));
    }

    @PatchMapping("/{reviewId}/unhide")
    public ResponseEntity<ReviewResponse> unhideReview(
            @PathVariable("restaurantId") Long restaurantId,
            @PathVariable("reviewId") Long reviewId) {
        return ResponseEntity.ok(toResponse(reviewService.setHidden(restaurantId, reviewId, false)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("restaurantId") Long restaurantId,
            @PathVariable("reviewId") Long reviewId) {
        reviewService.delete(restaurantId, reviewId);
        return ResponseEntity.noContent().build();
    }

    private ReviewResponse toResponse(RestaurantReview r) {
        ReviewResponse res = new ReviewResponse();
        res.setId(r.getId());
        res.setRestaurantId(r.getRestaurant().getId());
        res.setUserName(r.getUser() != null ? r.getUser().getName() : "Ẩn danh");
        res.setBookingId(r.getBooking() != null ? r.getBooking().getId() : null);
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
        private Long restaurantId;
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
