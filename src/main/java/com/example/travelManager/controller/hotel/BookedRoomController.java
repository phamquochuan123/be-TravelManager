package com.example.travelManager.controller.hotel;

import com.example.travelManager.domain.BookedRoom;
import com.example.travelManager.domain.Hotel;
import com.example.travelManager.domain.request.hotel.BookingRequest;
import com.example.travelManager.domain.response.hotel.BookingResponse;
import com.example.travelManager.service.hotel.IBookedRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookedRoomController {

    private final IBookedRoomService bookedRoomService;

    @PostMapping("/hotels/{hotelId}/rooms/{roomId}/bookings")
    public ResponseEntity<String> bookRoom(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("roomId") Long roomId,
            @Valid @RequestBody BookingRequest request) {
        String confirmationCode = bookedRoomService.bookRoom(hotelId, roomId, request);
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
            @PathVariable("email") String email) {
        return ResponseEntity.ok(
                bookedRoomService.getBookingsByGuestEmail(email)
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
        return res;
    }
}
