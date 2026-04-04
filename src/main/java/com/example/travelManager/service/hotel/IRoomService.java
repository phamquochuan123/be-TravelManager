package com.example.travelManager.service.hotel;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.rowset.serial.SerialException;

import org.springframework.web.multipart.MultipartFile;

import com.example.travelManager.domain.Room;
import com.example.travelManager.domain.request.hotel.RoomCreateRequest;

public interface IRoomService {

    Room addNewRoom(MultipartFile photo, String roomType, BigDecimal roomPrice)
            throws SerialException, SQLException, IOException;

    List<String> getAllRoomTypes();

    List<Room> getAllRooms();

    byte[] getRoomPhotoByRoomId(Long roomId) throws SQLException;

    void deleteRoom(Long roomId);

    Room updateRoom(Long roomId, String roomType, BigDecimal roomPrice, byte[] photoBytes);

    Optional<Room> getRoomById(Long roomId);

    List<Room> getRoomsByHotelId(Long hotelId);

    Room addRoomToHotel(Long hotelId, RoomCreateRequest request, MultipartFile photo)
            throws IOException, SQLException;

    void deleteRoomFromHotel(Long hotelId, Long roomId);

    long countRoomsByHotelId(Long hotelId);
}
