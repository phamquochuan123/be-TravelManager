package com.example.travelManager.repository.hotel;

import com.example.travelManager.domain.BookedRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookedRoomRepository extends JpaRepository<BookedRoom, Long> {
    List<BookedRoom> findByRoom_Id(Long roomId);
    Optional<BookedRoom> findByBookingConfirmationCode(String confirmationCode);
    List<BookedRoom> findByGuestEmail(String email);
    // Tìm booking trùng ngày: existing.checkOut > newCheckIn AND existing.checkIn < newCheckOut
    List<BookedRoom> findByRoom_IdAndCheckOutDateAfterAndCheckInDateBefore(
            Long roomId, LocalDate newCheckInDate, LocalDate newCheckOutDate);
    List<BookedRoom> findByRoom_Hotel_Id(Long hotelId);
}
