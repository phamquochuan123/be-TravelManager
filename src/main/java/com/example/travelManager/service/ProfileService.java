package com.example.travelManager.service;

import com.example.travelManager.domain.io.ProfileResponse;
import com.example.travelManager.domain.io.ProfileRequest;

public interface ProfileService {

    ProfileResponse createProfile(ProfileRequest profileRequest);
}
