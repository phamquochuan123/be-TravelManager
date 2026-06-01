package com.example.travelManager.service;

import java.time.LocalDate;

import com.example.travelManager.domain.request.ProfileRequest;
import com.example.travelManager.domain.response.ProfileResponse;

public interface ProfileService {

    ProfileResponse createProfile(ProfileRequest profileRequest);

    ProfileResponse setupAdmin(ProfileRequest profileRequest);

    ProfileResponse getProfile(String email);

    void sendResetOtp(String email);

    void resetPassword(String email, String otp, String newPassword);

    void sendOtp(String email);

    void verifyOtp(String email, String otp);

    ProfileResponse updateProfile(String email, String name, String phone, LocalDate birthDate, String gender);

    void changePassword(String email, String currentPassword, String newPassword);

    ProfileResponse updateAvatar(String email, byte[] avatarData);

}
