package com.example.travelManager.domain.restaurant;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "restaurant_bookings")
public class RestaurantBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime bookingTime;

    @Column(nullable = false)
    private int guestCount;

    @Column(columnDefinition = "TEXT")
    private String specialRequests;

    private String contactName;
    private String contactPhone;
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantBookingStatus status = RestaurantBookingStatus.PENDING;

    @Column(unique = true)
    private String confirmationCode;

    @Column(name = "admin_note")
    private String adminNote;

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
}
