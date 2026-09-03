package com.example.travelManager.controller.tour;

import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.domain.tour.TourImage;
import com.example.travelManager.domain.tour.TourItinerary;
import com.example.travelManager.domain.request.tour.TourBookingRequest;
import com.example.travelManager.domain.request.tour.TourDepartureRequest;
import com.example.travelManager.domain.request.tour.TourItineraryRequest;
import com.example.travelManager.domain.request.tour.TourRequest;
import com.example.travelManager.domain.response.tour.TourBookingResponse;
import com.example.travelManager.domain.response.tour.TourDetailResponse;
import com.example.travelManager.domain.response.tour.TourDepartureResponse;
import com.example.travelManager.domain.response.tour.TourImageResponse;
import com.example.travelManager.domain.response.tour.TourItineraryResponse;
import com.example.travelManager.domain.response.tour.TourResponse;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.repository.tour.TourItineraryRepository;
import com.example.travelManager.repository.tour.TourReviewRepository;
import com.example.travelManager.service.tour.ITourService;
import com.example.travelManager.service.tour.TourFavoriteService;
import com.example.travelManager.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tours")
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class TourController {

    private final ITourService tourService;
    private final TourReviewRepository reviewRepository;
    private final TourDepartureRepository departureRepository;
    private final TourItineraryRepository itineraryRepository;
    private final UserRepository userRepository;
    private final com.example.travelManager.repository.tour.TourImageRepository imageRepository;
    private final TourFavoriteService favoriteService;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Tour CRUD ────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<TourResponse> createTour(@Valid @RequestBody TourRequest request) {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("system");
        Tour tour = tourService.createTour(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tour));
    }

    @GetMapping
    public ResponseEntity<List<TourResponse>> getAllTours(
            @RequestParam(value = "admin", defaultValue = "false") boolean admin,
            @RequestParam(value = "destination", required = false) String destination) {
        List<Tour> tours = admin ? tourService.getAllToursAdmin() : tourService.getAllActiveTours();
        if (destination != null && !destination.isBlank()) {
            tours = tours.stream()
                    .filter(t -> destination.equalsIgnoreCase(t.getDestination()))
                    .toList();
        }

        List<Long> tourIds = tours.stream().map(Tour::getId).toList();

        Map<Long, Double> avgRatingByTourId = reviewRepository.avgRatingGroupByTourIds(tourIds).stream()
                .collect(java.util.stream.Collectors.toMap(row -> (Long) row[0], row -> (Double) row[1]));

        Map<Long, Long> departureCountByTourId = departureRepository.countGroupedByTourIds(tourIds).stream()
                .collect(java.util.stream.Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Map<Long, String> firstImageByTourId = new java.util.HashMap<>();
        for (TourImage img : imageRepository.findByTourIdInOrderByTourIdAscSortOrderAsc(tourIds)) {
            firstImageByTourId.computeIfAbsent(img.getTour().getId(), id -> blobToBase64(img.getImageData()));
        }

        return ResponseEntity.ok(tours.stream()
                .map(t -> toListResponse(t, avgRatingByTourId, departureCountByTourId, firstImageByTourId))
                .toList());
    }

    @GetMapping("/{tourId}")
    public ResponseEntity<TourDetailResponse> getTourById(@PathVariable("tourId") Long tourId) {
        Tour tour = tourService.getTourById(tourId);
        return ResponseEntity.ok(toDetailResponse(tour));
    }

    @PutMapping("/{tourId}")
    public ResponseEntity<TourResponse> updateTour(
            @PathVariable("tourId") Long tourId,
            @Valid @RequestBody TourRequest request) {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("system");
        Tour tour = tourService.updateTour(tourId, request, currentUser);
        return ResponseEntity.ok(toResponse(tour));
    }

    @PatchMapping("/{tourId}/active")
    public ResponseEntity<TourResponse> toggleActive(@PathVariable("tourId") Long tourId) {
        Tour tour = tourService.toggleActive(tourId);
        return ResponseEntity.ok(toResponse(tour));
    }

    @DeleteMapping("/{tourId}")
    public ResponseEntity<Void> deleteTour(@PathVariable("tourId") Long tourId) {
        tourService.deleteTour(tourId);
        return ResponseEntity.noContent().build();
    }

    // ── Itinerary ────────────────────────────────────────────────

    @PostMapping("/{tourId}/itineraries")
    public ResponseEntity<TourItineraryResponse> addItinerary(
            @PathVariable("tourId") Long tourId,
            @Valid @RequestBody TourItineraryRequest request) {
        TourItinerary itinerary = tourService.addItinerary(tourId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toItineraryResponse(itinerary));
    }

    @PutMapping("/{tourId}/itineraries/{itineraryId}")
    public ResponseEntity<TourItineraryResponse> updateItinerary(
            @PathVariable("tourId") Long tourId,
            @PathVariable("itineraryId") Long itineraryId,
            @Valid @RequestBody TourItineraryRequest request) {
        TourItinerary existing = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found: " + itineraryId));
        if (!existing.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Itinerary " + itineraryId + " không thuộc tour " + tourId);
        }
        TourItinerary itinerary = tourService.updateItinerary(itineraryId, request);
        return ResponseEntity.ok(toItineraryResponse(itinerary));
    }

    @DeleteMapping("/{tourId}/itineraries/{itineraryId}")
    public ResponseEntity<Void> deleteItinerary(
            @PathVariable("tourId") Long tourId,
            @PathVariable("itineraryId") Long itineraryId) {
        TourItinerary existing = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found: " + itineraryId));
        if (!existing.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Itinerary " + itineraryId + " không thuộc tour " + tourId);
        }
        tourService.deleteItinerary(itineraryId);
        return ResponseEntity.noContent().build();
    }

    // ── Departure ────────────────────────────────────────────────

    @PostMapping("/{tourId}/departures")
    public ResponseEntity<TourDepartureResponse> addDeparture(
            @PathVariable("tourId") Long tourId,
            @Valid @RequestBody TourDepartureRequest request) {
        TourDeparture departure = tourService.addDeparture(tourId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDepartureResponse(departure));
    }

    @DeleteMapping("/{tourId}/departures/{departureId}")
    public ResponseEntity<Void> deleteDeparture(
            @PathVariable("tourId") Long tourId,
            @PathVariable("departureId") Long departureId) {
        TourDeparture existing = departureRepository.findById(departureId)
                .orElseThrow(() -> new ResourceNotFoundException("Departure not found: " + departureId));
        if (!existing.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Departure " + departureId + " không thuộc tour " + tourId);
        }
        tourService.deleteDeparture(departureId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{tourId}/departures/{departureId}/assign-staff")
    public ResponseEntity<TourDepartureResponse> assignStaff(
            @PathVariable("tourId") Long tourId,
            @PathVariable("departureId") Long departureId,
            @RequestBody java.util.Map<String, Long> body) {
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new ResourceNotFoundException("Departure not found: " + departureId));
        if (!departure.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Departure này không thuộc tour " + tourId);
        }
        Long staffId = body.get("staffId");
        if (staffId != null) {
            com.example.travelManager.domain.UserEntity staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));
            departure.setStaff(staff);
        } else {
            departure.setStaff(null);
        }
        return ResponseEntity.ok(toDepartureResponse(departureRepository.save(departure)));
    }

    // ── Image ────────────────────────────────────────────────────

    @PostMapping(value = "/{tourId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> addImage(
            @PathVariable("tourId") Long tourId,
            @RequestParam("file") MultipartFile file) throws Exception {
        tourService.addImage(tourId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{tourId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable("tourId") Long tourId,
            @PathVariable("imageId") Long imageId) {
        TourImage existing = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
        if (!existing.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Image " + imageId + " không thuộc tour " + tourId);
        }
        tourService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    // ── Booking ──────────────────────────────────────────────────

    @PostMapping("/{tourId}/book")
    public ResponseEntity<TourBookingResponse> bookTour(
            @PathVariable("tourId") Long tourId,
            @Valid @RequestBody TourBookingRequest request) {
        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        TourBookingResponse response = tourService.bookTour(tourId, request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<TourBookingResponse>> getMyBookings() {
        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        return ResponseEntity.ok(tourService.getMyBookings(email));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/bookings")
    public ResponseEntity<org.springframework.data.domain.Page<TourBookingResponse>> getAllBookings(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        // Phân trang ở DB. Cách cũ trả toàn bộ bảng booking trong một response.
        return ResponseEntity.ok(tourService.getAllBookings(page, size));
    }

    // ── Favorites ────────────────────────────────────────────────

    @PostMapping("/{tourId}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable("tourId") Long tourId) {
        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        com.example.travelManager.domain.UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Tour tour = tourService.getTourById(tourId);
        boolean favorited = favoriteService.toggle(user, tour);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/{tourId}/favorite")
    public ResponseEntity<Map<String, Object>> checkFavorite(@PathVariable("tourId") Long tourId) {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) return ResponseEntity.ok(Map.of("favorited", false));
        com.example.travelManager.domain.UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.ok(Map.of("favorited", false));
        boolean favorited = favoriteService.isFavorited(user.getId(), tourId);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/my-favorites")
    public ResponseEntity<List<TourResponse>> getMyFavorites() {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) return ResponseEntity.ok(java.util.List.of());
        com.example.travelManager.domain.UserEntity user = userRepository.findByEmail(email)
                .orElse(null);
        if (user == null) return ResponseEntity.ok(java.util.List.of());
        List<Long> tourIds = favoriteService.getFavoriteTourIds(user.getId());
        return ResponseEntity.ok(tourIds.stream().map(id -> toListResponse(tourService.getTourById(id))).toList());
    }

    // ── Helpers ──────────────────────────────────────────────────

    private TourResponse toResponse(Tour tour) {
        TourResponse res = new TourResponse();
        res.setId(tour.getId());
        res.setName(tour.getName());
        res.setDestination(tour.getDestination());
        res.setDeparture(tour.getDeparture());
        res.setTourType(tour.getTourType());
        res.setPriceAdult(tour.getPriceAdult());
        res.setPriceChild(tour.getPriceChild());
        res.setDurationDays(tour.getDurationDays());
        res.setMaxSlots(tour.getMaxSlots());
        res.setStatus(tour.getStatus());
        res.setAverageRating(reviewRepository.findAverageRatingByTourId(tour.getId()));
        res.setTotalDepartures(tour.getDepartures().size());
        List<TourImage> imgs = imageRepository.findByTourIdOrderBySortOrderAsc(tour.getId());
        if (!imgs.isEmpty()) {
            String b64 = blobToBase64(imgs.get(0).getImageData());
            if (b64 != null) res.setImages(java.util.List.of(b64));
        }
        return res;
    }

    private TourResponse toListResponse(Tour tour) {
        return toListResponse(tour, null, null, null);
    }

    /**
     * Dùng cho danh sách tour — nhận vào các map đã batch-query sẵn (rating trung bình,
     * số lượt khởi hành, ảnh đại diện) để tránh N+1 query khi map từng tour trong loop.
     */
    private TourResponse toListResponse(Tour tour, Map<Long, Double> avgRatingByTourId,
                                         Map<Long, Long> departureCountByTourId,
                                         Map<Long, String> firstImageByTourId) {
        TourResponse res = new TourResponse();
        res.setId(tour.getId());
        res.setName(tour.getName());
        res.setDestination(tour.getDestination());
        res.setDeparture(tour.getDeparture());
        res.setTourType(tour.getTourType());
        res.setPriceAdult(tour.getPriceAdult());
        res.setPriceChild(tour.getPriceChild());
        res.setDurationDays(tour.getDurationDays());
        res.setMaxSlots(tour.getMaxSlots());
        res.setStatus(tour.getStatus());

        res.setTotalDepartures(departureCountByTourId != null
                ? departureCountByTourId.getOrDefault(tour.getId(), 0L).intValue()
                : tour.getDepartures().size());
        res.setAverageRating(avgRatingByTourId != null
                ? avgRatingByTourId.get(tour.getId())
                : reviewRepository.findAverageRatingByTourId(tour.getId()));

        if (firstImageByTourId != null) {
            String b64 = firstImageByTourId.get(tour.getId());
            if (b64 != null) res.setImages(java.util.List.of(b64));
        } else {
            List<TourImage> imgs = imageRepository.findByTourIdOrderBySortOrderAsc(tour.getId());
            if (!imgs.isEmpty()) {
                String b64 = blobToBase64(imgs.get(0).getImageData());
                if (b64 != null) res.setImages(java.util.List.of(b64));
            }
        }
        return res;
    }

    private String blobToBase64(java.sql.Blob blob) {
        if (blob == null) return null;
        try {
            byte[] bytes = blob.getBytes(1, (int) blob.length());
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private TourDetailResponse toDetailResponse(Tour tour) {
        TourDetailResponse res = new TourDetailResponse();
        res.setId(tour.getId());
        res.setName(tour.getName());
        res.setDescription(tour.getDescription());
        res.setDestination(tour.getDestination());
        res.setDeparture(tour.getDeparture());
        res.setTourType(tour.getTourType());
        res.setPriceAdult(tour.getPriceAdult());
        res.setPriceChild(tour.getPriceChild());
        res.setDurationDays(tour.getDurationDays());
        res.setDurationNights(tour.getDurationNights());
        res.setMaxSlots(tour.getMaxSlots());
        res.setPackageDiscountPercent(tour.getPackageDiscountPercent());
        res.setStatus(tour.getStatus());
        res.setCancellationPolicy(tour.getCancellationPolicy());
        res.setIncludedServices(tour.getIncludedServices());
        res.setAverageRating(reviewRepository.findAverageRatingByTourId(tour.getId()));
        res.setImages(tour.getImages().stream().map(this::toImageResponse).toList());
        res.setItineraries(tour.getItineraries().stream().map(this::toItineraryResponse).toList());
        res.setDepartures(tour.getDepartures().stream().map(this::toDepartureResponse).toList());
        try {
            if (tour.getLinkedHotels() != null && !tour.getLinkedHotels().isBlank()
                    && !tour.getLinkedHotels().equals("[]")) {
                res.setLinkedHotels(MAPPER.readValue(tour.getLinkedHotels(), new TypeReference<List<Integer>>() {}));
            }
        } catch (Exception ignored) {}
        try {
            if (tour.getLinkedRestaurants() != null && !tour.getLinkedRestaurants().isBlank()
                    && !tour.getLinkedRestaurants().equals("[]")) {
                res.setLinkedRestaurants(MAPPER.readValue(tour.getLinkedRestaurants(), new TypeReference<List<Integer>>() {}));
            }
        } catch (Exception ignored) {}
        try {
            if (tour.getLinkedDestinations() != null && !tour.getLinkedDestinations().isBlank()
                    && !tour.getLinkedDestinations().equals("[]")) {
                res.setLinkedDestinations(MAPPER.readValue(tour.getLinkedDestinations(), new TypeReference<List<Integer>>() {}));
            }
        } catch (Exception ignored) {}
        return res;
    }

    private TourImageResponse toImageResponse(TourImage image) {
        byte[] bytes = null;
        if (image.getImageData() != null) {
            try { bytes = image.getImageData().getBytes(1, (int) image.getImageData().length()); }
            catch (SQLException ignored) {}
        }
        TourImageResponse res = new TourImageResponse();
        res.setId(image.getId());
        res.setPhoto(bytes);
        res.setSortOrder(image.getSortOrder());
        return res;
    }

    private TourItineraryResponse toItineraryResponse(TourItinerary itinerary) {
        TourItineraryResponse res = new TourItineraryResponse();
        res.setId(itinerary.getId());
        res.setDayNumber(itinerary.getDayNumber());
        res.setTitle(itinerary.getTitle());
        res.setDescription(itinerary.getDescription());
        res.setActivities(itinerary.getActivities());
        return res;
    }

    private TourDepartureResponse toDepartureResponse(TourDeparture departure) {
        TourDepartureResponse res = new TourDepartureResponse();
        res.setId(departure.getId());
        res.setDepartureDate(departure.getDepartureDate());
        res.setAvailableSlots(departure.getAvailableSlots());
        if (departure.getStaff() != null) {
            res.setStaffId(departure.getStaff().getId());
            res.setStaffName(departure.getStaff().getName());
        }
        return res;
    }
}

