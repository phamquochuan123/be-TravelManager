package com.example.travelManager.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.travelManager.domain.Role;
import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.response.UserResponse;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.RoleRepository;
import com.example.travelManager.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    public void deleteUser(long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User không tồn tại");
        }
        userRepository.deleteById(id);
    }

    public UserResponse lockUser(long id, String reason) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        user.setIsActive(false);
        user.setLockReason(reason);
        return toResponse(userRepository.save(user));
    }

    public UserResponse unlockUser(long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
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
