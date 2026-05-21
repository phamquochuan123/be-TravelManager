package com.example.travelManager.controller;

import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.destination.DestinationRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.util.constant.tour.BookingStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final BookedRoomRepository hotelBookingRepository;
    private final TourRepository tourRepository;
    private final TourBookingRepository tourBookingRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;
    private final DestinationRepository destinationRepository;

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> overview() {
        OverviewResponse res = new OverviewResponse();

        // Entity counts
        res.setTotalUsers(userRepository.count());
        res.setActiveUsers(userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive())).count());

        res.setTotalHotels(hotelRepository.count());
        res.setActiveHotels(hotelRepository.findByIsActiveTrue().size());

        res.setTotalTours(tourRepository.count());
        res.setActiveTours(tourRepository.findAll().stream()
                .filter(t -> t.getStatus() != null &&
                        t.getStatus().name().equals("ACTIVE")).count());

        res.setTotalRestaurants(restaurantRepository.count());
        res.setActiveRestaurants(restaurantRepository.findByIsActiveTrue().size());

        res.setTotalDestinations(destinationRepository.count());
        res.setActiveDestinations(destinationRepository.findByIsActiveTrue().size());

        // Hotel bookings
        List<com.example.travelManager.domain.hotel.BookedRoom> hotelBookings = hotelBookingRepository.findAll();
        res.setTotalHotelBookings((long) hotelBookings.size());

        // Tour bookings
        List<TourBooking> tourBookings = tourBookingRepository.findAll();
        res.setTotalTourBookings((long) tourBookings.size());

        Map<String, Long> tourByStatus = tourBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));
        res.setTourBookingsByStatus(tourByStatus);

        BigDecimal tourRevenue = tourBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(TourBooking::getFinalPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        res.setTourRevenue(tourRevenue);

        // Restaurant bookings
        List<RestaurantBooking> restaurantBookings = restaurantBookingRepository.findAll();
        res.setTotalRestaurantBookings((long) restaurantBookings.size());

        Map<String, Long> restByStatus = restaurantBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));
        res.setRestaurantBookingsByStatus(restByStatus);

        // Recent 30 days stats
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        res.setNewUsersLast30Days(userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(thirtyDaysAgo))
                .count());
        res.setNewTourBookingsLast30Days(tourBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(thirtyDaysAgo))
                .count());
        res.setNewRestaurantBookingsLast30Days(restaurantBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(thirtyDaysAgo))
                .count());
        res.setNewHotelBookingsLast30Days((long) hotelBookings.size()); // no createdAt on BookedRoom

        return ResponseEntity.ok(res);
    }

    @GetMapping("/tour-bookings")
    public ResponseEntity<List<TourBookingStatRow>> tourBookingStats() {
        List<TourBooking> all = tourBookingRepository.findAllByOrderByCreatedAtDesc();
        List<TourBookingStatRow> rows = all.stream().map(b -> {
            TourBookingStatRow row = new TourBookingStatRow();
            row.setId(b.getId());
            row.setTourName(b.getTour() != null ? b.getTour().getName() : "");
            row.setContactName(b.getContactName());
            row.setContactEmail(b.getContactEmail());
            row.setNumAdults(b.getNumAdults());
            row.setNumChildren(b.getNumChildren());
            row.setFinalPrice(b.getFinalPrice());
            row.setStatus(b.getStatus() != null ? b.getStatus().name() : "");
            row.setCreatedAt(b.getCreatedAt());
            return row;
        }).toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/restaurant-bookings")
    public ResponseEntity<List<RestaurantBookingStatRow>> restaurantBookingStats() {
        List<RestaurantBooking> all = restaurantBookingRepository.findAllByOrderByCreatedAtDesc();
        List<RestaurantBookingStatRow> rows = all.stream().map(b -> {
            RestaurantBookingStatRow row = new RestaurantBookingStatRow();
            row.setId(b.getId());
            row.setRestaurantName(b.getRestaurant() != null ? b.getRestaurant().getName() : "");
            row.setContactName(b.getContactName());
            row.setContactEmail(b.getContactEmail());
            row.setGuestCount(b.getGuestCount());
            row.setBookingDate(b.getBookingDate() != null ? b.getBookingDate().toString() : "");
            row.setStatus(b.getStatus() != null ? b.getStatus().name() : "");
            row.setCreatedAt(b.getCreatedAt());
            return row;
        }).toList();
        return ResponseEntity.ok(rows);
    }

    // ── Response DTOs ──────────────────────────────────────────────

    @Data
    public static class OverviewResponse {
        private long totalUsers;
        private long activeUsers;
        private long totalHotels;
        private long activeHotels;
        private long totalTours;
        private long activeTours;
        private long totalRestaurants;
        private long activeRestaurants;
        private long totalDestinations;
        private long activeDestinations;
        private long totalHotelBookings;
        private long totalTourBookings;
        private long totalRestaurantBookings;
        private Map<String, Long> tourBookingsByStatus;
        private Map<String, Long> restaurantBookingsByStatus;
        private BigDecimal tourRevenue;
        private long newUsersLast30Days;
        private long newTourBookingsLast30Days;
        private long newRestaurantBookingsLast30Days;
        private long newHotelBookingsLast30Days;
    }

    @Data
    public static class TourBookingStatRow {
        private Long id;
        private String tourName;
        private String contactName;
        private String contactEmail;
        private int numAdults;
        private int numChildren;
        private BigDecimal finalPrice;
        private String status;
        private Instant createdAt;
    }

    @Data
    public static class RestaurantBookingStatRow {
        private Long id;
        private String restaurantName;
        private String contactName;
        private String contactEmail;
        private int guestCount;
        private String bookingDate;
        private String status;
        private Instant createdAt;
    }
}
