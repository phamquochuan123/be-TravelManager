package com.example.travelManager.controller.restaurant;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.request.restaurant.RestaurantBookingRequest;
import com.example.travelManager.domain.request.restaurant.RestaurantRequest;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.response.restaurant.RestaurantBookingResponse;
import com.example.travelManager.domain.response.restaurant.RestaurantResponse;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.service.EmailService;
import com.example.travelManager.service.restaurant.IRestaurantService;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
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
import java.util.UUID;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final IRestaurantService restaurantService;
    private final RestaurantBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ── Restaurant CRUD ──────────────────────────────────────────

    @PostMapping
    public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody RestaurantRequest request) {
        String user = SecurityUtil.getCurrentUserLogin().orElse("system");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(restaurantService.createRestaurant(request, user)));
    }

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAll(
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "admin", defaultValue = "false") boolean admin) {
        List<Restaurant> list;
        if (admin) list = restaurantService.getAllAdmin();
        else if (city != null) list = restaurantService.getByCity(city);
        else list = restaurantService.getAllActive();
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

