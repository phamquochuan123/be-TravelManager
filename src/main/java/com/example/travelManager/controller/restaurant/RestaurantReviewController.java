package com.example.travelManager.controller.restaurant;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.restaurant.RestaurantReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.repository.restaurant.RestaurantReviewRepository;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
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

@RestController
@RequestMapping("/restaurants/{restaurantId}/reviews")
@RequiredArgsConstructor
public class RestaurantReviewController {

    private final RestaurantReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantBookingRepository bookingRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable("restaurantId") Long restaurantId) {
        List<ReviewResponse> reviews = reviewRepository.findByRestaurantId(restaurantId)
                .stream()
                .filter(r -> !r.isHidden())
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ReviewResponse>> getAllReviews(@PathVariable("restaurantId") Long restaurantId) {
        return ResponseEntity.ok(
                reviewRepository.findByRestaurantId(restaurantId).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable("restaurantId") Long restaurantId,
            @Valid @RequestBody ReviewRequest request) {

        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

        RestaurantBooking booking = bookingRepository.findByConfirmationCode(request.getConfirmationCode())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getRestaurant().getId().equals(restaurantId)) {
            throw new IllegalArgumentException("Booking này không thuộc nhà hàng " + restaurantId);
        }
        if (booking.getUser() == null || booking.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("Booking này không thuộc về bạn");
        }
        if (booking.getStatus() != RestaurantBookingStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ được đánh giá sau khi booking hoàn thành");
        }
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new IllegalStateException("Bạn đã đánh giá nhà hàng này rồi");
        }

        RestaurantReview review = new RestaurantReview();
        review.setRestaurant(restaurant);
        review.setUser(user);
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(reviewRepository.save(review)));
    }

    @PatchMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponse> replyReview(
            @PathVariable("restaurantId") Long restaurantId,
            @PathVariable("reviewId") Long reviewId,
            @RequestBody Map<String, String> body) {
        RestaurantReview review = findReviewInRestaurant(restaurantId, reviewId);
        review.setAdminReply(body.get("reply"));
        return ResponseEntity.ok(toResponse(reviewRepository.save(review)));
    }

    @PatchMapping("/{reviewId}/hide")
    public ResponseEntity<ReviewResponse> hideReview(
            @PathVariable("restaurantId") Long restaurantId,
            @PathVariable("reviewId") Long reviewId) {
        RestaurantReview review = findReviewInRestaurant(restaurantId, reviewId);
        review.setHidden(true);
        return ResponseEntity.ok(toResponse(reviewRepository.save(review)));
    }

    @PatchMapping("/{reviewId}/unhide")
    public ResponseEntity<ReviewResponse> unhideReview(
            @PathVariable("restaurantId") Long restaurantId,
            @PathVariable("reviewId") Long reviewId) {
        RestaurantReview review = findReviewInRestaurant(restaurantId, reviewId);
        review.setHidden(false);
        return ResponseEntity.ok(toResponse(reviewRepository.save(review)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("restaurantId") Long restaurantId,
            @PathVariable("reviewId") Long reviewId) {
        reviewRepository.delete(findReviewInRestaurant(restaurantId, reviewId));
        return ResponseEntity.noContent().build();
    }

    private RestaurantReview findReviewInRestaurant(Long restaurantId, Long reviewId) {
        RestaurantReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!review.getRestaurant().getId().equals(restaurantId)) {
            throw new IllegalArgumentException("Review này không thuộc nhà hàng " + restaurantId);
        }
        return review;
    }

    private ReviewResponse toResponse(RestaurantReview r) {
        ReviewResponse res = new ReviewResponse();
        res.setId(r.getId());
        res.setRestaurantId(r.getRestaurant().getId());
        res.setUserName(r.getUser() != null ? r.getUser().getName() : "Ẩn danh");
        res.setBookingId(r.getBooking() != null ? r.getBooking().getId() : null);
        res.setRating(r.getRating());
        res.setComment(r.getComment());
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
    }

    @Data
    public static class ReviewResponse {
        private Long id;
        private Long restaurantId;
        private String userName;
        private Long bookingId;
        private int rating;
        private String comment;
        private String adminReply;
        private boolean isHidden;
        private Instant createdAt;
    }
}

