package com.example.travelManager.domain;

import com.example.travelManager.domain.tour.TourDeparture;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false)
    private TourDeparture departure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter; // Staff gửi báo cáo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType incidentType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String adminNote; // Admin phản hồi

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

    public enum IncidentType {
        WEATHER,        // Thời tiết xấu
        PASSENGER,      // Sự cố hành khách
        TRANSPORT,      // Sự cố phương tiện
        HEALTH,         // Y tế / sức khoẻ
        ACCOMMODATION,  // Sự cố lưu trú
        OTHER           // Khác
    }

    public enum IncidentStatus {
        OPEN,       // Mới báo cáo
        REVIEWING,  // Admin đang xem xét
        RESOLVED    // Đã xử lý
    }
}
