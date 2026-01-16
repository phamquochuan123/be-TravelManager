package com.example.travelManager.service;

import com.example.travelManager.domain.io.ProfileResponse;
import com.example.travelManager.domain.io.ProfileRequest;

public interface ProfileService {

    ProfileResponse createProfile(ProfileRequest profileRequest);

    ProfileResponse getProfile(String email);

    void sendResetOtp(String email);

    void resetPassword(String email, String otp, String newPassword);
}
