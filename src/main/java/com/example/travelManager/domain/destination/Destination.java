package com.example.travelManager.domain.destination;

import com.example.travelManager.util.constant.destination.DestinationType;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Blob;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "destinations")
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private DestinationType destinationType;

    private String province;

    @Column(nullable = false)
    private String city;

    private String address;

    private Double latitude;

    private Double longitude;

    @Lob
    private Blob photo;

    private boolean isActive = true;

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
