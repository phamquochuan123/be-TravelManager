package com.example.travelManager.service.hotel;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import javax.sql.rowset.serial.SerialException;

import org.springframework.web.multipart.MultipartFile;

import com.example.travelManager.domain.Room;

public interface IRoomService {

    Room addNewRoom(MultipartFile photo, String roomType, BigDecimal roomPrice)
            throws SerialException, SQLException, IOException;

    List<String> getAllRoomTypes();

    List<Room> getAllRooms();

    byte[] getRoomPhotoByRoomId(Long roomId) throws SQLException;

}
