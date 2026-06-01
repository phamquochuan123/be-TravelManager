package com.example.travelManager.controller.tour;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.request.tour.TourReviewRequest;
import com.example.travelManager.domain.response.tour.TourReviewResponse;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.domain.tour.TourReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourReviewRepository;
import com.example.travelManager.service.tour.ITourService;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.constant.tour.BookingStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tours/{tourId}/reviews")
@RequiredArgsConstructor
public class TourReviewController {

    private final TourReviewRepository reviewRepository;
    private final TourBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ITourService tourService;

    @GetMapping
    public ResponseEntity<List<TourReviewResponse>> getReviews(@PathVariable("tourId") Long tourId) {
        return ResponseEntity.ok(
                reviewRepository.findByTourId(tourId)
                        .stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<TourReviewResponse> createReview(
            @PathVariable("tourId") Long tourId,
            @Valid @RequestBody TourReviewRequest request) {

        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TourBooking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Booking này không thuộc tour " + tourId);
        }
        if (booking.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("Bạn không có quyền đánh giá booking này");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ được đánh giá sau khi tour hoàn thành");
        }
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new IllegalStateException("Bạn đã đánh giá tour này rồi");
        }

        Tour tour = tourService.getTourById(tourId);

        TourReview review = new TourReview();
        review.setTour(tour);
        review.setUser(user);
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(reviewRepository.save(review)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("tourId") Long tourId,
            @PathVariable("reviewId") Long reviewId) {
        TourReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!review.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Review này không thuộc tour " + tourId);
        }
        reviewRepository.delete(review);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/reply")
    public ResponseEntity<TourReviewResponse> replyReview(
            @PathVariable("tourId") Long tourId,
            @PathVariable("reviewId") Long reviewId,
            @RequestBody java.util.Map<String, String> body) {
        TourReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!review.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Review này không thuộc tour " + tourId);
        }
        review.setAdminReply(body.get("reply"));
        return ResponseEntity.ok(toResponse(reviewRepository.save(review)));
    }

    @PatchMapping("/{reviewId}/hide")
    public ResponseEntity<TourReviewResponse> hideReview(
            @PathVariable("tourId") Long tourId,
            @PathVariable("reviewId") Long reviewId) {
        TourReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!review.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Review này không thuộc tour " + tourId);
        }
        review.setHidden(true);
        return ResponseEntity.ok(toResponse(reviewRepository.save(review)));
    }

    @PatchMapping("/{reviewId}/unhide")
    public ResponseEntity<TourReviewResponse> unhideReview(
            @PathVariable("tourId") Long tourId,
            @PathVariable("reviewId") Long reviewId) {
        TourReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!review.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Review này không thuộc tour " + tourId);
        }
        review.setHidden(false);
        return ResponseEntity.ok(toResponse(reviewRepository.save(review)));
    }

    private TourReviewResponse toResponse(TourReview r) {
        TourReviewResponse res = new TourReviewResponse();
        res.setId(r.getId());
        res.setTourId(r.getTour().getId());
        res.setUserName(r.getUser() != null ? r.getUser().getName() : "Ẩn danh");
        res.setBookingId(r.getBooking() != null ? r.getBooking().getId() : null);
        res.setRating(r.getRating());
        res.setComment(r.getComment());
        res.setAdminReply(r.getAdminReply());
        res.setHidden(r.isHidden());
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }
}

