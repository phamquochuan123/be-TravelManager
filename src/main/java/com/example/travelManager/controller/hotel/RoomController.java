package com.example.travelManager.controller.hotel;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.hotel.Room;
import com.example.travelManager.domain.response.hotel.BookingResponse;
import com.example.travelManager.domain.response.hotel.RoomResponse;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.service.hotel.BookingService;
import com.example.travelManager.service.hotel.IRoomService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RoomController {

    private final IRoomService roomService;
    private final BookingService bookingService;

    @PostMapping("/rooms")
    public ResponseEntity<RoomResponse> addNewRoom(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("roomType") String roomType,
            @RequestParam("roomPrice") BigDecimal roomPrice) throws IOException, SQLException {

        roomType = com.example.travelManager.util.InputValidator.ten(roomType, "Loại phòng");
        com.example.travelManager.util.InputValidator.duong(roomPrice, "Giá phòng");

        Room savedRoom = roomService.addNewRoom(photo, roomType, roomPrice);
        RoomResponse response = new RoomResponse(savedRoom.getId(), savedRoom.getRoomType(),
                savedRoom.getRoomPrice());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/types")
    public List<String> getRoomTypes() {
        return roomService.getAllRoomTypes();
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomResponse>> getAllRooms() throws SQLException {
        List<Room> rooms = roomService.getAllRooms();
        Map<Long, List<BookedRoom>> bookingsByRoomId = bookingService
                .getAllBookingsByRoomIds(rooms.stream().map(Room::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(b -> b.getRoom().getId()));
        List<RoomResponse> responses = new ArrayList<>();
        for (Room room : rooms) {
            responses.add(getRoomResponse(room, bookingsByRoomId.getOrDefault(room.getId(), List.of())));
        }
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable("roomId") Long roomId) {
        roomService.deleteRoom(roomId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable("roomId") Long roomId,
            @RequestParam(value = "roomType", required = false) String roomType,
            @RequestParam(value = "roomPrice", required = false) BigDecimal roomPrice,
            @RequestParam(value = "photo", required = false) MultipartFile photo) throws IOException, SQLException {
        // Cả hai đều không bắt buộc khi sửa — chỉ kiểm cái nào thực sự được gửi lên.
        if (roomType != null) {
            roomType = com.example.travelManager.util.InputValidator.ten(roomType, "Loại phòng");
        }
        if (roomPrice != null) {
            com.example.travelManager.util.InputValidator.duong(roomPrice, "Giá phòng");
        }

        byte[] photoBytes = photo != null && !photo.isEmpty() ? photo.getBytes()
                : roomService.getRoomPhotoByRoomId(roomId);
        Room theRoom = roomService.updateRoom(roomId, roomType, roomPrice, photoBytes);
        theRoom.setPhoto(photoBytes);
        return ResponseEntity.ok(getRoomResponse(theRoom));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable("roomId") Long roomId) {
        Room theRoom = roomService.getRoomById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        return ResponseEntity.ok(getRoomResponse(theRoom));
    }

    private RoomResponse getRoomResponse(Room room) {
        return getRoomResponse(room, bookingService.getAllBookingsByRoomId(room.getId()));
    }

    private RoomResponse getRoomResponse(Room room, List<BookedRoom> bookings) {
        // GET /rooms và GET /rooms/{id} là endpoint PUBLIC (SecurityConfig permitAll).
        // Chỉ trả khoảng ngày đã kín để FE vẽ lịch trống — KHÔNG trả bookingConfirmationCode,
        // vì mã đó tra được /bookings/confirmation/{code} (cũng public) để lấy tên + email khách.
        List<BookingResponse> bookingInfo = bookings.stream()
                .map(booking -> new BookingResponse(booking.getBookingId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate(),
                        null))
                .toList();
        Long hotelId = room.getHotel() != null ? room.getHotel().getId() : null;
        String hotelName = room.getHotel() != null ? room.getHotel().getName() : null;
        return new RoomResponse(room.getId(), room.getRoomNumber(), room.getRoomType(),
                room.getRoomPrice(), room.getStatus(), room.getMaxGuests(), room.getNumBeds(),
                room.getArea(), room.getDescription(), room.isBooked(),
                room.getPhoto(), hotelId, hotelName, bookingInfo);
    }
}
