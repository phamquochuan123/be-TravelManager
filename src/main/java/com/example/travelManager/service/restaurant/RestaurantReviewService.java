package com.example.travelManager.service.restaurant;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.restaurant.RestaurantReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.repository.restaurant.RestaurantReviewRepository;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantReviewService {

    private final RestaurantReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantBookingRepository bookingRepository;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<RestaurantReview> getVisibleReviews(Long restaurantId) {
        return reviewRepository.findByRestaurantId(restaurantId).stream()
                .filter(r -> !r.isHidden())
                .toList();
    }

    public List<RestaurantReview> getAllReviews(Long restaurantId) {
        return reviewRepository.findByRestaurantId(restaurantId);
    }

    public RestaurantReview createReview(Long restaurantId, String confirmationCode, UserEntity user,
                                          int rating, String comment, List<String> images) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

        RestaurantBooking booking = bookingRepository.findByConfirmationCode(confirmationCode)
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

    public RestaurantReview reply(Long restaurantId, Long reviewId, String reply) {
        RestaurantReview review = findInRestaurant(restaurantId, reviewId);
        review.setAdminReply(reply);
        return reviewRepository.save(review);
    }

    public RestaurantReview setHidden(Long restaurantId, Long reviewId, boolean hidden) {
        RestaurantReview review = findInRestaurant(restaurantId, reviewId);
        review.setHidden(hidden);
        return reviewRepository.save(review);
    }

    public void delete(Long restaurantId, Long reviewId) {
        reviewRepository.delete(findInRestaurant(restaurantId, reviewId));
    }

    private RestaurantReview findInRestaurant(Long restaurantId, Long reviewId) {
        RestaurantReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!review.getRestaurant().getId().equals(restaurantId)) {
            throw new IllegalArgumentException("Review này không thuộc nhà hàng " + restaurantId);
        }
        return review;
    }
}
