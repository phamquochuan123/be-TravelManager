package com.example.travelManager.controller.hotel;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.hotel.HotelReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.hotel.HotelReviewRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/hotels/{hotelId}/reviews")
@RequiredArgsConstructor
public class HotelReviewController {

    private final HotelReviewRepository reviewRepository;
    private final HotelRepository hotelRepository;
    private final BookedRoomRepository bookedRoomRepository;
    private final UserRepository userRepository;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable("hotelId") Long hotelId) {
        List<ReviewResponse> reviews = reviewRepository.findByHotelId(hotelId)
                .stream()
                .filter(r -> !r.isHidden())
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ReviewResponse>> getAllReviews(@PathVariable("hotelId") Long hotelId) {
        return ResponseEntity.ok(
                reviewRepository.findByHotelId(hotelId).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable("hotelId") Long hotelId,
            @Valid @RequestBody ReviewRequest request) {

        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + hotelId));

        BookedRoom booking = bookedRoomRepository.findByBookingConfirmationCode(request.getConfirmationCode())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getRoom().getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Booking này không thuộc khách sạn " + hotelId);
        }
        if (!booking.getGuestEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Booking này không thuộc về bạn");
        }
        boolean checkoutPassed = !booking.getCheckOutDate().isAfter(LocalDate.now());
        boolean isCompleted = booking.getStatus() == com.example.travelManager.util.constant.hotel.HotelBookingStatus.COMPLETED;
        if (!checkoutPassed && !isCompleted) {
            throw new IllegalStateException("Chỉ được đánh giá sau khi đã check-out");
        }
        if (reviewRepository.existsByBooking_BookingId(booking.getBookingId())) {
            throw new IllegalStateException("Bạn đã đánh giá khách sạn này rồi");
        }

        HotelReview review = new HotelReview();
        review.setHotel(hotel);
        review.setUser(user);
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setHidden(true);
        review.setStatus("PENDING");
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try { review.setImages(MAPPER.writeValueAsString(request.getImages())); }
            catch (Exception ignored) {}
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(reviewRepository.save(review)));
    }

    @PatchMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponse> replyReview(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("reviewId") Long reviewId,
            @RequestBody Map<String, String> body) {
        HotelReview review = findReviewInHotel(hotelId, reviewId);
        review.setAdminReply(body.get("reply"));
        return ResponseEntity.ok(toResponse(reviewRepository.save(review)));
    }

    @PatchMapping("/{reviewId}/hide")
    public ResponseEntity<ReviewResponse> hideReview(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("reviewId") Long reviewId) {
        HotelReview review = findReviewInHotel(hotelId, reviewId);
        review.setHidden(true);
        return ResponseEntity.ok(toResponse(reviewRepository.save(review)));
    }

    @PatchMapping("/{reviewId}/unhide")
    public ResponseEntity<ReviewResponse> unhideReview(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("reviewId") Long reviewId) {
        HotelReview review = findReviewInHotel(hotelId, reviewId);
        review.setHidden(false);
        return ResponseEntity.ok(toResponse(reviewRepository.save(review)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("reviewId") Long reviewId) {
        reviewRepository.delete(findReviewInHotel(hotelId, reviewId));
        return ResponseEntity.noContent().build();
    }

    private HotelReview findReviewInHotel(Long hotelId, Long reviewId) {
        HotelReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!review.getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Review này không thuộc khách sạn " + hotelId);
        }
        return review;
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

