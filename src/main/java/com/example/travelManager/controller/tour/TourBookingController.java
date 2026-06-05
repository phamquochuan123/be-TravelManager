package com.example.travelManager.controller.tour;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.hotel.Room;
import com.example.travelManager.domain.request.tour.TourBookingRequest;
import com.example.travelManager.domain.response.tour.TourBookingResponse;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.domain.tour.TourCoupon;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.RoomRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourCouponRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.service.EmailService;
import com.example.travelManager.service.tour.ITourService;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.constant.hotel.HotelBookingStatus;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import com.example.travelManager.util.constant.tour.BookingStatus;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
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
    private final RoomRepository roomRepository;
    private final BookedRoomRepository bookedRoomRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;
    private final EmailService emailService;

    @Transactional
    @PostMapping("/tours/{tourId}")
    public ResponseEntity<TourBookingResponse> book(
            @PathVariable("tourId") Long tourId,
            @Valid @RequestBody TourBookingRequest request) {

        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tour tour = tourService.getTourById(tourId);
        TourDeparture departure = departureRepository.findById(request.getDepartureId())
                .orElseThrow(() -> new ResourceNotFoundException("Departure not found"));

        BigDecimal priceAdult = tour.getPriceAdult();
        BigDecimal priceChild = tour.getPriceChild() != null ? tour.getPriceChild() : priceAdult;
        BigDecimal original = priceAdult.multiply(BigDecimal.valueOf(request.getNumAdults()))
                .add(priceChild.multiply(BigDecimal.valueOf(request.getNumChildren())));

        BigDecimal discount = BigDecimal.ZERO;
        TourCoupon coupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponRepository.findByCodeForUpdate(request.getCouponCode()).orElse(null);
            if (coupon != null && coupon.isActive()
                    && coupon.getUsedCount() < coupon.getUsageLimit()
                    && !java.time.LocalDate.now().isAfter(coupon.getEndDate())
                    && !java.time.LocalDate.now().isBefore(coupon.getStartDate())
                    && (coupon.getMinOrderValue() == null || original.compareTo(coupon.getMinOrderValue()) >= 0)) {
                if (coupon.getCouponType() == com.example.travelManager.util.constant.tour.CouponType.PERCENT) {
                    discount = original.multiply(coupon.getDiscountValue())
                                       .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
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

        int totalGuests = request.getNumAdults() + request.getNumChildren();

        // Hotel booking
        BigDecimal hotelPrice = BigDecimal.ZERO;
        BookedRoom bookedRoom = null;
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.getRoomId()));
            hotelPrice = room.getRoomPrice() != null
                    ? room.getRoomPrice().multiply(BigDecimal.valueOf(Math.max(tour.getDurationNights(), 1)))
                    : BigDecimal.ZERO;
            bookedRoom = new BookedRoom();
            bookedRoom.setRoom(room);
            bookedRoom.setCheckInDate(departure.getDepartureDate());
            bookedRoom.setCheckOutDate(departure.getDepartureDate().plusDays(Math.max(tour.getDurationNights(), 1)));
            bookedRoom.setGuestFullName(request.getContactName());
            bookedRoom.setGuestEmail(request.getContactEmail());
            bookedRoom.setNumOfAdults(request.getNumAdults());
            bookedRoom.setNumOfChildren(request.getNumChildren());
            bookedRoom.calculateTotalNumOfGuests();
            bookedRoom.setStatus(HotelBookingStatus.PENDING);
            bookedRoom.setBookingConfirmationCode(org.apache.commons.lang3.RandomStringUtils.randomNumeric(10));
            bookedRoom = bookedRoomRepository.save(bookedRoom);
        }

        // Restaurant booking
        BigDecimal restaurantPrice = BigDecimal.ZERO;
        RestaurantBooking restaurantBooking = null;
        if (request.getRestaurantId() != null) {
            Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + request.getRestaurantId()));
            if (restaurant.getPricePerPerson() != null) {
                restaurantPrice = restaurant.getPricePerPerson().multiply(BigDecimal.valueOf(totalGuests));
            }
            restaurantBooking = new RestaurantBooking();
            restaurantBooking.setRestaurant(restaurant);
            restaurantBooking.setUser(user);
            restaurantBooking.setBookingDate(departure.getDepartureDate());
            restaurantBooking.setBookingTime(
                    request.getRestaurantBookingTime() != null ? request.getRestaurantBookingTime()
                            : java.time.LocalTime.of(12, 0));
            restaurantBooking.setGuestCount(totalGuests);
            restaurantBooking.setContactName(request.getContactName());
            restaurantBooking.setContactPhone(request.getContactPhone());
            restaurantBooking.setContactEmail(request.getContactEmail());
            restaurantBooking.setStatus(RestaurantBookingStatus.CONFIRMED);
            restaurantBooking.setConfirmationCode(org.apache.commons.lang3.RandomStringUtils.randomNumeric(10));
            restaurantBooking = restaurantBookingRepository.save(restaurantBooking);
        }

        // Apply package discount — restaurant is paid on-site, not through VNPay
        BigDecimal rawTotal = original.add(hotelPrice);
        BigDecimal packageDiscountAmt = BigDecimal.ZERO;
        if (tour.getPackageDiscountPercent() != null && tour.getPackageDiscountPercent() > 0
                && (bookedRoom != null || restaurantBooking != null)) {
            packageDiscountAmt = rawTotal
                    .multiply(BigDecimal.valueOf(tour.getPackageDiscountPercent() / 100.0))
                    .setScale(0, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal totalDiscount = packageDiscountAmt.add(discount);
        BigDecimal finalPrice = rawTotal.subtract(totalDiscount);
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) finalPrice = BigDecimal.ZERO;

        TourBooking booking = new TourBooking();
        booking.setTour(tour);
        booking.setDeparture(departure);
        booking.setUser(user);
        booking.setCoupon(coupon);
        booking.setBookedRoom(bookedRoom);
        booking.setRestaurantBooking(restaurantBooking);
        booking.setContactName(request.getContactName());
        booking.setContactPhone(request.getContactPhone());
        booking.setContactEmail(request.getContactEmail());
        booking.setNumAdults(request.getNumAdults());
        booking.setNumChildren(request.getNumChildren());
        booking.setOriginalPrice(original);
        booking.setPackageHotelPrice(hotelPrice);
        booking.setPackageRestaurantPrice(restaurantPrice);
        booking.setDiscountAmount(totalDiscount);
        booking.setFinalPrice(finalPrice);
        booking.setNote(request.getNote());

        int needed = request.getNumAdults() + request.getNumChildren();
        if (departureRepository.decrementSlot(departure.getId(), needed) == 0) {
            throw new IllegalStateException("Hết chỗ trống");
        }
        TourBooking saved = bookingRepository.save(booking);

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
            @PathVariable("bookingId") Long bookingId,
            @RequestParam("status") BookingStatus status) {
        TourBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        booking.setStatus(status);
        return ResponseEntity.ok(toResponse(bookingRepository.save(booking)));
    }

    @Transactional
    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<TourBookingResponse> cancel(@PathVariable("bookingId") Long bookingId) {
        TourBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        String currentEmail = com.example.travelManager.util.SecurityUtil
                .getCurrentUserLogin().orElseThrow();
        UserEntity currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean isPrivileged = currentUser.getRole() != null &&
                ("ADMIN".equals(currentUser.getRole().getName()) ||
                 "STAFF".equals(currentUser.getRole().getName()));
        if (!isPrivileged && !booking.getUser().getEmail().equals(currentEmail)) {
            throw new AccessDeniedException("Không có quyền hủy booking này");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking đã được hủy trước đó");
        }

        departureRepository.incrementSlot(
                booking.getDeparture().getId(),
                booking.getNumAdults() + booking.getNumChildren());

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
        res.setPackageHotelPrice(b.getPackageHotelPrice());
        res.setPackageRestaurantPrice(b.getPackageRestaurantPrice());
        res.setDiscountAmount(b.getDiscountAmount());
        res.setFinalPrice(b.getFinalPrice());
        res.setPackageDiscountPercent(b.getTour().getPackageDiscountPercent());
        res.setStatus(b.getStatus());
        res.setNote(b.getNote());
        res.setCreatedAt(b.getCreatedAt());
        if (b.getBookedRoom() != null) {
            BookedRoom br = b.getBookedRoom();
            res.setBookedRoomId(br.getBookingId());
            if (br.getRoom() != null) {
                res.setRoomType(br.getRoom().getRoomType());
                if (br.getRoom().getHotel() != null) {
                    res.setHotelId(br.getRoom().getHotel().getId());
                    res.setHotelName(br.getRoom().getHotel().getName());
                }
            }
            res.setCheckInDate(br.getCheckInDate());
            res.setCheckOutDate(br.getCheckOutDate());
        }
        if (b.getRestaurantBooking() != null) {
            RestaurantBooking rb = b.getRestaurantBooking();
            res.setRestaurantBookingId(rb.getId());
            if (rb.getRestaurant() != null) {
                res.setRestaurantId(rb.getRestaurant().getId());
                res.setRestaurantName(rb.getRestaurant().getName());
            }
        }
        return res;
    }
}

