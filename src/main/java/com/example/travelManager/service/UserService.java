package com.example.travelManager.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.travelManager.domain.Role;
import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.response.UserResponse;
import com.example.travelManager.exception.DuplicateResourceException;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.RoleRepository;
import com.example.travelManager.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        return toResponse(user);
    }

    public UserResponse assignRole(long userId, long roleId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        if (user.getRole() != null && "ADMIN".equals(user.getRole().getName())) {
            throw new IllegalArgumentException("Không thể thay đổi role của tài khoản ADMIN");
        }
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));
        if ("ADMIN".equals(role.getName())) {
            throw new IllegalArgumentException("Không thể gán role ADMIN qua API này");
        }
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    public void deleteUser(long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        if (user.getRole() != null && "ADMIN".equals(user.getRole().getName())) {
            throw new IllegalArgumentException("Không thể xóa tài khoản ADMIN");
        }
        String currentEmail = com.example.travelManager.util.SecurityUtil.getCurrentUserLogin().orElse("");
        if (currentEmail.equals(user.getEmail())) {
            throw new IllegalArgumentException("Không thể tự xóa tài khoản của mình");
        }
        userRepository.delete(user);
    }

    public UserResponse createStaff(String name, String email, String password, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email đã tồn tại");
        }
        Role staffRole = roleRepository.findByName("STAFF");
        if (staffRole == null) throw new ResourceNotFoundException("Role STAFF không tồn tại");
        UserEntity staff = UserEntity.builder()
                .userId(UUID.randomUUID().toString())
                .name(name)
                .email(email)
                .passWord(passwordEncoder.encode(password))
                .phone(phone)
                .isAccountVerified(true)
                .isActive(true)
                .resetOtpExpireAt(0L)
                .verifyOtpExpireAt(0L)
                .role(staffRole)
                .build();
        return toResponse(userRepository.save(staff));
    }

    public UserResponse lockUser(long id, String reason) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        if (user.getRole() != null && "ADMIN".equals(user.getRole().getName())) {
            throw new IllegalArgumentException("Không thể khóa tài khoản ADMIN");
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa rồi");
        }
        user.setIsActive(false);
        user.setLockReason(reason);
        return toResponse(userRepository.save(user));
    }

    public UserResponse unlockUser(long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản đang hoạt động bình thường");
        }
        user.setIsActive(true);
        user.setLockReason(null);
        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .isAccountVerified(user.getIsAccountVerified())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .lockReason(user.getLockReason())
                .build();
    }
}
