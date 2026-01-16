package com.example.travelManager.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.travelManager.domain.io.ProfileRequest;
import com.example.travelManager.domain.io.ProfileResponse;
import com.example.travelManager.service.EmailService;
import com.example.travelManager.service.ProfileService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class ProfileController {

    private final ProfileService profileService;
    private final EmailService emailService;

    public ProfileController(ProfileService profileService,
            EmailService emailService) {
        this.profileService = profileService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse registResponse(@Valid @RequestBody ProfileRequest profileRequest) {
        ProfileResponse profileResponse = profileService.createProfile(profileRequest);
        emailService.sendWelcomeEmail(profileResponse.getEmail(), profileResponse.getName());
        return profileResponse;
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(@CurrentSecurityContext(expression = "authentication?.name") String email) {
        return profileService.getProfile(email);
    }

}
