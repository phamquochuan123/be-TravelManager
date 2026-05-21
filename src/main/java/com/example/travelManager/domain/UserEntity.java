package com.example.travelManager.domain;

import java.time.Instant;

import com.example.travelManager.util.SecurityUtil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String userId;

    private String name;

    @Column(unique = true)
    private String email;

    private String passWord;

    private String verifyOtp;
    private Boolean isAccountVerified;
    private long verifyOtpExpireAt;
    private String resetOtp;
    private long resetOtpExpireAt;
    // Đếm số lần nhập OTP sai liên tiếp để chống brute-force
    @Builder.Default
    private int otpAttempts = 0;

    private String phone;

    @Column(columnDefinition = "LONGBLOB")
    private byte[] avatar;

    @Builder.Default
    private Boolean isActive = true;

    private String lockReason;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    private String createdBy;
    private String updatedBy;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        this.updatedAt = Instant.now();
    }
}
