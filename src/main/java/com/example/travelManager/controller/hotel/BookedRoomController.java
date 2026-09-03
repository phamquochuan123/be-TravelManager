package com.example.travelManager.controller.hotel;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.request.hotel.BookingRequest;
import com.example.travelManager.domain.response.hotel.BookingResponse;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.service.EmailService;
import com.example.travelManager.service.hotel.IBookedRoomService;
import com.example.travelManager.util.SecurityUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BookedRoomController {

    private final IBookedRoomService bookedRoomService;
    private final EmailService emailService;
    private final TourBookingRepository tourBookingRepository;
    private final HotelRepository hotelRepository;
    private final BookedRoomRepository bookedRoomRepository;
    private final UserRepository userRepository;

    @PostMapping("/hotels/{hotelId}/rooms/{roomId}/bookings")
    public ResponseEntity<BookingResponse> bookRoom(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("roomId") Long roomId,
            @Valid @RequestBody BookingRequest request) {
        if (request.getTourBookingId() != null) {
            TourBooking tourBooking = tourBookingRepository.findById(request.getTourBookingId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Tour booking không tồn tại: " + request.getTourBookingId()));
            String currentEmail = SecurityUtil.getCurrentUserLogin().orElse("");
            if (!tourBooking.getUser().getEmail().equalsIgnoreCase(currentEmail)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Tour booking không thuộc về người dùng hiện tại");
            }
            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Hotel không tồn tại: " + hotelId));
            String tourDest  = tourBooking.getTour().getDestination();
            String hotelCity = hotel.getCity();
            if (tourDest != null && hotelCity != null) {
                String d = tourDest.toLowerCase().split("[–\\-/,]")[0].trim();
                String c = hotelCity.toLowerCase();
                if (!c.contains(d) && !d.contains(c)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Khách sạn ở " + hotelCity + " không phù hợp với tour đến " + tourDest);
                }
            }
        }
        String confirmationCode = bookedRoomService.bookRoom(hotelId, roomId, request,
                SecurityUtil.getCurrentUserLogin().orElse(null));
        BookedRoom booking = bookedRoomService.findByConfirmationCode(confirmationCode);
        try {
            Hotel hotel = booking.getRoom() != null ? booking.getRoom().getHotel() : null;
            emailService.sendHotelBookingConfirmation(
                    request.getGuestEmail(),
                    request.getGuestFullName(),
                    hotel != null ? hotel.getName() : "N/A",
                    booking.getRoom() != null ? booking.getRoom().getRoomNumber() : "N/A",
                    request.getCheckInDate().toString(),
                    request.getCheckOutDate().toString(),
                    confirmationCode);
        } catch (Exception e) {
            log.warn("Gửi email xác nhận đặt phòng thất bại (confirmationCode={}): {}",
                    confirmationCode, e.getMessage());
        }
        BookingResponse res = new BookingResponse();
        res.setId(booking.getBookingId());
        res.setBookingConfirmationCode(confirmationCode);
        res.setCheckInDate(booking.getCheckInDate());
        res.setCheckOutDate(booking.getCheckOutDate());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/hotels/{hotelId}/bookings")
    public ResponseEntity<List<BookingResponse>> getBookingsByHotel(
            @PathVariable("hotelId") Long hotelId) {
        return ResponseEntity.ok(
                bookedRoomService.getAllBookingsByHotelId(hotelId)
                        .stream().map(this::toResponse).toList());
    }

    @GetMapping("/bookings/confirmation/{confirmationCode}")
    public ResponseEntity<BookingResponse> getByConfirmationCode(
            @PathVariable("confirmationCode") String confirmationCode) {
        BookedRoom booking = bookedRoomService.findByConfirmationCode(confirmationCode);
        return ResponseEntity.ok(toResponse(booking));
    }

    @Transactional
    @GetMapping("/bookings/guest/{email}")
    public ResponseEntity<List<BookingResponse>> getByGuestEmail(
            @PathVariable("email") String email,
            @CurrentSecurityContext(expression = "authentication") org.springframework.security.core.Authentication auth) {
        // Chỉ cho xem booking của chính mình, trừ ADMIN/STAFF
        boolean isPrivileged = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        if (!isPrivileged && (auth == null || !auth.getName().equalsIgnoreCase(email))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xem booking của người khác");
        }
        return ResponseEntity.ok(
                bookedRoomService.getBookingsByGuestEmail(email)
                        .stream().map(this::toResponse).toList());
    }

    @Transactional
    @GetMapping("/bookings/all")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        // Phân trang ở DB. Cách cũ trả toàn bộ bảng booking trong một response.
        return ResponseEntity.ok(
                bookedRoomRepository.findAllForAdmin(PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/bookings/room/{roomId}")
    public ResponseEntity<List<BookingResponse>> getByRoomId(
            @PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(
                bookedRoomService.getAllBookingsByRoomId(roomId)
                        .stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> cancelBooking(@PathVariable("bookingId") Long bookingId) {
        BookedRoom booking = bookedRoomRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Booking not found: " + bookingId));

        String currentEmail = SecurityUtil.getCurrentUserLogin().orElseThrow();
        UserEntity currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        boolean isPrivileged = currentUser.getRole() != null &&
                ("ADMIN".equals(currentUser.getRole().getName()) ||
                 "STAFF".equals(currentUser.getRole().getName()));
        if (!isPrivileged && !isOwnedBy(booking, currentUser, currentEmail)) {
            throw new AccessDeniedException("Không có quyền hủy booking này");
        }

        bookedRoomService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Booking thuộc về user hiện tại hay không.
     * Ưu tiên cột user_id (chủ sở hữu thật). Chỉ fallback về guestEmail cho những
     * booking cũ tạo trước khi có cột user_id — dữ liệu mới luôn có user.
     */
    private boolean isOwnedBy(BookedRoom booking, UserEntity currentUser, String currentEmail) {
        if (booking.getUser() != null) {
            return booking.getUser().getId() == currentUser.getId();
        }
        return booking.getGuestEmail() != null
                && booking.getGuestEmail().equalsIgnoreCase(currentEmail);
    }

    private BookingResponse toResponse(BookedRoom b) {
        Hotel hotel = b.getRoom() != null ? b.getRoom().getHotel() : null;
        BookingResponse res = new BookingResponse();
        res.setId(b.getBookingId());
        res.setCheckInDate(b.getCheckInDate());
        res.setCheckOutDate(b.getCheckOutDate());
        res.setGuestFullName(b.getGuestFullName());
        res.setGuestEmail(b.getGuestEmail());
        res.setNumOfGuests(b.getNumOfAdults());
        res.setNumOfChildren(b.getNumOfChildren());
        res.setTotalNumOfGuests(b.getTotalNumOfGuests());
        res.setBookingConfirmationCode(b.getBookingConfirmationCode());
        res.setHotelId(hotel != null ? hotel.getId() : null);
        res.setHotelName(hotel != null ? hotel.getName() : null);
        if (hotel != null && hotel.getPhoto() != null) {
            try {
                byte[] bytes = hotel.getPhoto().getBytes(1, (int) hotel.getPhoto().length());
                res.setHotelPhoto(java.util.Base64.getEncoder().encodeToString(bytes));
            } catch (java.sql.SQLException ignored) {}
        }
        res.setRoomType(b.getRoom() != null ? b.getRoom().getRoomType() : null);
        res.setStatus(b.getStatus() != null ? b.getStatus().name() : "PENDING");
        return res;
    }
}
