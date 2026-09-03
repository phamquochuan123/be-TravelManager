package com.example.travelManager.repository.hotel;

import com.example.travelManager.domain.hotel.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT DISTINCT r.roomType FROM Room r")
    List<String> findDistinctRoomTypes();

    @Query("SELECT r FROM Room r JOIN FETCH r.hotel WHERE r.hotel.id = :hotelId")
    List<Room> findByHotel_Id(@Param("hotelId") Long hotelId);

    long countByHotel_Id(Long hotelId);

    @Query("SELECT r.hotel.id, COUNT(r) FROM Room r WHERE r.hotel.id IN :hotelIds GROUP BY r.hotel.id")
    List<Object[]> countGroupedByHotelIds(@Param("hotelIds") List<Long> hotelIds);
}
