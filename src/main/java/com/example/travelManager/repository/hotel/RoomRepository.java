package com.example.travelManager.repository.hotel;

import com.example.travelManager.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("SELECT DISTINCT r.roomType FROM Room r")
    List<String> findDistinctRoomTypes();

    @Query("SELECT r FROM Room r JOIN FETCH r.hotel WHERE r.hotel.id = :hotelId")
    List<Room> findByHotel_Id(@Param("hotelId") Long hotelId);

    long countByHotel_Id(Long hotelId);
}
