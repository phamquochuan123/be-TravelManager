package com.example.travelManager.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TOUR | HOTEL | RESTAURANT
    @Column(nullable = false)
    private String bookingType;

    @Column(nullable = false)
    private Long bookingId;

    private String userEmail;

    private BigDecimal amount;

    // VNPay txn ref (unique per payment attempt)
    @Column(unique = true)
    private String txnRef;

    // VNPay order info (mô tả giao dịch)
    private String orderInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    // VNPay response code (00 = success)
    private String responseCode;

    // VNPay transaction no
    private String transactionNo;

    // VNPay bank code
    private String bankCode;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, REFUNDED
    }
}
