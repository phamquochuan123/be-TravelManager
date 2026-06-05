package com.example.travelManager.domain.tour;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.util.constant.tour.BookingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "tour_bookings")
public class TourBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id")
    private TourDeparture departure; // ngay khoi hanh duoc chon

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private TourCoupon coupon; // nullable - co the khong dung ma

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_room_id")
    private BookedRoom bookedRoom; // nullable - chi co khi tour >= 2 ngay va khach chon hotel

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_booking_id")
    private RestaurantBooking restaurantBooking; // nullable - tuy chon

    // thong tin lien he
    private String contactName;
    private String contactPhone;
    private String contactEmail;

    private int numAdults;
    private int numChildren;

    // tien
    private BigDecimal originalPrice;       // gia goc (tong truoc discount)
    private BigDecimal packageHotelPrice;   // phan gia hotel trong package
    private BigDecimal packageRestaurantPrice; // phan gia restaurant trong package
    private BigDecimal discountAmount;      // tong so tien giam (package + coupon)
    private BigDecimal finalPrice;          // gia cuoi cung

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

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
