package com.example.travelManager.service.hotel;

import com.example.travelManager.domain.BookedRoom;
import com.example.travelManager.domain.request.hotel.BookingRequest;

import java.util.List;

public interface IBookedRoomService {
    String bookRoom(Long hotelId, Long roomId, BookingRequest request);
    BookedRoom findByConfirmationCode(String confirmationCode);
    List<BookedRoom> getAllBookingsByRoomId(Long roomId);
    List<BookedRoom> getAllBookingsByHotelId(Long hotelId);
    List<BookedRoom> getBookingsByGuestEmail(String email);
    void cancelBooking(Long bookingId);
}
