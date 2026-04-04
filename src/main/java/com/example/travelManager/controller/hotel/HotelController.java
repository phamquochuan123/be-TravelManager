package com.example.travelManager.controller.hotel;

import com.example.travelManager.domain.Hotel;
import com.example.travelManager.domain.Room;
import com.example.travelManager.domain.request.hotel.HotelRequest;
import com.example.travelManager.domain.request.hotel.RoomCreateRequest;
import com.example.travelManager.domain.response.hotel.HotelResponse;
import com.example.travelManager.domain.response.hotel.RoomResponse;
import com.example.travelManager.service.hotel.IHotelService;
import com.example.travelManager.service.hotel.IRoomService;
import com.example.travelManager.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final IHotelService hotelService;
    private final IRoomService roomService;

    // ── Hotel CRUD ──────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<HotelResponse> createHotel(@Valid @RequestBody HotelRequest request) {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("system");
        Hotel hotel = hotelService.createHotel(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(hotel));
    }

    @GetMapping
    public ResponseEntity<List<HotelResponse>> getAllHotels(
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "admin", required = false, defaultValue = "false") boolean admin) {
        List<Hotel> hotels;
        if (admin) {
            hotels = hotelService.getAllHotelsAdmin();
        } else if (city != null) {
            hotels = hotelService.getHotelsByCity(city);
        } else {
            hotels = hotelService.getAllHotels();
        }
        return ResponseEntity.ok(hotels.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelResponse> getHotelById(@PathVariable("hotelId") Long hotelId) {
        return ResponseEntity.ok(toResponse(hotelService.getHotelById(hotelId)));
    }

    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelResponse> updateHotel(
            @PathVariable("hotelId") Long hotelId,
            @Valid @RequestBody HotelRequest request) {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("system");
        Hotel hotel = hotelService.updateHotel(hotelId, request, currentUser);
        return ResponseEntity.ok(toResponse(hotel));
    }

    @PatchMapping("/{hotelId}/active")
    public ResponseEntity<HotelResponse> toggleActive(@PathVariable("hotelId") Long hotelId) {
        Hotel hotel = hotelService.toggleActive(hotelId);
        return ResponseEntity.ok(toResponse(hotel));
    }

    @PatchMapping(value = "/{hotelId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HotelResponse> uploadHotelPhoto(
            @PathVariable("hotelId") Long hotelId,
            @RequestParam("photo") MultipartFile photo) throws IOException, SQLException {
        Hotel hotel = hotelService.uploadPhoto(hotelId, photo);
        return ResponseEntity.ok(toResponse(hotel));
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotel(@PathVariable("hotelId") Long hotelId) {
        hotelService.deleteHotel(hotelId);
        return ResponseEntity.noContent().build();
    }

    // ── Room management per hotel ────────────────────────────────

    @GetMapping("/{hotelId}/rooms")
    public ResponseEntity<List<RoomResponse>> getRoomsByHotel(
            @PathVariable("hotelId") Long hotelId) {
        List<Room> rooms = roomService.getRoomsByHotelId(hotelId);
        return ResponseEntity.ok(rooms.stream().map(room -> toRoomResponse(room, hotelId)).toList());
    }

    @PostMapping(value = "/{hotelId}/rooms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RoomResponse> addRoomToHotel(
            @PathVariable("hotelId") Long hotelId,
            @RequestParam("roomType") String roomType,
            @RequestParam("roomPrice") BigDecimal roomPrice,
            @RequestParam(value = "roomNumber", required = false) String roomNumber,
            @RequestParam(value = "maxGuests", defaultValue = "2") int maxGuests,
            @RequestParam(value = "numBeds", defaultValue = "1") int numBeds,
            @RequestParam(value = "area", required = false) Double area,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "photo", required = false) MultipartFile photo)
            throws IOException, SQLException {
        RoomCreateRequest req = new RoomCreateRequest();
        req.setRoomType(roomType);
        req.setRoomPrice(roomPrice);
        req.setRoomNumber(roomNumber);
        req.setMaxGuests(maxGuests);
        req.setNumBeds(numBeds);
        req.setArea(area);
        req.setDescription(description);
        Room room = roomService.addRoomToHotel(hotelId, req, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(toRoomResponse(room, hotelId));
    }

    @DeleteMapping("/{hotelId}/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoomFromHotel(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("roomId") Long roomId) {
        roomService.deleteRoomFromHotel(hotelId, roomId);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private HotelResponse toResponse(Hotel hotel) {
        byte[] photoBytes = null;
        if (hotel.getPhoto() != null) {
            try { photoBytes = hotel.getPhoto().getBytes(1, (int) hotel.getPhoto().length()); }
            catch (SQLException ignored) {}
        }
        HotelResponse res = new HotelResponse();
        res.setId(hotel.getId());
        res.setName(hotel.getName());
        res.setDescription(hotel.getDescription());
        res.setAddress(hotel.getAddress());
        res.setCity(hotel.getCity());
        res.setStarRating(hotel.getStarRating());
        res.setHotelType(hotel.getHotelType());
        res.setAmenities(hotel.getAmenities());
        res.setLatitude(hotel.getLatitude());
        res.setLongitude(hotel.getLongitude());
        res.setActive(hotel.isActive());
        res.setTotalRooms((int) roomService.countRoomsByHotelId(hotel.getId()));
        res.setPhoto(photoBytes);
        return res;
    }

    private RoomResponse toRoomResponse(Room room, Long hotelId) {
        byte[] photoBytes = null;
        if (room.getPhoto() != null) {
            try { photoBytes = room.getPhoto().getBytes(1, (int) room.getPhoto().length()); }
            catch (SQLException ignored) {}
        }
        String hotelName = room.getHotel() != null ? room.getHotel().getName() : null;
        return new RoomResponse(room.getId(), room.getRoomNumber(), room.getRoomType(),
                room.getRoomPrice(), room.getStatus(), room.getMaxGuests(), room.getNumBeds(),
                room.getArea(), room.getDescription(), room.isBooked(),
                photoBytes, hotelId, hotelName,
                Collections.emptyList());
    }
}
