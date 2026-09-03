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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
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

        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tour tour = tourService.getTourById(tourId);
        TourDeparture departure = departureRepository.findById(request.getDepartureId())
                .orElseThrow(() -> new ResourceNotFoundException("Departure not found"));

        // Giá tính theo `tour` (lấy từ path) nhưng chỗ ngồi trừ vào `departure` (lấy từ body).
        // Không đối chiếu 2 thứ này thì đặt được tour rẻ mà chiếm chỗ chuyến của tour đắt.
        if (departure.getTour() == null || !departure.getTour().getId().equals(tourId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chuyến khởi hành không thuộc tour này");
        }

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
                throw new IllegalStateException("Mã giảm giá không hợp lệ, đã hết hạn hoặc đã hết lượt sử dụng");
            }
        }

        int totalGuests = request.getNumAdults() + request.getNumChildren();

        // Hotel booking
        BigDecimal hotelPrice = BigDecimal.ZERO;
        BookedRoom bookedRoom = null;
        if (request.getRoomId() != null) {
            // Khoá bi quan giống BookedRoomServiceImpl.bookRoom — nếu luồng này dùng findById thường
            // thì khoá ở luồng kia vô tác dụng, 2 request đồng thời sẽ đặt trùng 1 phòng.
            Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.getRoomId()));
            hotelPrice = room.getRoomPrice() != null
                    ? room.getRoomPrice().multiply(BigDecimal.valueOf(Math.max(tour.getDurationNights(), 1)))
                    : BigDecimal.ZERO;

            java.time.LocalDate roomCheckIn = departure.getDepartureDate();
            java.time.LocalDate roomCheckOut = departure.getDepartureDate().plusDays(Math.max(tour.getDurationNights(), 1));
            List<BookedRoom> roomConflicts = bookedRoomRepository
                    .findByRoom_IdAndStatusNotAndCheckOutDateAfterAndCheckInDateBefore(
                            room.getId(), HotelBookingStatus.CANCELLED, roomCheckIn, roomCheckOut);
            if (!roomConflicts.isEmpty()) {
                throw new IllegalStateException("Phòng đã được đặt cho khoảng thời gian này. Vui lòng chọn phòng khác.");
            }

            bookedRoom = new BookedRoom();
            bookedRoom.setUser(user);
            bookedRoom.setRoom(room);
            bookedRoom.setCheckInDate(roomCheckIn);
            bookedRoom.setCheckOutDate(roomCheckOut);
            bookedRoom.setGuestFullName(request.getContactName());
            bookedRoom.setGuestEmail(request.getContactEmail());
            bookedRoom.setNumOfAdults(request.getNumAdults());
            bookedRoom.setNumOfChildren(request.getNumChildren());
            // Chốt giá phòng ngay lúc đặt, khớp với hotelPrice đã tính vào tổng tiền tour ở trên.
            bookedRoom.setTotalPrice(hotelPrice);
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
            java.time.LocalDate restaurantBookingDate = departure.getDepartureDate();
            java.time.LocalTime restaurantBookingTime = request.getRestaurantBookingTime() != null
                    ? request.getRestaurantBookingTime() : java.time.LocalTime.of(12, 0);
            if (restaurant.getCapacity() != null) {
                // Khoá row nhà hàng — cùng lý do như RestaurantController.book
                restaurantRepository.findByIdForUpdate(restaurant.getId());
                int alreadyBooked = restaurantBookingRepository.sumGuestCountByRestaurantAndDateTime(
                        restaurant.getId(), restaurantBookingDate, restaurantBookingTime,
                        RestaurantBookingStatus.CANCELLED);
                if (alreadyBooked + totalGuests > restaurant.getCapacity()) {
                    throw new IllegalStateException("Nhà hàng không đủ chỗ trống vào khung giờ này");
                }
            }
            restaurantBooking = new RestaurantBooking();
            restaurantBooking.setRestaurant(restaurant);
            restaurantBooking.setUser(user);
            restaurantBooking.setBookingDate(restaurantBookingDate);
            restaurantBooking.setBookingTime(restaurantBookingTime);
            restaurantBooking.setGuestCount(totalGuests);
            restaurantBooking.setContactName(request.getContactName());
            restaurantBooking.setContactPhone(request.getContactPhone());
            restaurantBooking.setContactEmail(request.getContactEmail());
            // PENDING chứ không CONFIRMED: tour đi kèm vẫn đang chờ thanh toán.
            // Xác nhận CONFIRMED sẽ do PaymentIpnService.confirmBooking làm khi tiền về.
            restaurantBooking.setStatus(RestaurantBookingStatus.PENDING);
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
        } catch (Exception e) {
            log.warn("Gửi email xác nhận đặt tour thất bại (bookingId={}): {}",
                    saved.getId(), e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @Transactional
    @GetMapping("/my")
    public ResponseEntity<List<TourBookingResponse>> myBookings() {
        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        return ResponseEntity.ok(
                bookingRepository.findByUserEmailOrderByCreatedAtDesc(email)
                        .stream().map(this::toResponse).toList());
    }

    @Transactional
    @GetMapping("/all")
    public ResponseEntity<org.springframework.data.domain.Page<TourBookingResponse>> allBookings(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        // Phân trang ở DB. Cách cũ trả toàn bộ bảng booking trong một response.
        return ResponseEntity.ok(
                bookingRepository
                        .findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(page, size))
                        .map(this::toResponse));
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

        if (booking.getBookedRoom() != null) {
            BookedRoom room = booking.getBookedRoom();
            room.setStatus(HotelBookingStatus.CANCELLED);
            bookedRoomRepository.save(room);
        }
        if (booking.getRestaurantBooking() != null) {
            RestaurantBooking restBooking = booking.getRestaurantBooking();
            restBooking.setStatus(RestaurantBookingStatus.CANCELLED);
            restaurantBookingRepository.save(restBooking);
        }
        if (booking.getCoupon() != null) {
            TourCoupon coupon = couponRepository.findByCodeForUpdate(booking.getCoupon().getCode())
                    .orElse(null);
            if (coupon != null && coupon.getUsedCount() > 0) {
                coupon.setUsedCount(coupon.getUsedCount() - 1);
                couponRepository.save(coupon);
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return ResponseEntity.ok(toResponse(bookingRepository.save(booking)));
    }

    private TourBookingResponse toResponse(TourBooking b) {
        TourBookingResponse res = new TourBookingResponse();
        res.setId(b.getId());
        res.setTourId(b.getTour().getId());
        res.setTourName(b.getTour().getName());
        res.setTourDestination(b.getTour().getDestination());
        if (b.getTour().getImages() != null && !b.getTour().getImages().isEmpty()) {
            com.example.travelManager.domain.tour.TourImage img = b.getTour().getImages().get(0);
            if (img.getImageData() != null) {
                try {
                    byte[] bytes = img.getImageData().getBytes(1, (int) img.getImageData().length());
                    res.setTourImage(java.util.Base64.getEncoder().encodeToString(bytes));
                } catch (java.sql.SQLException ignored) {}
            }
        }
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

