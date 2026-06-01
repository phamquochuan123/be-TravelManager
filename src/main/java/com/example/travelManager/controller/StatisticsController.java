package com.example.travelManager.controller;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.repository.IncidentReportRepository;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.destination.DestinationRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.util.constant.tour.BookingStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private final TourDepartureRepository departureRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;
    private final DestinationRepository destinationRepository;
    private final IncidentReportRepository incidentReportRepository;

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> overview() {
        OverviewResponse res = new OverviewResponse();

        // Cache lists — tránh gọi DB nhiều lần cho cùng data
        List<UserEntity> allUsers = userRepository.findAll();
        List<Tour> allTours = tourRepository.findAll();
        List<com.example.travelManager.domain.hotel.BookedRoom> hotelBookings = hotelBookingRepository.findAll();
        List<TourBooking> tourBookings = tourBookingRepository.findAll();
        List<RestaurantBooking> restaurantBookings = restaurantBookingRepository.findAll();

        // Entity counts
        res.setTotalUsers((long) allUsers.size());
        res.setActiveUsers(allUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive())).count());

        res.setTotalHotels(hotelRepository.count());
        res.setActiveHotels(hotelRepository.findByIsActiveTrue().size());

        res.setTotalTours((long) allTours.size());
        res.setActiveTours(allTours.stream()
                .filter(t -> t.getStatus() != null &&
                        t.getStatus().name().equals("ACTIVE")).count());

        res.setTotalRestaurants(restaurantRepository.count());
        res.setActiveRestaurants(restaurantRepository.findByIsActiveTrue().size());

        res.setTotalDestinations(destinationRepository.count());
        res.setActiveDestinations(destinationRepository.findByIsActiveTrue().size());

        // Hotel bookings
        res.setTotalHotelBookings((long) hotelBookings.size());

        // Tour bookings
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
        res.setTotalRestaurantBookings((long) restaurantBookings.size());

        Map<String, Long> restByStatus = restaurantBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));
        res.setRestaurantBookingsByStatus(restByStatus);

        // Recent 30 days stats
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        res.setNewUsersLast30Days(allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(thirtyDaysAgo))
                .count());
        res.setNewTourBookingsLast30Days(tourBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(thirtyDaysAgo))
                .count());
        res.setNewRestaurantBookingsLast30Days(restaurantBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(thirtyDaysAgo))
                .count());
        res.setNewHotelBookingsLast30Days((long) hotelBookings.size()); // no createdAt on BookedRoom

        // Dashboard extra fields
        Instant startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfPrevMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfThisMonth = startOfMonth;

        res.setMonthlyRevenue(tourBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED
                        && b.getCreatedAt() != null && !b.getCreatedAt().isBefore(startOfThisMonth))
                .map(TourBooking::getFinalPrice).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).longValue());

        res.setPreviousMonthRevenue(tourBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED
                        && b.getCreatedAt() != null
                        && !b.getCreatedAt().isBefore(startOfPrevMonth)
                        && b.getCreatedAt().isBefore(startOfThisMonth))
                .map(TourBooking::getFinalPrice).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).longValue());

        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long todayTour = tourBookings.stream()
                .filter(b -> b.getCreatedAt() != null && !b.getCreatedAt().isBefore(startOfToday)).count();
        long todayRest = restaurantBookings.stream()
                .filter(b -> b.getCreatedAt() != null && !b.getCreatedAt().isBefore(startOfToday)).count();
        res.setTodayTourOrders(todayTour);
        res.setTodayRestaurantOrders(todayRest);
        res.setTodayHotelOrders(0L);
        res.setTodayOrders(todayTour + todayRest);

        res.setNewUsersThisMonth(allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(startOfThisMonth))
                .count());
        res.setPreviousMonthUsers(allUsers.stream()
                .filter(u -> u.getCreatedAt() != null
                        && !u.getCreatedAt().isBefore(startOfPrevMonth)
                        && u.getCreatedAt().isBefore(startOfThisMonth))
                .count());

        res.setActiveTours(allTours.stream()
                .filter(t -> t.getStatus() != null && "ACTIVE".equals(t.getStatus().name())).count());
        res.setTotalDepartures(departureRepository.count());
        res.setOpenIncidentsCount(incidentReportRepository
                .findByStatus(com.example.travelManager.domain.IncidentReport.IncidentStatus.OPEN).size());

        return ResponseEntity.ok(res);
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<Map<String, Object>>> revenue(@RequestParam(name = "year", defaultValue = "0") int year) {
        int targetYear = year > 0 ? year : LocalDate.now().getYear();
        List<TourBooking> all = tourBookingRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            final int month = m;
            LocalDate start = LocalDate.of(targetYear, month, 1);
            LocalDate end = start.plusMonths(1);
            Instant from = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant to = end.atStartOfDay(ZoneId.systemDefault()).toInstant();

            BigDecimal rev = all.stream()
                    .filter(b -> b.getStatus() != BookingStatus.CANCELLED
                            && b.getCreatedAt() != null
                            && !b.getCreatedAt().isBefore(from) && b.getCreatedAt().isBefore(to))
                    .map(TourBooking::getFinalPrice).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long orders = all.stream()
                    .filter(b -> b.getCreatedAt() != null
                            && !b.getCreatedAt().isBefore(from) && b.getCreatedAt().isBefore(to))
                    .count();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", "T" + month);
            row.put("revenue", rev.longValue());
            row.put("orders", orders);
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/top-tours")
    public ResponseEntity<List<Map<String, Object>>> topTours() {
        List<TourBooking> all = tourBookingRepository.findAll();
        Map<Long, List<TourBooking>> byTour = all.stream()
                .filter(b -> b.getTour() != null)
                .collect(Collectors.groupingBy(b -> b.getTour().getId()));

        List<Map<String, Object>> result = byTour.entrySet().stream()
                .map(e -> {
                    TourBooking first = e.getValue().get(0);
                    BigDecimal rev = e.getValue().stream()
                            .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                            .map(TourBooking::getFinalPrice).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", e.getKey());
                    row.put("name", first.getTour().getName());
                    row.put("bookings", e.getValue().size());
                    row.put("revenue", rev.longValue());
                    return row;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("bookings"), (int) a.get("bookings")))
                .limit(5)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
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
        // Dashboard extra fields
        private long monthlyRevenue;
        private long previousMonthRevenue;
        private long todayOrders;
        private long todayTourOrders;
        private long todayHotelOrders;
        private long todayRestaurantOrders;
        private long newUsersThisMonth;
        private long previousMonthUsers;
        private long totalDepartures;
        private long openIncidentsCount;
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
