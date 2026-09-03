package com.example.travelManager.service.tour;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.domain.tour.TourReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourReviewRepository;
import com.example.travelManager.util.constant.tour.BookingStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourReviewService {

    private final TourReviewRepository reviewRepository;
    private final TourBookingRepository bookingRepository;
    private final ITourService tourService;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<TourReview> getReviews(Long tourId) {
        return reviewRepository.findByTourId(tourId);
    }

    public TourReview createReview(Long tourId, Long bookingId, UserEntity user,
                                    int rating, String comment, List<String> images) {
        TourBooking booking = bookingRepository.findById(bookingId)
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
        review.setRating(rating);
        review.setComment(comment);
        review.setHidden(true);
        review.setStatus("PENDING");
        if (images != null && !images.isEmpty()) {
            try { review.setImages(MAPPER.writeValueAsString(images)); }
            catch (Exception ignored) {}
        }
        return reviewRepository.save(review);
    }

    public void delete(Long tourId, Long reviewId) {
        reviewRepository.delete(findInTour(tourId, reviewId));
    }

    public TourReview reply(Long tourId, Long reviewId, String reply) {
        TourReview review = findInTour(tourId, reviewId);
        review.setAdminReply(reply);
        return reviewRepository.save(review);
    }

    public TourReview setHidden(Long tourId, Long reviewId, boolean hidden) {
        TourReview review = findInTour(tourId, reviewId);
        review.setHidden(hidden);
        return reviewRepository.save(review);
    }

    private TourReview findInTour(Long tourId, Long reviewId) {
        TourReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!review.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Review này không thuộc tour " + tourId);
        }
        return review;
    }
}
