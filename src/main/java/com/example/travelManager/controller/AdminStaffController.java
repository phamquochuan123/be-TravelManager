package com.example.travelManager.controller;

import com.example.travelManager.domain.Role;
import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.exception.DuplicateResourceException;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.RoleRepository;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TourDepartureRepository departureRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Role staffRole = roleRepository.findByName("STAFF");
        List<UserEntity> all = userRepository.findAll().stream()
                .filter(u -> staffRole != null && staffRole.equals(u.getRole()))
                .filter(u -> active == null || active.equals(u.getIsActive()))
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
        List<StaffResponse> content = all.subList(fromIdx, toIdx).stream()
                .map(this::toResponse).toList();

        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 1;
        return ResponseEntity.ok(Map.of(
                "content", content,
                "totalElements", total,
                "totalPages", totalPages
        ));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<StaffResponse> create(
            @RequestParam(name = "fullName") String fullName,
            @RequestParam(name = "email") String email,
            @RequestParam(name = "phone", required = false, defaultValue = "") String phone,
            @RequestParam(name = "password") String password,
            @RequestParam(name = "sendEmail", required = false, defaultValue = "false") boolean sendEmail,
            @RequestParam(name = "avatar", required = false) MultipartFile avatar) throws IOException {

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email đã tồn tại");
        }
        Role staffRole = roleRepository.findByName("STAFF");
        if (staffRole == null) throw new ResourceNotFoundException("Role STAFF không tồn tại");

        UserEntity staff = UserEntity.builder()
                .userId(UUID.randomUUID().toString())
                .name(fullName)
                .email(email)
                .passWord(passwordEncoder.encode(password))
                .phone(phone)
                .isAccountVerified(true)
                .isActive(true)
                .resetOtpExpireAt(0L)
                .verifyOtpExpireAt(0L)
                .role(staffRole)
                .build();
        if (avatar != null && !avatar.isEmpty()) {
            staff.setAvatar(avatar.getBytes());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(userRepository.save(staff)));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<StaffResponse> update(
            @PathVariable("id") Long id,
            @RequestParam(name = "fullName") String fullName,
            @RequestParam(name = "phone", required = false, defaultValue = "") String phone,
            @RequestParam(name = "avatar", required = false) MultipartFile avatar) throws IOException {

        UserEntity staff = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại"));
        staff.setName(fullName);
        staff.setPhone(phone);
        if (avatar != null && !avatar.isEmpty()) {
            staff.setAvatar(avatar.getBytes());
        }
        return ResponseEntity.ok(toResponse(userRepository.save(staff)));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<StaffResponse> toggleActive(@PathVariable("id") Long id) {
        UserEntity staff = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại"));
        staff.setIsActive(!Boolean.TRUE.equals(staff.getIsActive()));
        if (Boolean.TRUE.equals(staff.getIsActive())) {
            staff.setLockReason(null);
        }
        return ResponseEntity.ok(toResponse(userRepository.save(staff)));
    }

    private StaffResponse toResponse(UserEntity u) {
        StaffResponse r = new StaffResponse();
        r.setId(u.getId());
        r.setFullName(u.getName());
        r.setEmail(u.getEmail());
        r.setPhone(u.getPhone());
        r.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        r.setActive(Boolean.TRUE.equals(u.getIsActive()));
        r.setToursCount(departureRepository.findByStaffId(u.getId()).size());
        if (u.getAvatar() != null) {
            r.setAvatarUrl("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(u.getAvatar()));
        }
        return r;
    }

    @Data
    public static class StaffResponse {
        private long id;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
        private String createdAt;
        private boolean active;
        private int toursCount;
    }
}

