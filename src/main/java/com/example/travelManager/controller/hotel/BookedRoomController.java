package com.example.travelManager.controller.hotel;

import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.request.hotel.BookingRequest;
import com.example.travelManager.domain.response.hotel.BookingResponse;
import com.example.travelManager.service.EmailService;
import com.example.travelManager.service.hotel.IBookedRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookedRoomController {

    private final IBookedRoomService bookedRoomService;
    private final EmailService emailService;

    @PostMapping("/hotels/{hotelId}/rooms/{roomId}/bookings")
    public ResponseEntity<String> bookRoom(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("roomId") Long roomId,
            @Valid @RequestBody BookingRequest request) {
        String confirmationCode = bookedRoomService.bookRoom(hotelId, roomId, request);
        try {
            BookedRoom booking = bookedRoomService.findByConfirmationCode(confirmationCode);
            Hotel hotel = booking.getRoom() != null ? booking.getRoom().getHotel() : null;
            emailService.sendHotelBookingConfirmation(
                    request.getGuestEmail(),
                    request.getGuestFullName(),
                    hotel != null ? hotel.getName() : "N/A",
                    booking.getRoom() != null ? booking.getRoom().getRoomNumber() : "N/A",
                    request.getCheckInDate().toString(),
                    request.getCheckOutDate().toString(),
                    confirmationCode);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(confirmationCode);
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

    @GetMapping("/bookings/all")
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(
                bookedRoomService.getAllBookings()
                        .stream().map(this::toResponse).toList());
    }

    @GetMapping("/bookings/room/{roomId}")
    public ResponseEntity<List<BookingResponse>> getByRoomId(
            @PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(
                bookedRoomService.getAllBookingsByRoomId(roomId)
                        .stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> cancelBooking(@PathVariable("bookingId") Long bookingId) {
        bookedRoomService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
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
        res.setRoomType(b.getRoom() != null ? b.getRoom().getRoomType() : null);
        res.setStatus("CONFIRMED");
        return res;
    }
}
