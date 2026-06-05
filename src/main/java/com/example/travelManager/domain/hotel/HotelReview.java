package com.example.travelManager.domain.hotel;

import com.example.travelManager.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "hotel_reviews")
public class HotelReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private BookedRoom booking;

    private int rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "LONGTEXT")
    private String images; // JSON array of base64 strings

    @Column(columnDefinition = "TEXT")
    private String adminReply;

    private boolean isHidden = true;

    private String status = "PENDING"; // PENDING | APPROVED | REJECTED

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
