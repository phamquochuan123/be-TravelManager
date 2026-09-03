package com.example.travelManager.controller;

import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/restaurants")
@RequiredArgsConstructor
public class AdminRestaurantController {

    private final RestaurantRepository restaurantRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status) {

        List<RestaurantItem> items = restaurantRepository.findAll().stream()
                .filter(r -> search == null || search.isBlank()
                        || r.getName().toLowerCase().contains(search.toLowerCase())
                        || (r.getAddress() != null && r.getAddress().toLowerCase().contains(search.toLowerCase())))
                .filter(r -> status == null || "all".equalsIgnoreCase(status)
                        || ("ACTIVE".equalsIgnoreCase(status) && r.isActive())
                        || ("INACTIVE".equalsIgnoreCase(status) && !r.isActive()))
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
    public ResponseEntity<RestaurantItem> create(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "address", required = false, defaultValue = "") String address,
            @RequestParam(name = "city", required = false, defaultValue = "") String city,
            @RequestParam(name = "latitude", required = false) Double latitude,
            @RequestParam(name = "longitude", required = false) Double longitude,
            @RequestParam(name = "cuisineType", required = false, defaultValue = "") String cuisineType,
            @RequestParam(name = "openTime", required = false, defaultValue = "08:00") String openTime,
            @RequestParam(name = "closeTime", required = false, defaultValue = "22:00") String closeTime,
            @RequestParam(name = "maxTables", defaultValue = "20") int maxTables,
            @RequestParam(name = "description", required = false, defaultValue = "") String description,
            @RequestParam(name = "images", required = false) List<MultipartFile> images) throws Exception {

        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setAddress(address);
        restaurant.setCity(city);
        restaurant.setLatitude(latitude);
        restaurant.setLongitude(longitude);
        restaurant.setDescription(description);
        restaurant.setOpeningHours(openTime + "-" + closeTime);
        restaurant.setCapacity(maxTables);
        restaurant.setActive(true);
        if (cuisineType != null && !cuisineType.isBlank()) {
            try {
                restaurant.setCuisineType(
                    com.example.travelManager.util.constant.restaurant.CuisineType.valueOf(cuisineType.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
            restaurant.setPhoto(new SerialBlob(images.get(0).getBytes()));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toItem(restaurantRepository.save(restaurant)));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<RestaurantItem> update(
            @PathVariable("id") Long id,
            @RequestParam(name = "name") String name,
            @RequestParam(name = "address", required = false, defaultValue = "") String address,
            @RequestParam(name = "city", required = false, defaultValue = "") String city,
            @RequestParam(name = "latitude", required = false) Double latitude,
            @RequestParam(name = "longitude", required = false) Double longitude,
            @RequestParam(name = "cuisineType", required = false, defaultValue = "") String cuisineType,
            @RequestParam(name = "openTime", required = false, defaultValue = "08:00") String openTime,
            @RequestParam(name = "closeTime", required = false, defaultValue = "22:00") String closeTime,
            @RequestParam(name = "maxTables", defaultValue = "20") int maxTables,
            @RequestParam(name = "description", required = false, defaultValue = "") String description,
            @RequestParam(name = "images", required = false) List<MultipartFile> images) throws Exception {

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + id));
        restaurant.setName(name);
        restaurant.setAddress(address);
        restaurant.setCity(city);
        if (latitude != null) restaurant.setLatitude(latitude);
        if (longitude != null) restaurant.setLongitude(longitude);
        restaurant.setDescription(description);
        restaurant.setOpeningHours(openTime + "-" + closeTime);
        restaurant.setCapacity(maxTables);
        if (cuisineType != null && !cuisineType.isBlank()) {
            try {
                restaurant.setCuisineType(
                    com.example.travelManager.util.constant.restaurant.CuisineType.valueOf(cuisineType.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
            restaurant.setPhoto(new SerialBlob(images.get(0).getBytes()));
        }

        return ResponseEntity.ok(toItem(restaurantRepository.save(restaurant)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + id));
        restaurant.setActive(false);
        restaurantRepository.save(restaurant);
        return ResponseEntity.noContent().build();
    }

    private RestaurantItem toItem(Restaurant r) {
        RestaurantItem item = new RestaurantItem();
        item.setId(r.getId());
        item.setName(r.getName());
        item.setAddress(r.getAddress() != null ? r.getAddress() : "");
        item.setLatitude(r.getLatitude());
        item.setLongitude(r.getLongitude());
        item.setCuisineType(r.getCuisineType() != null ? r.getCuisineType().name() : "");
        item.setMaxTables(r.getCapacity() != null ? r.getCapacity() : 0);
        item.setDescription(r.getDescription());
        item.setStatus(r.isActive() ? "ACTIVE" : "INACTIVE");
        item.setRating(r.getAverageRating());

        // Parse openTime / closeTime from openingHours "HH:mm-HH:mm"
        String oh = r.getOpeningHours();
        if (oh != null && oh.contains("-")) {
            String[] parts = oh.split("-", 2);
            item.setOpenTime(parts[0]);
            item.setCloseTime(parts[1]);
        } else {
            item.setOpenTime("08:00");
            item.setCloseTime("22:00");
        }

        String photoUrl = blobToDataUrl(r.getPhoto());
        item.setImageUrls(photoUrl != null ? List.of(photoUrl) : List.of());

        return item;
    }

    private String blobToDataUrl(Blob blob) {
        if (blob == null) return null;
        try {
            byte[] bytes = blob.getBytes(1, (int) blob.length());
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    public static class RestaurantItem {
        private long id;
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private String cuisineType;
        private String openTime;
        private String closeTime;
        private int maxTables;
        private String description;
        private List<String> imageUrls;
        private String status;
        private Double rating;
    }
}

