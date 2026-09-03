package com.example.travelManager.service.hotel;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.hotel.HotelReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.hotel.HotelReviewRepository;
import com.example.travelManager.util.constant.hotel.HotelBookingStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelReviewService {

    private final HotelReviewRepository reviewRepository;
    private final HotelRepository hotelRepository;
    private final BookedRoomRepository bookedRoomRepository;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<HotelReview> getVisibleReviews(Long hotelId) {
        return reviewRepository.findByHotelId(hotelId).stream()
                .filter(r -> !r.isHidden())
                .toList();
    }

    public List<HotelReview> getAllReviews(Long hotelId) {
        return reviewRepository.findByHotelId(hotelId);
    }

    public HotelReview createReview(Long hotelId, String confirmationCode, String currentEmail,
                                     UserEntity user, int rating, String comment, List<String> images) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + hotelId));

        BookedRoom booking = bookedRoomRepository.findByBookingConfirmationCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getRoom().getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Booking này không thuộc khách sạn " + hotelId);
        }
        if (!booking.getGuestEmail().equalsIgnoreCase(currentEmail)) {
            throw new IllegalArgumentException("Booking này không thuộc về bạn");
        }
        if (booking.getStatus() == HotelBookingStatus.CANCELLED) {
            throw new IllegalStateException("Không thể đánh giá booking đã bị hủy");
        }
        boolean checkoutPassed = !booking.getCheckOutDate().isAfter(LocalDate.now());
        boolean isCompleted = booking.getStatus() == HotelBookingStatus.COMPLETED;
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

    public HotelReview reply(Long hotelId, Long reviewId, String reply) {
        HotelReview review = findInHotel(hotelId, reviewId);
        review.setAdminReply(reply);
        return reviewRepository.save(review);
    }

    public HotelReview setHidden(Long hotelId, Long reviewId, boolean hidden) {
        HotelReview review = findInHotel(hotelId, reviewId);
        review.setHidden(hidden);
        return reviewRepository.save(review);
    }

    public void delete(Long hotelId, Long reviewId) {
        reviewRepository.delete(findInHotel(hotelId, reviewId));
    }

    private HotelReview findInHotel(Long hotelId, Long reviewId) {
        HotelReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!review.getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Review này không thuộc khách sạn " + hotelId);
        }
        return review;
    }
}
