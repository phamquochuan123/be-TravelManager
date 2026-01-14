package com.example.travelManager.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.io.ProfileRequest;
import com.example.travelManager.domain.io.ProfileResponse;

import com.example.travelManager.repository.UserRepository;

@Service

public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ProfileResponse createProfile(ProfileRequest profileRequest) {
        UserEntity newProfile = convertToUserDTO(profileRequest);
        if (!userRepository.existsByEmail(profileRequest.getEmail())) {
            newProfile = userRepository.save(newProfile);
            return convertToProfileResponse(newProfile);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    private ProfileResponse convertToProfileResponse(UserEntity newProfile) {
        return ProfileResponse.builder()
                .name(newProfile.getName())
                .email(newProfile.getEmail())
                .userId(newProfile.getUserId())
                .isAccountVerified(newProfile.getIsAccountVerified())
                .build();
    }

    private UserEntity convertToUserDTO(ProfileRequest profileRequest) {
        return UserEntity.builder()
                .email(profileRequest.getEmail())
                .name(profileRequest.getName())
                .passWord(passwordEncoder.encode(profileRequest.getPassWord()))
                .userId(UUID.randomUUID().toString())
                .isAccountVerified(false)
                .resetOtpExpireAt(0L)
                .verifyOtp(null)
                .verifyOtpExpireAt(0L)
                .resetOtp(null)
                .build();
    }

}
