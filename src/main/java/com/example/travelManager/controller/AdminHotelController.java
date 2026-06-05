package com.example.travelManager.controller;

import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.hotel.Room;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.hotel.RoomRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.math.BigDecimal;
import java.sql.Blob;
import java.util.*;
import java.util.stream.Collectors;

import javax.sql.rowset.serial.SerialBlob;

@RestController
@RequestMapping("/admin/hotels")
@RequiredArgsConstructor
public class AdminHotelController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status) {

        List<HotelItem> items = hotelRepository.findAll().stream()
                .filter(h -> search == null || search.isBlank()
                        || h.getName().toLowerCase().contains(search.toLowerCase())
                        || (h.getAddress() != null && h.getAddress().toLowerCase().contains(search.toLowerCase())))
                .filter(h -> status == null || "all".equalsIgnoreCase(status)
                        || ("ACTIVE".equalsIgnoreCase(status) && h.isActive())
                        || ("INACTIVE".equalsIgnoreCase(status) && !h.isActive()))
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(this::toItem)
                .collect(Collectors.toList());

        int total = items.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);

        Map<String, Object> result = new HashMap<>();
        result.put("content", items.subList(fromIdx, toIdx));
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("totalElements", total);
        return ResponseEntity.ok(result);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<HotelItem> create(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "address", required = false, defaultValue = "") String address,
            @RequestParam(name = "city", required = false, defaultValue = "") String city,
            @RequestParam(name = "stars", defaultValue = "3") int stars,
            @RequestParam(name = "description", required = false, defaultValue = "") String description,
            @RequestParam(name = "amenities", required = false, defaultValue = "[]") String amenities,
            @RequestParam(name = "roomTypes", required = false, defaultValue = "[]") String roomTypes,
            @RequestParam(name = "images", required = false) List<MultipartFile> images,
            MultipartHttpServletRequest multipartRequest) throws Exception {

        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setCity(city.isBlank() ? null : city);
        hotel.setStarRating(stars);
        hotel.setDescription(description);
        hotel.setAmenities(amenities);
        hotel.setActive(true);

        if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
            hotel.setPhoto(new SerialBlob(images.get(0).getBytes()));
        }

        hotel = hotelRepository.save(hotel);
        saveRooms(hotel, roomTypes, multipartRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(toItem(hotel));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<HotelItem> update(
            @PathVariable("id") Long id,
            @RequestParam(name = "name") String name,
            @RequestParam(name = "address", required = false, defaultValue = "") String address,
            @RequestParam(name = "city", required = false, defaultValue = "") String city,
            @RequestParam(name = "stars", defaultValue = "3") int stars,
            @RequestParam(name = "description", required = false, defaultValue = "") String description,
            @RequestParam(name = "amenities", required = false, defaultValue = "[]") String amenities,
            @RequestParam(name = "roomTypes", required = false, defaultValue = "[]") String roomTypes,
            @RequestParam(name = "images", required = false) List<MultipartFile> images,
            MultipartHttpServletRequest multipartRequest) throws Exception {

        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + id));
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setCity(city.isBlank() ? null : city);
        hotel.setStarRating(stars);
        hotel.setDescription(description);
        hotel.setAmenities(amenities);

        if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
            hotel.setPhoto(new SerialBlob(images.get(0).getBytes()));
        }

        hotel = hotelRepository.save(hotel);
        saveRooms(hotel, roomTypes, multipartRequest);
        return ResponseEntity.ok(toItem(hotel));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + id));
        hotel.setActive(false);
        hotelRepository.save(hotel);
        return ResponseEntity.noContent().build();
    }

    private void saveRooms(Hotel hotel, String roomTypesJson, MultipartHttpServletRequest req) throws Exception {
        List<Map<String, Object>> rooms = MAPPER.readValue(roomTypesJson, new TypeReference<>() {
        });
        for (int i = 0; i < rooms.size(); i++) {
            Map<String, Object> r = rooms.get(i);
            Object idVal = r.get("id");
            Room room;
            if (idVal != null) {
                Long roomId = Long.valueOf(idVal.toString());
                room = roomRepository.findById(roomId).orElse(new Room());
            } else {
                room = new Room();
            }
            room.setHotel(hotel);
            room.setRoomType(getString(r, "name"));
            room.setArea(getDouble(r, "area"));
            room.setMaxGuests(getInt(r, "capacity"));
            room.setRoomPrice(getBigDecimal(r, "pricePerNight"));

            // Per-room image: roomImage_{i}
            if (req != null) {
                MultipartFile roomImg = req.getFile("roomImage_" + i);
                if (roomImg != null && !roomImg.isEmpty()) {
                    room.setPhoto(roomImg.getBytes());
                }
            }
            roomRepository.save(room);
        }
    }

    private HotelItem toItem(Hotel hotel) {
        HotelItem item = new HotelItem();
        item.setId(hotel.getId());
        item.setName(hotel.getName());
        item.setAddress(hotel.getAddress() != null ? hotel.getAddress() : "");
        item.setStars(hotel.getStarRating());
        item.setDescription(hotel.getDescription());
        item.setStatus(hotel.isActive() ? "ACTIVE" : "INACTIVE");
        item.setAmenities(parseStringList(hotel.getAmenities()));

        String photoUrl = blobToDataUrl(hotel.getPhoto());
        item.setImageUrls(photoUrl != null ? List.of(photoUrl) : List.of());

        List<Room> rooms = roomRepository.findByHotel_Id(hotel.getId());
        item.setRoomTypeCount(rooms.size());
        item.setRoomTypes(rooms.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getRoomType() != null ? r.getRoomType() : "");
            m.put("area", r.getArea() != null ? r.getArea() : 0);
            m.put("capacity", r.getMaxGuests());
            m.put("pricePerNight", r.getRoomPrice() != null ? r.getRoomPrice().doubleValue() : 0);
            m.put("imageUrl", bytesToDataUrl(r.getPhoto()));
            return m;
        }).collect(Collectors.toList()));

        return item;
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String blobToDataUrl(Blob blob) {
        if (blob == null)
            return null;
        try {
            byte[] bytes = blob.getBytes(1, (int) blob.length());
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private String bytesToDataUrl(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return null;
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private int getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null)
            return 0;
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private double getDouble(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null)
            return 0;
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal getBigDecimal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null)
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(v.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Data
    public static class HotelItem {
        private long id;
        private String name;
        private String address;
        private int stars;
        private String description;
        private List<String> amenities;
        private List<String> imageUrls;
        private List<Map<String, Object>> roomTypes;
        private int roomTypeCount;
        private String status;
    }
}
