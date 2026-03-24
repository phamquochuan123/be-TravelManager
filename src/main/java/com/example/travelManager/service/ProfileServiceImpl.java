package com.example.travelManager.service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.travelManager.domain.Role;
import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.request.ProfileRequest;
import com.example.travelManager.domain.response.ProfileResponse;
import com.example.travelManager.exception.DuplicateResourceException;
import com.example.travelManager.exception.InvalidOtpException;
import com.example.travelManager.exception.OtpExpiredException;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.RoleRepository;
import com.example.travelManager.repository.UserRepository;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RoleRepository roleRepository;

    public ProfileServiceImpl(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.roleRepository = roleRepository;
    }

    @Override
    public ProfileResponse createProfile(ProfileRequest profileRequest) {
        UserEntity newProfile = convertToUserDTO(profileRequest);
        if (!userRepository.existsByEmail(profileRequest.getEmail())) {
            newProfile = userRepository.save(newProfile);
            return convertToProfileResponse(newProfile);
        }
        throw new DuplicateResourceException("Email đã tồn tại");
    }

    @Override
    public ProfileResponse setupAdmin(ProfileRequest profileRequest) {
        Role adminRole = roleRepository.findByName("ADMIN");
        if (userRepository.existsByRole(adminRole)) {
            throw new DuplicateResourceException("Tài khoản admin đã tồn tại");
        }
        if (userRepository.existsByEmail(profileRequest.getEmail())) {
            throw new DuplicateResourceException("Email đã tồn tại");
        }
        UserEntity admin = UserEntity.builder()
                .email(profileRequest.getEmail())
                .name(profileRequest.getName())
                .passWord(passwordEncoder.encode(profileRequest.getPassWord()))
                .userId(UUID.randomUUID().toString())
                .isAccountVerified(true)
                .resetOtpExpireAt(0L)
                .verifyOtpExpireAt(0L)
                .role(adminRole)
                .build();
        admin = userRepository.save(admin);
        return convertToProfileResponse(admin);
    }

    private ProfileResponse convertToProfileResponse(UserEntity newProfile) {
        return ProfileResponse.builder()
                .name(newProfile.getName())
                .email(newProfile.getEmail())
                .userId(newProfile.getUserId())
                .isAccountVerified(newProfile.getIsAccountVerified())
                .roleName(newProfile.getRole() != null ? newProfile.getRole().getName() : null)
                .build();
    }

    private UserEntity convertToUserDTO(ProfileRequest profileRequest) {
        Role userRole = roleRepository.findByName("USER");
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
                .role(userRole)
                .build();
    }

    @Override
    public ProfileResponse getProfile(String email) {
        UserEntity exitingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + email));
        return convertToProfileResponse(exitingUser);
    }

    @Override
    public void sendResetOtp(String email) {
        UserEntity existingEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + email));

        // generate 6 digit otp
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        // calculate expiry time (current time + 15 minutes in milliseconds)
        long expiryTime = System.currentTimeMillis() + (15 * 60 * 1000);

        // update the profile/user
        existingEntity.setResetOtp(otp);
        existingEntity.setResetOtpExpireAt(expiryTime);

        // save into the database
        userRepository.save(existingEntity);
        try {
            emailService.sendResetOtpEmail(existingEntity.getEmail(), otp);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to send email");
        }
    }

    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + email));

        if (existingUser.getResetOtp() == null || !existingUser.getResetOtp().equals(otp)) {
            throw new InvalidOtpException("OTP không hợp lệ");
        }

        if (existingUser.getResetOtpExpireAt() < System.currentTimeMillis()) {
            throw new OtpExpiredException("OTP đã hết hạn");
        }

        existingUser.setPassWord(passwordEncoder.encode(newPassword));
        existingUser.setResetOtp(null);
        existingUser.setResetOtpExpireAt(0L);

        userRepository.save(existingUser);
    }

    @Override
    public void sendOtp(String email) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + email));

        // generate 6 digit otp
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        // calculate expiry time (current time + 24 hours in milliseconds)
        long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);

        // Update the user entity
        existingUser.setVerifyOtp(otp);
        existingUser.setVerifyOtpExpireAt(expiryTime);

        // save to database
        userRepository.save(existingUser);

        try {
            emailService.sendOtpEmail(existingUser.getEmail(), otp);
        } catch (Exception e) {
            throw new RuntimeException("Unable to send email");
        }
    }

    @Override
    public void verifyOtp(String email, String otp) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + email));
        if (existingUser.getVerifyOtp() == null || !existingUser.getVerifyOtp().equals(otp)) {
            throw new InvalidOtpException("OTP không hợp lệ");
        }
        if (existingUser.getVerifyOtpExpireAt() < System.currentTimeMillis()) {
            throw new OtpExpiredException("OTP đã hết hạn");
        }

        existingUser.setIsAccountVerified(true);
        existingUser.setVerifyOtp(null);
        existingUser.setVerifyOtpExpireAt(0L);

        userRepository.save(existingUser);
    }

}
