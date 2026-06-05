package com.example.travelManager.controller;

import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourImage;
import com.example.travelManager.domain.tour.TourItinerary;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.repository.tour.TourImageRepository;
import com.example.travelManager.repository.tour.TourItineraryRepository;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.util.constant.tour.BookingStatus;
import com.example.travelManager.util.constant.tour.TourStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.math.BigDecimal;
import java.sql.Blob;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/tours")
@RequiredArgsConstructor
public class AdminTourController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TourRepository tourRepository;
    private final TourImageRepository imageRepository;
    private final TourItineraryRepository itineraryRepository;
    private final TourDepartureRepository departureRepository;
    private final TourBookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RestaurantRepository restaurantRepository;

    @GetMapping("/names")
    public ResponseEntity<List<Map<String, Object>>> names() {
        List<Map<String, Object>> result = tourRepository.findAllByDeletedFalse().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(t -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "destination", required = false) String destination,
            @RequestParam(name = "status", required = false) String status) {

        List<Tour> all = tourRepository.findAllByDeletedFalse().stream()
                .filter(t -> destination == null || destination.isBlank()
                        || destination.equalsIgnoreCase(t.getDestination()))
                .filter(t -> search == null || search.isBlank()
                        || t.getName().toLowerCase().contains(search.toLowerCase())
                        || (t.getDestination() != null && t.getDestination().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());

        // Compute display statuses and filter
        List<TourItem> items = all.stream()
                .map(this::toItem)
                .filter(item -> status == null || "all".equalsIgnoreCase(status) || status.equalsIgnoreCase(item.getStatus()))
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
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
    public ResponseEntity<TourItem> create(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "destination", required = false, defaultValue = "") String destination,
            @RequestParam(name = "durationDays", defaultValue = "1") int durationDays,
            @RequestParam(name = "durationNights", defaultValue = "0") int durationNights,
            @RequestParam(name = "priceAdult", defaultValue = "0") long priceAdult,
            @RequestParam(name = "priceChild", defaultValue = "0") long priceChild,
            @RequestParam(name = "description", required = false, defaultValue = "") String description,
            @RequestParam(name = "itinerary", required = false, defaultValue = "[]") String itinerary,
            @RequestParam(name = "destinations", required = false, defaultValue = "[]") String destinations,
            @RequestParam(name = "images", required = false) List<MultipartFile> images) throws Exception {

        Tour tour = new Tour();
        tour.setName(name);
        tour.setDestination(destination);
        tour.setDurationDays(durationDays);
        tour.setDurationNights(durationNights);
        tour.setPriceAdult(BigDecimal.valueOf(priceAdult));
        tour.setPriceChild(BigDecimal.valueOf(priceChild));
        tour.setDescription(description);
        tour.setLinkedHotels(buildLinkedJson(autoLinkHotels(destination)));
        tour.setLinkedRestaurants(buildLinkedJson(autoLinkRestaurants(destination)));
        tour.setLinkedDestinations(destinations);
        tour.setStatus(TourStatus.ACTIVE);
        tour = tourRepository.save(tour);

        saveItineraries(tour, itinerary);
        if (images != null) saveImages(tour, images);

        return ResponseEntity.status(HttpStatus.CREATED).body(toItem(tour));
    }

    @lombok.Data
    public static class TourUpdateRequest {
        private String name;
        private String destination;
        private int durationDays;
        private int durationNights;
        private long priceAdult;
        private long priceChild;
        private String description;
        private String itinerary = "[]";
        private String destinations = "[]";
    }

    @PutMapping("/{id}")
    public ResponseEntity<TourItem> update(
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestBody TourUpdateRequest req) throws Exception {

        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + id));
        String destination = req.getDestination() != null ? req.getDestination() : "";
        tour.setName(req.getName());
        tour.setDestination(destination);
        tour.setDurationDays(req.getDurationDays());
        tour.setDurationNights(req.getDurationNights());
        tour.setPriceAdult(BigDecimal.valueOf(req.getPriceAdult()));
        tour.setPriceChild(BigDecimal.valueOf(req.getPriceChild()));
        tour.setDescription(req.getDescription() != null ? req.getDescription() : "");
        tour.setLinkedHotels(buildLinkedJson(autoLinkHotels(destination)));
        tour.setLinkedRestaurants(buildLinkedJson(autoLinkRestaurants(destination)));
        tour.setLinkedDestinations(req.getDestinations() != null ? req.getDestinations() : "[]");
        tour = tourRepository.save(tour);

        itineraryRepository.deleteAll(itineraryRepository.findByTourIdOrderByDayNumberAsc(id));
        saveItineraries(tour, req.getItinerary() != null ? req.getItinerary() : "[]");

        return ResponseEntity.ok(toItem(tour));
    }

    @PostMapping(value = "/{id}/images/replace", consumes = "multipart/form-data")
    public ResponseEntity<TourItem> replaceImages(
            @PathVariable("id") Long id,
            @RequestParam(name = "images", required = false) List<MultipartFile> images) throws Exception {

        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + id));

        if (images != null && images.stream().anyMatch(f -> f != null && !f.isEmpty())) {
            imageRepository.deleteAll(imageRepository.findByTourIdOrderBySortOrderAsc(id));
            saveImages(tour, images);
        }

        return ResponseEntity.ok(toItem(tour));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + id));
        boolean hasActive = bookingRepository.existsByTourIdAndStatusIn(id,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        if (hasActive) {
            throw new IllegalStateException("Không thể xóa tour đang có đặt chỗ hoạt động");
        }
        tour.setDeleted(true);
        tour.setStatus(TourStatus.DELETED);
        tourRepository.save(tour);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.getOrDefault("ids", List.of());
        for (Long id : ids) {
            tourRepository.findById(id).ifPresent(tour -> {
                boolean hasActive = bookingRepository.existsByTourIdAndStatusIn(id,
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
                if (!hasActive) {
                    tour.setDeleted(true);
                    tour.setStatus(TourStatus.DELETED);
                    tourRepository.save(tour);
                }
            });
        }
        return ResponseEntity.noContent().build();
    }

    private TourItem toItem(Tour tour) {
        TourItem item = new TourItem();
        item.setId(tour.getId());
        item.setName(tour.getName());
        item.setDestination(tour.getDestination() != null ? tour.getDestination() : "");
        item.setDurationDays(tour.getDurationDays());
        item.setDurationNights(tour.getDurationNights());
        item.setPriceAdult(tour.getPriceAdult() != null ? tour.getPriceAdult().doubleValue() : 0);
        item.setPriceChild(tour.getPriceChild() != null ? tour.getPriceChild().doubleValue() : 0);
        item.setDescription(tour.getDescription());
        item.setDepartureCount(departureRepository.findByTourIdOrderByDepartureDateAsc(tour.getId()).size());
        item.setStatus(computeStatus(tour));

        // Images — include id so frontend can delete individually
        List<TourImage> imgs = imageRepository.findByTourIdOrderBySortOrderAsc(tour.getId());
        List<String> imageUrls = new ArrayList<>();
        List<Map<String, Object>> imageList = new ArrayList<>();
        for (TourImage img : imgs) {
            String url = blobToDataUrl(img.getImageData());
            if (url != null) {
                imageUrls.add(url);
                Map<String, Object> m = new HashMap<>();
                m.put("id", img.getId());
                m.put("url", url);
                imageList.add(m);
            }
        }
        item.setImageUrls(imageUrls);
        item.setImageList(imageList);
        if (!imageUrls.isEmpty()) item.setImageUrl(imageUrls.get(0));

        // Itinerary
        List<TourItinerary> its = itineraryRepository.findByTourIdOrderByDayNumberAsc(tour.getId());
        item.setItinerary(its.stream().map(it -> {
            Map<String, String> m = new HashMap<>();
            m.put("title", it.getTitle() != null ? it.getTitle() : "");
            m.put("description", it.getDescription() != null ? it.getDescription() : "");
            return m;
        }).collect(Collectors.toList()));

        // Linked hotels/restaurants
        item.setHotels(parseIds(tour.getLinkedHotels()));
        item.setRestaurants(parseIds(tour.getLinkedRestaurants()));
        item.setDestinations(parseIds(tour.getLinkedDestinations()));

        return item;
    }

    private String computeStatus(Tour tour) {
        if (tour.getStatus() == TourStatus.INACTIVE) return "INACTIVE";
        LocalDate today = LocalDate.now();
        List<com.example.travelManager.domain.tour.TourDeparture> futureDeps = tour.getDepartures().stream()
                .filter(d -> d.getDepartureDate() != null && !d.getDepartureDate().isBefore(today))
                .collect(Collectors.toList());
        if (!futureDeps.isEmpty() && futureDeps.stream().allMatch(d -> d.getAvailableSlots() <= 0)) {
            return "FULL";
        }
        return "ACTIVE";
    }

    private void saveItineraries(Tour tour, String itineraryJson) throws Exception {
        List<Map<String, String>> days = MAPPER.readValue(itineraryJson,
                new TypeReference<>() {});
        for (int i = 0; i < days.size(); i++) {
            Map<String, String> day = days.get(i);
            TourItinerary it = new TourItinerary();
            it.setTour(tour);
            it.setDayNumber(i + 1);
            it.setTitle(day.getOrDefault("title", ""));
            it.setDescription(day.getOrDefault("description", ""));
            itineraryRepository.save(it);
        }
    }

    private void saveImages(Tour tour, List<MultipartFile> images) throws Exception {
        for (int i = 0; i < images.size(); i++) {
            MultipartFile f = images.get(i);
            if (f == null || f.isEmpty()) continue;
            TourImage img = new TourImage();
            img.setTour(tour);
            img.setImageData(new SerialBlob(f.getBytes()));
            img.setSortOrder(i);
            imageRepository.save(img);
        }
    }

    // ── Auto-link helpers ────────────────────────────────────────

    private String extractPrimaryCity(String destination) {
        if (destination == null || destination.isBlank()) return "";
        return destination.split("[–\\-/,]")[0].trim().toLowerCase();
    }

    private List<Integer> autoLinkHotels(String destination) {
        String city = extractPrimaryCity(destination);
        if (city.isEmpty()) return List.of();
        return hotelRepository.findAll().stream()
                .filter(h -> h.isActive()
                        && h.getCity() != null
                        && (h.getCity().toLowerCase().contains(city) || city.contains(h.getCity().toLowerCase())))
                .map(h -> h.getId().intValue())
                .collect(Collectors.toList());
    }

    private List<Integer> autoLinkRestaurants(String destination) {
        String city = extractPrimaryCity(destination);
        if (city.isEmpty()) return List.of();
        return restaurantRepository.findAll().stream()
                .filter(r -> r.isActive()
                        && r.getCity() != null
                        && (r.getCity().toLowerCase().contains(city) || city.contains(r.getCity().toLowerCase())))
                .map(r -> r.getId().intValue())
                .collect(Collectors.toList());
    }

    private String buildLinkedJson(List<Integer> ids) {
        try { return MAPPER.writeValueAsString(ids); }
        catch (Exception e) { return "[]"; }
    }

    private List<Integer> parseIds(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
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

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable("id") Long id,
            @PathVariable("imageId") Long imageId) {
        imageRepository.findById(imageId).ifPresent(img -> {
            if (img.getTour().getId().equals(id)) {
                imageRepository.delete(img);
            }
        });
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class TourItem {
        private long id;
        private String name;
        private String destination;
        private int durationDays;
        private int durationNights;
        private double priceAdult;
        private double priceChild;
        private int departureCount;
        private String status;
        private String imageUrl;
        private List<String> imageUrls;
        private List<Map<String, Object>> imageList;
        private String description;
        private List<Integer> hotels;
        private List<Integer> restaurants;
        private List<Integer> destinations;
        private List<Map<String, String>> itinerary;
    }
}

