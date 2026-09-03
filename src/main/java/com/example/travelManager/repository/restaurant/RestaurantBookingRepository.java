package com.example.travelManager.repository.restaurant;

import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface RestaurantBookingRepository extends JpaRepository<RestaurantBooking, Long> {
    List<RestaurantBooking> findByUserEmailOrderByCreatedAtDesc(String email);
    List<RestaurantBooking> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    Optional<RestaurantBooking> findByConfirmationCode(String confirmationCode);
    List<RestaurantBooking> findAllByOrderByCreatedAtDesc();

    /** Bản phân trang ở DB của findAllByOrderByCreatedAtDesc, dùng cho màn quản trị. */
    Page<RestaurantBooking> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<RestaurantBooking> findByCreatedAtBetweenOrderByCreatedAtDesc(java.time.Instant from, java.time.Instant to);

    @Query("SELECT COALESCE(SUM(rb.guestCount), 0) FROM RestaurantBooking rb " +
            "WHERE rb.restaurant.id = :restaurantId AND rb.bookingDate = :bookingDate " +
            "AND rb.bookingTime = :bookingTime AND rb.status <> :excludeStatus")
    int sumGuestCountByRestaurantAndDateTime(@Param("restaurantId") Long restaurantId,
            @Param("bookingDate") LocalDate bookingDate, @Param("bookingTime") LocalTime bookingTime,
            @Param("excludeStatus") RestaurantBookingStatus excludeStatus);
}
