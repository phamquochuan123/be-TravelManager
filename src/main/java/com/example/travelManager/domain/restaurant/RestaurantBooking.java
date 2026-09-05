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

    /**
     * Đơn tour đã kèm lượt đặt bàn này, null nếu khách đặt bàn lẻ.
     *
     * Khoá ngoại nằm ở BÊN NÀY chứ không phải tour_bookings.restaurant_booking_id như
     * trước: một đơn tour giờ kèm được nhiều bữa, mà cột đơn lẻ bên tour_bookings chỉ
     * chứa nổi một. Xem migrate_tour_multi_restaurant.sql.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_booking_id")
    private com.example.travelManager.domain.tour.TourBooking tourBooking;

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
