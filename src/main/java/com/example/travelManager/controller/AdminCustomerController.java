package com.example.travelManager.controller;

import com.example.travelManager.domain.Role;
import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.RoleRepository;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TourBookingRepository tourBookingRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Role staffRole = roleRepository.findByName("STAFF");
        Role adminRole = roleRepository.findByName("ADMIN");

        List<UserEntity> all = userRepository.findAll().stream()
                .filter(u -> !u.getRole().equals(staffRole) && !u.getRole().equals(adminRole))
                .filter(u -> {
                    if ("ACTIVE".equals(status)) return Boolean.TRUE.equals(u.getIsActive());
                    if ("LOCKED".equals(status)) return !Boolean.TRUE.equals(u.getIsActive());
                    return true;
                })
                .filter(u -> search == null || search.isBlank()
                        || u.getName().toLowerCase().contains(search.toLowerCase())
                        || u.getEmail().toLowerCase().contains(search.toLowerCase()))
                .sorted((a, b) -> {
                    Instant ia = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH;
                    Instant ib = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH;
                    return ib.compareTo(ia);
                })
                .collect(Collectors.toList());

        int total = all.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);
        List<CustomerResponse> content = all.subList(fromIdx, toIdx).stream()
                .map(this::toResponse).toList();

        Page<CustomerResponse> result = new PageImpl<>(content,
                PageRequest.of(page, size, Sort.by("createdAt").descending()), total);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<CustomerResponse> lock(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> body) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng không tồn tại"));
        Role adminRole = roleRepository.findByName("ADMIN");
        if (user.getRole() != null && user.getRole().equals(adminRole)) {
            throw new IllegalArgumentException("Không thể khóa tài khoản ADMIN");
        }
        user.setIsActive(false);
        String reason = body.containsKey("reason") ? body.get("reason").toString() : "";
        user.setLockReason(reason);
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<CustomerResponse> unlock(@PathVariable("id") Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng không tồn tại"));
        user.setIsActive(true);
        user.setLockReason(null);
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    private CustomerResponse toResponse(UserEntity u) {
        CustomerResponse r = new CustomerResponse();
        r.setId(u.getId());
        r.setFullName(u.getName());
        r.setEmail(u.getEmail());
        r.setPhone(u.getPhone());
        r.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        r.setStatus(Boolean.TRUE.equals(u.getIsActive()) ? "ACTIVE" : "LOCKED");
        r.setLockReason(u.getLockReason());

        List<TourBooking> tourBookings = tourBookingRepository.findByUserId(u.getId());
        long restCount = restaurantBookingRepository.findByUserEmailOrderByCreatedAtDesc(u.getEmail()).size();
        r.setBookingsCount((int) (tourBookings.size() + restCount));

        BigDecimal totalSpent = tourBookings.stream()
                .filter(b -> b.getFinalPrice() != null)
                .map(TourBooking::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        r.setTotalSpent(totalSpent.doubleValue());

        if (u.getAvatar() != null) {
            r.setAvatarUrl("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(u.getAvatar()));
        }
        return r;
    }

    @Data
    public static class CustomerResponse {
        private long id;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
        private String createdAt;
        private String status;
        private int bookingsCount;
        private double totalSpent;
        private String lockReason;
        private String lockedUntil;
    }
}

