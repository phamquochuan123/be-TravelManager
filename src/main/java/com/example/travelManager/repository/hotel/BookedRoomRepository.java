package com.example.travelManager.repository.hotel;

import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.util.constant.hotel.HotelBookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookedRoomRepository extends JpaRepository<BookedRoom, Long> {

    /**
     * Danh sách booking cho màn quản trị — phân trang ở DB.
     * Fetch join room + hotel để tránh N+1 khi map sang response.
     */
    @Query(value = "SELECT b FROM BookedRoom b LEFT JOIN FETCH b.room r LEFT JOIN FETCH r.hotel",
           countQuery = "SELECT COUNT(b) FROM BookedRoom b")
    Page<BookedRoom> findAllForAdmin(Pageable pageable);

    List<BookedRoom> findByRoom_Id(Long roomId);
    List<BookedRoom> findByRoom_IdIn(List<Long> roomIds);
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
    boolean existsByRoom_IdAndStatusNot(Long roomId, HotelBookingStatus excludeStatus);
}
