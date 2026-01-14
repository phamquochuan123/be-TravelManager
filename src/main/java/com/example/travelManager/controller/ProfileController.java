package com.example.travelManager.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.travelManager.domain.io.ProfileRequest;
import com.example.travelManager.domain.io.ProfileResponse;

import com.example.travelManager.service.ProfileService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse registResponse(@Valid @RequestBody ProfileRequest profileRequest) {
        ProfileResponse profileResponse = profileService.createProfile(profileRequest);
        return profileResponse;

    }

}
