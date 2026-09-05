package com.example.travelManager.domain.tour;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.travelManager.util.constant.tour.TourRecurrenceType;
import com.example.travelManager.util.constant.tour.TourStatus;
import com.example.travelManager.util.constant.tour.TourType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tours")
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String destination;   // diem den
    private String departure;     // diem xuat phat

    private BigDecimal priceAdult;
    private BigDecimal priceChild;

    private int durationDays;   // so ngay
    private int durationNights; // so dem
    private int maxSlots;       // suc chua toi da

    private Double packageDiscountPercent; // % giam gia khi dat theo tour package

    // ── Lịch khởi hành lặp ───────────────────────────────────────
    // TourDepartureScheduler đọc 3 cột này để tự sinh chuyến mới, nhờ đó lịch
    // không cạn dần rồi khiến trang chi tiết hiện "Chưa có lịch khởi hành".

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TourRecurrenceType recurrenceType = TourRecurrenceType.NONE;

    /** WEEKLY: "MON,FRI" — MONTHLY: "5,20". Rỗng thì tour bị bỏ qua khi sinh lịch. */
    @Column(length = 100)
    private String recurrenceDays;

    /** Luôn giữ sẵn lịch trước bao nhiêu tháng. Null dùng mặc định của scheduler. */
    private Integer monthsAhead;

    @Column(columnDefinition = "TEXT")
    private String linkedHotels;      // JSON array of linked hotel IDs

    @Column(columnDefinition = "TEXT")
    private String linkedRestaurants; // JSON array of linked restaurant IDs

    @Column(columnDefinition = "TEXT")
    private String linkedDestinations; // JSON array of linked destination IDs

    @Enumerated(EnumType.STRING)
    private TourType tourType; // DOMESTIC / INTERNATIONAL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TourStatus status = TourStatus.ACTIVE;

    private boolean deleted = false; // soft delete

    @Column(columnDefinition = "TEXT")
    private String cancellationPolicy; // chinh sach huy

    @Column(columnDefinition = "TEXT")
    private String includedServices; // dich vu bao gom

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TourImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    private List<TourItinerary> itineraries = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("departureDate ASC")
    private List<TourDeparture> departures = new ArrayList<>();

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
