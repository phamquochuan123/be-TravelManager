package com.example.travelManager.controller.hotel;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.sql.rowset.serial.SerialException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.travelManager.domain.BookedRoom;
import com.example.travelManager.domain.Room;
import com.example.travelManager.domain.response.hotel.BookingResponse;
import com.example.travelManager.domain.response.hotel.RoomResponse;
import com.example.travelManager.exception.PhotoRetrievalException;
import com.example.travelManager.service.hotel.BookingService;
import com.example.travelManager.service.hotel.IRoomService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class RoomController {

    private final IRoomService roomService;
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<RoomResponse> addNewRoom(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("roomType") String roomType,
            @RequestParam("roomPrice") BigDecimal roomPrice) throws SerialException, SQLException, IOException {

        Room savedRoom = roomService.addNewRoom(photo, roomType, roomPrice);
        RoomResponse response = new RoomResponse(savedRoom.getId(), savedRoom.getRoomType(),
                savedRoom.getRoomPrice());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/types")
    public List<String> getRoomTyples() {
        return roomService.getAllRoomTypes();
    }

    public ResponseEntity<List<RoomResponse>> getAllRooms() throws SQLException {
        List<Room> rooms = roomService.getAllRooms();
        List<RoomResponse> responses = new ArrayList<>();
        for (Room room : rooms) {
            byte[] photoBytes = roomService.getRoomPhotoByRoomId(room.getId());
            if (photoBytes != null && photoBytes.length > 0) {
                String base64Photo = Base64.getEncoder().encodeToString(photoBytes);
                RoomResponse roomResponse = getRoomResponse(room);
                roomResponse.setPhoto(base64Photo);
                responses.add(roomResponse);
            }
        }
        return ResponseEntity.ok(responses);
    }

    private RoomResponse getRoomResponse(Room room) {
        List<BookedRoom> bookings = getAllBookingsByRoomid(room.getId());
        List<BookingResponse> bookingInfo = bookings.stream()
                .map(booking -> new BookingResponse(booking.getBookingId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate(),
                        booking.getBookingConfirmationCode()))
                .toList();
        byte[] photoBytes = null;
        Blob photoBlob = room.getPhoto();
        if (photoBlob != null) {

            try {
                photoBytes = photoBlob.getBytes(1, (int) photoBlob.length());
            } catch (SQLException e) {
                throw new PhotoRetrievalException("Error retrieving photo ");
            }
        }
        return new RoomResponse(room.getId(),
                room.getRoomType(),
                room.getRoomPrice(),
                room.isBooked(), photoBytes, bookingInfo);

    }

    private List<BookedRoom> getAllBookingsByRoomid(Long roomId) {
        return bookingService.getAllBookingsByRoomId(roomId);
    }

}
