package com.example.travelManager.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.travelManager.domain.User;
import com.example.travelManager.domain.io.ProfileRequest;
import com.example.travelManager.domain.io.ProfileResponse;

import com.example.travelManager.repository.UserRepository;

@Service

public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    public ProfileServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public ProfileResponse createProfile(ProfileRequest profileRequest) {
        User newProfile = convertToUserDTO(profileRequest);
        newProfile = userRepository.save(newProfile);
        return convertToProfileResponse(newProfile);
    }

    private ProfileResponse convertToProfileResponse(User newProfile) {
        return ProfileResponse.builder()
                .name(newProfile.getName())
                .email(newProfile.getEmail())
                .userId(newProfile.getUserId())
                .isAccountVerified(newProfile.getIsAccountVerified())
                .build();
    }

    private User convertToUserDTO(ProfileRequest profileRequest) {
        return User.builder()
                .email(profileRequest.getEmail())
                .name(profileRequest.getName())
                .passWord(profileRequest.getPassWord())
                .userId(UUID.randomUUID().toString())
                .isAccountVerified(false)
                .resetOtpExpireAt(0L)
                .verifyOtp(null)
                .verifyOtpExpireAt(0L)
                .resetOtp(null)
                .build();
    }

}
