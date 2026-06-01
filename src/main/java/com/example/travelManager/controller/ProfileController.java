package com.example.travelManager.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.RestController;

import com.example.travelManager.domain.request.ProfileRequest;
import com.example.travelManager.domain.response.ProfileResponse;
import com.example.travelManager.service.EmailService;
import com.example.travelManager.service.ProfileService;

import jakarta.validation.Valid;
import lombok.Data;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

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
        // Gửi OTP xác thực email ngay sau khi đăng ký
        profileService.sendOtp(profileResponse.getEmail());
        return profileResponse;
    }

    @PostMapping("/admin/setup")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse setupAdmin(@Valid @RequestBody ProfileRequest profileRequest) {
        return profileService.setupAdmin(profileRequest);
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(@CurrentSecurityContext(expression = "authentication?.name") String email) {
        return profileService.getProfile(email);
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(
            @CurrentSecurityContext(expression = "authentication?.name") String email,
            @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(email, request.getName(), request.getPhone(),
                request.getBirthDate(), request.getGender());
    }

    @PatchMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse updateAvatar(
            @CurrentSecurityContext(expression = "authentication?.name") String email,
            @RequestParam MultipartFile file) throws java.io.IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File không được rỗng");
        return profileService.updateAvatar(email, file.getBytes());
    }

    @PostMapping("/profile/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @CurrentSecurityContext(expression = "authentication?.name") String email,
            @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
    }

    @Data
    static class UpdateProfileRequest {
        private String name;
        private String phone;
        private LocalDate birthDate;
        private String gender;
    }

    @Data
    static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }

}
