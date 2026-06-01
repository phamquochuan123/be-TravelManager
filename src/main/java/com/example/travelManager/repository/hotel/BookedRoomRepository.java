package com.example.travelManager.repository.hotel;

import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.util.constant.hotel.HotelBookingStatus;
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
    // Chỉ tìm conflict với booking chưa bị hủy
    List<BookedRoom> findByRoom_IdAndStatusNotAndCheckOutDateAfterAndCheckInDateBefore(
            Long roomId, HotelBookingStatus excludeStatus,
            LocalDate newCheckInDate, LocalDate newCheckOutDate);
    List<BookedRoom> findByRoom_Hotel_Id(Long hotelId);
    boolean existsByRoom_Id(Long roomId);
}
