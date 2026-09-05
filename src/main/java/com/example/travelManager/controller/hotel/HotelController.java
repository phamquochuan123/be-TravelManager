package com.example.travelManager.controller.hotel;

import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.hotel.Room;
import com.example.travelManager.domain.request.hotel.HotelRequest;
import com.example.travelManager.domain.request.hotel.RoomCreateRequest;
import com.example.travelManager.domain.response.hotel.HotelResponse;
import com.example.travelManager.domain.response.hotel.RoomResponse;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.service.hotel.HotelFavoriteService;
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
import java.util.Map;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final IHotelService hotelService;
    private final IRoomService roomService;
    private final HotelFavoriteService favoriteService;
    private final UserRepository userRepository;
    private final com.example.travelManager.repository.hotel.BookedRoomRepository bookedRoomRepository;

    // ── Hotel CRUD ──────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<HotelResponse> createHotel(@Valid @RequestBody HotelRequest request) {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("system");
        Hotel hotel = hotelService.createHotel(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(hotel));
    }

    @GetMapping
    public ResponseEntity<List<HotelResponse>> getAllHotels(
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "destination", required = false) String destination,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "admin", required = false, defaultValue = "false") boolean admin) {
        List<Hotel> hotels;
        if (admin) {
            hotels = hotelService.getAllHotelsAdmin();
        } else {
            hotels = hotelService.getAllHotels();
            String cityFilter = city != null ? city : destination;
            if (cityFilter != null && !cityFilter.isBlank()) {
                final String cf = cityFilter.toLowerCase();
                hotels = hotels.stream()
                        .filter(h -> h.getCity() != null && (h.getCity().toLowerCase().contains(cf) || cf.contains(h.getCity().toLowerCase())))
                        .collect(java.util.stream.Collectors.toList());
            }
            if (search != null && !search.isBlank()) {
                final String s = search.toLowerCase();
                hotels = hotels.stream()
                        .filter(h -> h.getName() != null && h.getName().toLowerCase().contains(s))
                        .collect(java.util.stream.Collectors.toList());
            }
        }
        Map<Long, Long> roomCounts = roomService.countRoomsByHotelIds(
                hotels.stream().map(Hotel::getId).toList());
        return ResponseEntity.ok(hotels.stream().map(h -> toResponse(h, roomCounts)).toList());
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

    /**
     * Danh sách phòng của khách sạn.
     *
     * Truyền thêm checkIn/checkOut thì chỉ trả các phòng CÒN TRỐNG trong khoảng đó.
     * Trước đây FE lọc bằng cờ room.isBooked, nhưng cờ boolean trên bảng rooms không
     * diễn tả được "trống hay không" — một phòng chỉ bận trong những khoảng ngày cụ thể.
     * (Thực tế cờ đó chưa bao giờ được set true nên bộ lọc luôn vô hiệu.)
     */
    @GetMapping("/{hotelId}/rooms")
    public ResponseEntity<List<RoomResponse>> getRoomsByHotel(
            @PathVariable("hotelId") Long hotelId,
            @RequestParam(name = "checkIn", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate checkIn,
            @RequestParam(name = "checkOut", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate checkOut) {

        List<Room> rooms = roomService.getRoomsByHotelId(hotelId);

        if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            rooms = rooms.stream()
                    .filter(room -> bookedRoomRepository
                            .findByRoom_IdAndStatusNotAndCheckOutDateAfterAndCheckInDateBefore(
                                    room.getId(),
                                    com.example.travelManager.util.constant.hotel.HotelBookingStatus.CANCELLED,
                                    checkIn, checkOut)
                            .isEmpty())
                    .toList();
        }

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

    @PutMapping(value = "/{hotelId}/rooms/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RoomResponse> updateRoomInHotel(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("roomId") Long roomId,
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
        Room room = roomService.updateRoomInHotel(hotelId, roomId, req, photo);
        return ResponseEntity.ok(toRoomResponse(room, hotelId));
    }

    @DeleteMapping("/{hotelId}/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoomFromHotel(
            @PathVariable("hotelId") Long hotelId,
            @PathVariable("roomId") Long roomId) {
        roomService.deleteRoomFromHotel(hotelId, roomId);
        return ResponseEntity.noContent().build();
    }

    // ── Favorites ────────────────────────────────────────────────

    @PostMapping("/{hotelId}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable("hotelId") Long hotelId) {
        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        com.example.travelManager.domain.UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Hotel hotel = hotelService.getHotelById(hotelId);
        boolean favorited = favoriteService.toggle(user, hotel);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/{hotelId}/favorite")
    public ResponseEntity<Map<String, Object>> checkFavorite(@PathVariable("hotelId") Long hotelId) {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) return ResponseEntity.ok(Map.of("favorited", false));
        com.example.travelManager.domain.UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.ok(Map.of("favorited", false));
        boolean favorited = favoriteService.isFavorited(user.getId(), hotelId);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/my-favorites")
    public ResponseEntity<List<HotelResponse>> getMyFavorites() {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) return ResponseEntity.ok(java.util.List.of());
        com.example.travelManager.domain.UserEntity user = userRepository.findByEmail(email)
                .orElse(null);
        if (user == null) return ResponseEntity.ok(java.util.List.of());
        List<Long> hotelIds = favoriteService.getFavoriteHotelIds(user.getId());
        return ResponseEntity.ok(hotelIds.stream()
                .map(id -> toResponse(hotelService.getHotelById(id))).toList());
    }

    // ── Helpers ──────────────────────────────────────────────────

    private HotelResponse toResponse(Hotel hotel) {
        return toResponse(hotel, null);
    }

    private HotelResponse toResponse(Hotel hotel, Map<Long, Long> roomCounts) {
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
        res.setActive(hotel.isActive());
        long totalRooms = roomCounts != null
                ? roomCounts.getOrDefault(hotel.getId(), 0L)
                : roomService.countRoomsByHotelId(hotel.getId());
        res.setTotalRooms((int) totalRooms);
        res.setPhoto(photoBytes);
        res.setLatitude(hotel.getLatitude());
        res.setLongitude(hotel.getLongitude());
        return res;
    }

    private RoomResponse toRoomResponse(Room room, Long hotelId) {
        String hotelName = room.getHotel() != null ? room.getHotel().getName() : null;
        return new RoomResponse(room.getId(), room.getRoomNumber(), room.getRoomType(),
                room.getRoomPrice(), room.getStatus(), room.getMaxGuests(), room.getNumBeds(),
                room.getArea(), room.getDescription(), room.isBooked(),
                room.getPhoto(), hotelId, hotelName,
                Collections.emptyList());
    }
}
