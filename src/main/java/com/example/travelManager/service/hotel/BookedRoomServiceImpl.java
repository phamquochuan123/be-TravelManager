package com.example.travelManager.service.hotel;

import com.example.travelManager.domain.BookedRoom;
import com.example.travelManager.domain.Room;
import com.example.travelManager.domain.RoomStatus;
import com.example.travelManager.domain.request.hotel.BookingRequest;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookedRoomServiceImpl implements IBookedRoomService {

    private final BookedRoomRepository bookedRoomRepository;
    private final RoomRepository roomRepository;

    @Override
    public String bookRoom(Long hotelId, Long roomId, BookingRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        if (room.getHotel() == null || !room.getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Room " + roomId + " does not belong to hotel " + hotelId);
        }

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new IllegalStateException("Phòng này hiện không còn trống");
        }

        // Check-in lúc 14:00 — phải đặt trước ít nhất 12 tiếng (trước 02:00 cùng ngày)
        LocalDateTime checkInAt14 = request.getCheckInDate().atTime(14, 0);
        LocalDateTime bookingDeadline = checkInAt14.minusHours(12); // = 02:00 cùng ngày check-in
        if (LocalDateTime.now().isAfter(bookingDeadline)) {
            throw new IllegalStateException(
                "Chỉ được đặt phòng trước ít nhất 12 tiếng so với giờ nhận phòng (14:00). " +
                "Hạn đặt phòng cho ngày " + request.getCheckInDate() + " đã kết thúc lúc 02:00 cùng ngày."
            );
        }

        List<BookedRoom> conflicts = bookedRoomRepository
                .findByRoom_IdAndCheckOutDateAfterAndCheckInDateBefore(
                        roomId, request.getCheckInDate(), request.getCheckOutDate());
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Room is already booked for the selected dates");
        }

        BookedRoom booking = new BookedRoom();
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setGuestFullName(request.getGuestFullName());
        booking.setGuestEmail(request.getGuestEmail());
        booking.setNumOfAdults(request.getNumOfAdults());
        booking.setNumOfChildren(request.getNumOfChildren());

        room.addBooking(booking);
        roomRepository.save(room);

        return booking.getBookingConfirmationCode();
    }

    @Override
    public BookedRoom findByConfirmationCode(String confirmationCode) {
        return bookedRoomRepository.findByBookingConfirmationCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + confirmationCode));
    }

    @Override
    public List<BookedRoom> getAllBookingsByRoomId(Long roomId) {
        return bookedRoomRepository.findByRoom_Id(roomId);
    }

    @Override
    public List<BookedRoom> getAllBookingsByHotelId(Long hotelId) {
        return bookedRoomRepository.findByRoom_Hotel_Id(hotelId);
    }

    @Override
    public List<BookedRoom> getBookingsByGuestEmail(String email) {
        return bookedRoomRepository.findByGuestEmail(email);
    }

    @Override
    public void cancelBooking(Long bookingId) {
        BookedRoom booking = bookedRoomRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        Room room = booking.getRoom();
        room.setStatus(RoomStatus.AVAILABLE);
        room.setBooked(false);
        roomRepository.save(room);

        bookedRoomRepository.deleteById(bookingId);
    }
}
