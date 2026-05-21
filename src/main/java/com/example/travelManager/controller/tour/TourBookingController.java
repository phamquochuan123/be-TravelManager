package com.example.travelManager.controller.tour;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.request.tour.TourBookingRequest;
import com.example.travelManager.domain.response.tour.TourBookingResponse;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.domain.tour.TourCoupon;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourCouponRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.service.EmailService;
import com.example.travelManager.service.tour.ITourService;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.constant.tour.BookingStatus;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/tour-bookings")
@RequiredArgsConstructor
public class TourBookingController {

    private final ITourService tourService;
    private final TourBookingRepository bookingRepository;
    private final TourDepartureRepository departureRepository;
    private final TourCouponRepository couponRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    @PostMapping("/tours/{tourId}")
    public ResponseEntity<TourBookingResponse> book(
            @PathVariable Long tourId,
            @Valid @RequestBody TourBookingRequest request) {

        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tour tour = tourService.getTourById(tourId);
        TourDeparture departure = departureRepository.findById(request.getDepartureId())
                .orElseThrow(() -> new ResourceNotFoundException("Departure not found"));

        if (departure.getAvailableSlots() < request.getNumAdults() + request.getNumChildren()) {
            throw new IllegalStateException("Không đủ chỗ trống cho ngày khởi hành này");
        }

        BigDecimal priceAdult = tour.getPriceAdult();
        BigDecimal priceChild = tour.getPriceChild() != null ? tour.getPriceChild() : priceAdult;
        BigDecimal original = priceAdult.multiply(BigDecimal.valueOf(request.getNumAdults()))
                .add(priceChild.multiply(BigDecimal.valueOf(request.getNumChildren())));

        BigDecimal discount = BigDecimal.ZERO;
        TourCoupon coupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponRepository.findByCode(request.getCouponCode()).orElse(null);
            if (coupon != null && coupon.isActive()
                    && coupon.getUsedCount() < coupon.getUsageLimit()
                    && !java.time.LocalDate.now().isAfter(coupon.getEndDate())
                    && !java.time.LocalDate.now().isBefore(coupon.getStartDate())
                    && (coupon.getMinOrderValue() == null || original.compareTo(coupon.getMinOrderValue()) >= 0)) {
                if (coupon.getCouponType() == com.example.travelManager.util.constant.tour.CouponType.PERCENT) {
                    discount = original.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
                } else {
                    discount = coupon.getDiscountValue();
                }
                coupon.setUsedCount(coupon.getUsedCount() + 1);
                // Save ngay để lock — tránh race condition khi 2 user dùng cùng coupon đồng thời
                couponRepository.save(coupon);
            } else {
                coupon = null;
            }
        }

        TourBooking booking = new TourBooking();
        booking.setTour(tour);
        booking.setDeparture(departure);
        booking.setUser(user);
        booking.setCoupon(coupon);
        booking.setContactName(request.getContactName());
        booking.setContactPhone(request.getContactPhone());
        booking.setContactEmail(request.getContactEmail());
        booking.setNumAdults(request.getNumAdults());
        booking.setNumChildren(request.getNumChildren());
        booking.setOriginalPrice(original);
        booking.setDiscountAmount(discount);
        booking.setFinalPrice(original.subtract(discount));
        booking.setNote(request.getNote());

        // Save booking trước, sau đó mới giảm slot
        // → nếu save booking fail, slot không bị mất
        TourBooking saved = bookingRepository.save(booking);

        departure.setAvailableSlots(departure.getAvailableSlots() - request.getNumAdults() - request.getNumChildren());
        departureRepository.save(departure);

        try {
            emailService.sendTourBookingConfirmation(
                    request.getContactEmail(),
                    request.getContactName(),
                    tour.getName(),
                    departure.getDepartureDate().toString(),
                    request.getNumAdults(),
                    request.getNumChildren(),
                    saved.getFinalPrice(),
                    saved.getId().toString());
        } catch (Exception ignored) {}

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TourBookingResponse>> myBookings() {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        return ResponseEntity.ok(
                bookingRepository.findByUserEmailOrderByCreatedAtDesc(email)
                        .stream().map(this::toResponse).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<TourBookingResponse>> allBookings() {
        return ResponseEntity.ok(
                bookingRepository.findAllByOrderByCreatedAtDesc()
                        .stream().map(this::toResponse).toList());
    }

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<TourBookingResponse> updateStatus(
            @PathVariable Long bookingId,
            @RequestParam("status") BookingStatus status) {
        TourBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        booking.setStatus(status);
        return ResponseEntity.ok(toResponse(bookingRepository.save(booking)));
    }

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<TourBookingResponse> cancel(@PathVariable Long bookingId) {
        TourBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking đã được hủy trước đó");
        }
        // Hoàn lại slot
        TourDeparture dep = booking.getDeparture();
        dep.setAvailableSlots(dep.getAvailableSlots() + booking.getNumAdults() + booking.getNumChildren());
        departureRepository.save(dep);

        booking.setStatus(BookingStatus.CANCELLED);
        return ResponseEntity.ok(toResponse(bookingRepository.save(booking)));
    }

    private TourBookingResponse toResponse(TourBooking b) {
        TourBookingResponse res = new TourBookingResponse();
        res.setId(b.getId());
        res.setTourId(b.getTour().getId());
        res.setTourName(b.getTour().getName());
        res.setTourDestination(b.getTour().getDestination());
        res.setDepartureId(b.getDeparture().getId());
        res.setDepartureDate(b.getDeparture().getDepartureDate());
        res.setNumAdults(b.getNumAdults());
        res.setNumChildren(b.getNumChildren());
        res.setContactName(b.getContactName());
        res.setContactPhone(b.getContactPhone());
        res.setContactEmail(b.getContactEmail());
        res.setOriginalPrice(b.getOriginalPrice());
        res.setDiscountAmount(b.getDiscountAmount());
        res.setFinalPrice(b.getFinalPrice());
        res.setStatus(b.getStatus());
        res.setNote(b.getNote());
        res.setCreatedAt(b.getCreatedAt());
        return res;
    }
}
