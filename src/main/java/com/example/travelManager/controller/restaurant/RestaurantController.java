package com.example.travelManager.controller.restaurant;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.request.restaurant.RestaurantBookingRequest;
import com.example.travelManager.domain.request.restaurant.RestaurantRequest;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.restaurant.RestaurantFavorite;
import com.example.travelManager.domain.response.restaurant.RestaurantBookingResponse;
import com.example.travelManager.domain.response.restaurant.RestaurantResponse;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.restaurant.RestaurantFavoriteRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.service.EmailService;
import com.example.travelManager.service.restaurant.IRestaurantService;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final IRestaurantService restaurantService;
    private final RestaurantBookingRepository bookingRepository;
    private final RestaurantFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TourBookingRepository tourBookingRepository;

    // ── Restaurant CRUD ──────────────────────────────────────────

    @PostMapping
    public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody RestaurantRequest request) {
        String user = SecurityUtil.getCurrentUserLogin().orElse("system");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(restaurantService.createRestaurant(request, user)));
    }

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAll(
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "destination", required = false) String destination,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "admin", defaultValue = "false") boolean admin) {
        List<Restaurant> list;
        if (admin) {
            list = restaurantService.getAllAdmin();
        } else {
            list = restaurantService.getAllActive();
            String cityFilter = city != null ? city : destination;
            if (cityFilter != null && !cityFilter.isBlank()) {
                final String cf = cityFilter.toLowerCase();
                list = list.stream()
                        .filter(r -> r.getCity() != null && (r.getCity().toLowerCase().contains(cf) || cf.contains(r.getCity().toLowerCase())))
                        .collect(java.util.stream.Collectors.toList());
            }
            if (search != null && !search.isBlank()) {
                final String s = search.toLowerCase();
                list = list.stream()
                        .filter(r -> r.getName() != null && r.getName().toLowerCase().contains(s))
                        .collect(java.util.stream.Collectors.toList());
            }
        }
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(toResponse(restaurantService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody RestaurantRequest request) {
        String user = SecurityUtil.getCurrentUserLogin().orElse("system");
        return ResponseEntity.ok(toResponse(restaurantService.updateRestaurant(id, request, user)));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<RestaurantResponse> toggleActive(@PathVariable("id") Long id) {
        return ResponseEntity.ok(toResponse(restaurantService.toggleActive(id)));
    }

    @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RestaurantResponse> uploadPhoto(
            @PathVariable("id") Long id,
            @RequestParam("photo") MultipartFile photo) throws IOException, SQLException {
        return ResponseEntity.ok(toResponse(restaurantService.uploadPhoto(id, photo)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }

    // ── Booking ───────────────────────────────────────────────────

    @PostMapping("/{restaurantId}/bookings")
    public ResponseEntity<RestaurantBookingResponse> book(
            @PathVariable("restaurantId") Long restaurantId,
            @Valid @RequestBody RestaurantBookingRequest request) {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Restaurant restaurant = restaurantService.getById(restaurantId);

        if (request.getTourBookingId() != null) {
            TourBooking tourBooking = tourBookingRepository.findById(request.getTourBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tour booking không tồn tại: " + request.getTourBookingId()));
            if (!tourBooking.getUser().getEmail().equalsIgnoreCase(email)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Tour booking không thuộc về người dùng hiện tại");
            }
            String tourDest = tourBooking.getTour().getDestination();
            String restCity = restaurant.getCity();
            if (tourDest != null && restCity != null) {
                String d = tourDest.toLowerCase().split("[–\\-/,]")[0].trim();
                String c = restCity.toLowerCase();
                if (!c.contains(d) && !d.contains(c)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Nhà hàng ở " + restCity + " không phù hợp với tour đến " + tourDest);
                }
            }
        }

        RestaurantBooking booking = new RestaurantBooking();
        booking.setRestaurant(restaurant);
        booking.setUser(user);
        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(request.getBookingTime());
        booking.setGuestCount(request.getGuestCount());
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setContactName(request.getContactName());
        booking.setContactPhone(request.getContactPhone());
        booking.setContactEmail(request.getContactEmail());
        booking.setConfirmationCode("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        RestaurantBooking saved = bookingRepository.save(booking);

        try {
            emailService.sendRestaurantBookingConfirmation(
                    request.getContactEmail(),
                    request.getContactName(),
                    restaurant.getName(),
                    request.getBookingDate().toString(),
                    request.getBookingTime().toString(),
                    request.getGuestCount(),
                    saved.getConfirmationCode());
        } catch (Exception ignored) {}

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toBookingResponse(saved));
    }

    @GetMapping("/bookings/my")
    public ResponseEntity<List<RestaurantBookingResponse>> myBookings() {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        return ResponseEntity.ok(
                bookingRepository.findByUserEmailOrderByCreatedAtDesc(email)
                        .stream().map(this::toBookingResponse).toList());
    }

    @GetMapping("/bookings/all")
    public ResponseEntity<List<RestaurantBookingResponse>> allBookings() {
        return ResponseEntity.ok(
                bookingRepository.findAllByOrderByCreatedAtDesc()
                        .stream().map(this::toBookingResponse).toList());
    }

    @PatchMapping("/bookings/{bookingId}/status")
    public ResponseEntity<RestaurantBookingResponse> updateStatus(
            @PathVariable("bookingId") Long bookingId,
            @RequestParam("status") RestaurantBookingStatus status) {
        RestaurantBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        booking.setStatus(status);
        return ResponseEntity.ok(toBookingResponse(bookingRepository.save(booking)));
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<RestaurantBookingResponse> cancel(@PathVariable("bookingId") Long bookingId) {
        RestaurantBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        booking.setStatus(RestaurantBookingStatus.CANCELLED);
        return ResponseEntity.ok(toBookingResponse(bookingRepository.save(booking)));
    }

    // ── Favorites ─────────────────────────────────────────────────

    @PostMapping("/{restaurantId}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable("restaurantId") Long restaurantId) {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Restaurant restaurant = restaurantService.getById(restaurantId);
        Optional<RestaurantFavorite> existing = favoriteRepository.findByUserIdAndRestaurantId(user.getId(), restaurantId);
        boolean favorited;
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            favorited = false;
        } else {
            RestaurantFavorite fav = new RestaurantFavorite();
            fav.setUser(user);
            fav.setRestaurant(restaurant);
            favoriteRepository.save(fav);
            favorited = true;
        }
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/{restaurantId}/favorite")
    public ResponseEntity<Map<String, Object>> checkFavorite(@PathVariable("restaurantId") Long restaurantId) {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) return ResponseEntity.ok(Map.of("favorited", false));
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.ok(Map.of("favorited", false));
        boolean favorited = favoriteRepository.existsByUserIdAndRestaurantId(user.getId(), restaurantId);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/my-favorites")
    public ResponseEntity<List<RestaurantResponse>> getMyFavorites() {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) return ResponseEntity.ok(java.util.List.of());
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.ok(java.util.List.of());
        List<Long> restaurantIds = favoriteRepository.findByUserId(user.getId())
                .stream().map(f -> f.getRestaurant().getId()).toList();
        return ResponseEntity.ok(restaurantIds.stream()
                .map(id -> toResponse(restaurantService.getById(id))).toList());
    }

    // ── Helpers ───────────────────────────────────────────────────

    private RestaurantResponse toResponse(Restaurant r) {
        byte[] photoBytes = null;
        if (r.getPhoto() != null) {
            try { photoBytes = r.getPhoto().getBytes(1, (int) r.getPhoto().length()); }
            catch (SQLException ignored) {}
        }
        RestaurantResponse res = new RestaurantResponse();
        res.setId(r.getId());
        res.setName(r.getName());
        res.setDescription(r.getDescription());
        res.setAddress(r.getAddress());
        res.setCity(r.getCity());
        res.setCuisineType(r.getCuisineType());
        res.setPriceRange(r.getPriceRange());
        res.setCapacity(r.getCapacity());
        res.setOpeningHours(r.getOpeningHours());
        res.setAmenities(r.getAmenities());
        res.setActive(r.isActive());
        res.setAverageRating(r.getAverageRating());
        res.setPhoto(photoBytes);
        return res;
    }

    private RestaurantBookingResponse toBookingResponse(RestaurantBooking b) {
        RestaurantBookingResponse res = new RestaurantBookingResponse();
        res.setId(b.getId());
        res.setRestaurantId(b.getRestaurant().getId());
        res.setRestaurantName(b.getRestaurant().getName());
        res.setRestaurantCity(b.getRestaurant().getCity());
        res.setBookingDate(b.getBookingDate());
        res.setBookingTime(b.getBookingTime());
        res.setGuestCount(b.getGuestCount());
        res.setSpecialRequests(b.getSpecialRequests());
        res.setContactName(b.getContactName());
        res.setContactPhone(b.getContactPhone());
        res.setContactEmail(b.getContactEmail());
        res.setStatus(b.getStatus());
        res.setConfirmationCode(b.getConfirmationCode());
        res.setCreatedAt(b.getCreatedAt());
        return res;
    }
}

