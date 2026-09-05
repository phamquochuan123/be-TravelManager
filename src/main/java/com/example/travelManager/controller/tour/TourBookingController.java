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
import com.example.travelManager.util.constant.restaurant.MealSlot;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import com.example.travelManager.service.tour.TourRestaurantAddonService;
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
    private final com.example.travelManager.service.tour.TourSeasonalPriceService seasonalPriceService;
    private final com.example.travelManager.service.tour.TourRestaurantAddonService restaurantAddonService;
    private final EmailService emailService;

    /** Chuyến ở các trạng thái này không nhận đặt nữa, dù còn chỗ. */
    private static final java.util.Set<com.example.travelManager.util.constant.tour.TourDepartureStatus>
            TRANG_THAI_KHONG_DAT_DUOC = java.util.EnumSet.of(
                    com.example.travelManager.util.constant.tour.TourDepartureStatus.CANCELLED,
                    com.example.travelManager.util.constant.tour.TourDepartureStatus.COMPLETED,
                    com.example.travelManager.util.constant.tour.TourDepartureStatus.ONGOING,
                    com.example.travelManager.util.constant.tour.TourDepartureStatus.IN_PROGRESS);

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

        // Chuyến đã qua ngày đi vẫn còn availableSlots > 0, nên nếu chỉ dựa vào
        // số chỗ thì khách đặt được một chuyến đã khởi hành từ lâu.
        if (departure.getDepartureDate() == null
                || departure.getDepartureDate().isBefore(java.time.LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chuyến khởi hành này đã qua ngày đi, không đặt được nữa");
        }
        if (TRANG_THAI_KHONG_DAT_DUOC.contains(departure.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chuyến khởi hành này đã bị huỷ hoặc đã kết thúc");
        }

        // Giá tính theo NGÀY KHỞI HÀNH: nếu ngày đó rơi vào một mùa đã khai báo
        // (tour_seasonal_prices) thì dùng giá mùa, không thì dùng giá mặc định của tour.
        var giaHieuLuc = seasonalPriceService.resolvePrice(tour, departure.getDepartureDate());
        BigDecimal priceAdult = giaHieuLuc.adult();
        BigDecimal priceChild = giaHieuLuc.child();
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

        // Các bữa ăn kèm được dựng sau khi đã có đối tượng TourBooking (xem bên dưới):
        // khoá ngoại nay nằm ở restaurant_bookings.tour_booking_id nên mỗi bữa cần
        // tham chiếu ngược về đơn. Ở đây chỉ cần biết CÓ chọn bữa nào không, để xét
        // giảm giá combo.
        boolean coChonNhaHang = request.getRestaurants() != null && !request.getRestaurants().isEmpty();

        // Apply package discount — restaurant is paid on-site, not through VNPay
        BigDecimal rawTotal = original.add(hotelPrice);
        BigDecimal packageDiscountAmt = BigDecimal.ZERO;
        if (tour.getPackageDiscountPercent() != null && tour.getPackageDiscountPercent() > 0
                && (bookedRoom != null || coChonNhaHang)) {
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
        booking.setContactName(request.getContactName());
        booking.setContactPhone(request.getContactPhone());
        booking.setContactEmail(request.getContactEmail());
        booking.setNumAdults(request.getNumAdults());
        booking.setNumChildren(request.getNumChildren());
        booking.setOriginalPrice(original);
        booking.setPackageHotelPrice(hotelPrice);
        booking.setDiscountAmount(totalDiscount);
        booking.setFinalPrice(finalPrice);
        booking.setNote(request.getNote());

        // Dựng các bữa ăn và gắn vào đơn. cascade = ALL trên TourBooking.restaurantBookings
        // lo phần ghi xuống DB, nên không gọi restaurantBookingRepository.save ở đây —
        // lưu tay trước khi đơn tour có id sẽ để lại bữa mồ côi nếu đoạn sau ném lỗi.
        //
        // PENDING chứ không CONFIRMED: tour đi kèm vẫn đang chờ thanh toán. Xác nhận
        // CONFIRMED sẽ do PaymentIpnService.confirmBooking làm khi tiền về.
        TourRestaurantAddonService.KetQua buaAn = restaurantAddonService.dungCacBua(
                request.getRestaurants(), tour, departure.getDepartureDate(), booking, user,
                totalGuests, request.getContactName(), request.getContactPhone(),
                request.getContactEmail(), RestaurantBookingStatus.PENDING, true);
        booking.getRestaurantBookings().addAll(buaAn.buaAn());
        booking.setPackageRestaurantPrice(buaAn.tongTien());

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
        // Huỷ TẤT CẢ bữa của đơn. Bản cũ chỉ huỷ đúng một bữa; nay một đơn kèm nhiều
        // bữa nên bỏ sót là những bữa còn lại vẫn giữ chỗ ở nhà hàng dù tour đã huỷ.
        for (RestaurantBooking restBooking : booking.getRestaurantBookings()) {
            restBooking.setStatus(RestaurantBookingStatus.CANCELLED);
        }
        restaurantBookingRepository.saveAll(booking.getRestaurantBookings());
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
        for (RestaurantBooking rb : b.getRestaurantBookings()) {
            TourBookingResponse.BuaAn bua = new TourBookingResponse.BuaAn();
            bua.setRestaurantBookingId(rb.getId());
            if (rb.getRestaurant() != null) {
                bua.setRestaurantId(rb.getRestaurant().getId());
                bua.setRestaurantName(rb.getRestaurant().getName());
            }
            bua.setBookingDate(rb.getBookingDate());
            bua.setBookingTime(rb.getBookingTime());
            MealSlot khung = MealSlot.cuaGio(rb.getBookingTime());
            if (khung != null) {
                bua.setMealSlot(khung.name());
                bua.setMealSlotLabel(khung.nhan());
            }
            bua.setGuestCount(rb.getGuestCount());
            bua.setConfirmationCode(rb.getConfirmationCode());
            res.getRestaurants().add(bua);
        }
        return res;
    }
}

