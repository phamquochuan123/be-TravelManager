package com.example.travelManager.domain.restaurant;

import com.example.travelManager.util.constant.restaurant.CuisineType;
import com.example.travelManager.util.constant.restaurant.PriceRange;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Blob;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;

    @Column(nullable = false)
    private String city;

    @Enumerated(EnumType.STRING)
    private CuisineType cuisineType;

    @Enumerated(EnumType.STRING)
    private PriceRange priceRange;

    private Integer capacity;

    // e.g. "08:00-22:00"
    private String openingHours;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    private boolean isActive = true;

    @Lob
    private Blob photo;

    private Double averageRating;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RestaurantBooking> bookings = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

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
