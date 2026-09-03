package com.example.travelManager.controller.tour;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.request.tour.TourReviewRequest;
import com.example.travelManager.domain.response.tour.TourReviewResponse;
import com.example.travelManager.domain.tour.TourReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.service.tour.TourReviewService;
import com.example.travelManager.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/tours/{tourId}/reviews")
@RequiredArgsConstructor
public class TourReviewController {

    private final TourReviewService reviewService;
    private final UserRepository userRepository;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping
    public ResponseEntity<List<TourReviewResponse>> getReviews(@PathVariable("tourId") Long tourId) {
        return ResponseEntity.ok(
                reviewService.getReviews(tourId).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<TourReviewResponse> createReview(
            @PathVariable("tourId") Long tourId,
            @Valid @RequestBody TourReviewRequest request) {

        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TourReview review = reviewService.createReview(tourId, request.getBookingId(), user,
                request.getRating(), request.getComment(), request.getImages());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(review));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("tourId") Long tourId,
            @PathVariable("reviewId") Long reviewId) {
        reviewService.delete(tourId, reviewId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/reply")
    public ResponseEntity<TourReviewResponse> replyReview(
            @PathVariable("tourId") Long tourId,
            @PathVariable("reviewId") Long reviewId,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(toResponse(reviewService.reply(tourId, reviewId, body.get("reply"))));
    }

    @PatchMapping("/{reviewId}/hide")
    public ResponseEntity<TourReviewResponse> hideReview(
            @PathVariable("tourId") Long tourId,
            @PathVariable("reviewId") Long reviewId) {
        return ResponseEntity.ok(toResponse(reviewService.setHidden(tourId, reviewId, true)));
    }

    @PatchMapping("/{reviewId}/unhide")
    public ResponseEntity<TourReviewResponse> unhideReview(
            @PathVariable("tourId") Long tourId,
            @PathVariable("reviewId") Long reviewId) {
        return ResponseEntity.ok(toResponse(reviewService.setHidden(tourId, reviewId, false)));
    }

    private TourReviewResponse toResponse(TourReview r) {
        TourReviewResponse res = new TourReviewResponse();
        res.setId(r.getId());
        res.setTourId(r.getTour().getId());
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
}
