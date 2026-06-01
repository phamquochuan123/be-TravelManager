package com.example.travelManager.controller;

import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.util.constant.tour.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminReportsController {

    private final TourBookingRepository tourBookingRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;
    private final BookedRoomRepository hotelBookingRepository;

    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> reports(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {

        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusDays(29);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();
        Instant fromInst = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toInst = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<TourBooking> allTourBookings = tourBookingRepository.findAll();
        List<TourBooking> tourBookings = allTourBookings.stream()
                .filter(b -> b.getCreatedAt() != null
                        && !b.getCreatedAt().isBefore(fromInst)
                        && b.getCreatedAt().isBefore(toInst))
                .collect(Collectors.toList());

        List<RestaurantBooking> restaurantBookings = restaurantBookingRepository.findAll().stream()
                .filter(b -> b.getCreatedAt() != null
                        && !b.getCreatedAt().isBefore(fromInst)
                        && b.getCreatedAt().isBefore(toInst))
                .collect(Collectors.toList());

        // Previous period for growth
        long periodDays = fromDate.until(toDate.plusDays(1), java.time.temporal.ChronoUnit.DAYS);
        Instant prevFrom = fromInst.minus(periodDays, java.time.temporal.ChronoUnit.DAYS);
        Instant prevTo = fromInst;
        List<TourBooking> prevTourBookings = allTourBookings.stream()
                .filter(b -> b.getCreatedAt() != null
                        && !b.getCreatedAt().isBefore(prevFrom)
                        && b.getCreatedAt().isBefore(prevTo))
                .collect(Collectors.toList());

        // KPI
        BigDecimal totalRevenue = tourBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(TourBooking::getFinalPrice).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevRevenue = prevTourBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(TourBooking::getFinalPrice).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = tourBookings.size() + restaurantBookings.size();
        long prevOrders = prevTourBookings.size();

        Set<String> customers = tourBookings.stream()
                .map(TourBooking::getContactEmail).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> prevCustomers = prevTourBookings.stream()
                .map(TourBooking::getContactEmail).filter(Objects::nonNull).collect(Collectors.toSet());

        double revenueGrowth = prevRevenue.compareTo(BigDecimal.ZERO) > 0
                ? (totalRevenue.subtract(prevRevenue)).divide(prevRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0;
        double ordersGrowth = prevOrders > 0 ? (double)(totalOrders - prevOrders) / prevOrders * 100 : 0;
        double customersGrowth = prevCustomers.size() > 0
                ? (double)(customers.size() - prevCustomers.size()) / prevCustomers.size() * 100 : 0;
        double avgOrder = totalOrders > 0 ? totalRevenue.doubleValue() / totalOrders : 0;
        double prevAvg = prevOrders > 0 ? prevRevenue.doubleValue() / prevOrders : 0;
        double avgGrowth = prevAvg > 0 ? (avgOrder - prevAvg) / prevAvg * 100 : 0;

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalRevenue", totalRevenue.longValue());
        kpi.put("totalOrders", totalOrders);
        kpi.put("totalCustomers", customers.size());
        kpi.put("avgOrderValue", (long) avgOrder);
        kpi.put("revenueGrowth", Math.round(revenueGrowth * 10.0) / 10.0);
        kpi.put("ordersGrowth", Math.round(ordersGrowth * 10.0) / 10.0);
        kpi.put("customersGrowth", Math.round(customersGrowth * 10.0) / 10.0);
        kpi.put("avgOrderGrowth", Math.round(avgGrowth * 10.0) / 10.0);

        // Revenue timeline (daily)
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (LocalDate d = fromDate; !d.isAfter(toDate); d = d.plusDays(1)) {
            final LocalDate day = d;
            Instant dayStart = day.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            long tourRev = tourBookings.stream()
                    .filter(b -> b.getStatus() != BookingStatus.CANCELLED
                            && b.getCreatedAt() != null
                            && !b.getCreatedAt().isBefore(dayStart) && b.getCreatedAt().isBefore(dayEnd))
                    .map(TourBooking::getFinalPrice).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).longValue();

            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("date", day.toString());
            pt.put("tours", tourRev);
            pt.put("hotels", 0);
            pt.put("restaurants", 0);
            timeline.add(pt);
        }

        // Service breakdown (monthly if range > 30 days, else daily)
        Map<String, Long> byMonth = tourBookings.stream()
                .filter(b -> b.getCreatedAt() != null)
                .collect(Collectors.groupingBy(b -> {
                    LocalDate d = b.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                    return d.getYear() + "-" + String.format("%02d", d.getMonthValue());
                }, Collectors.counting()));

        List<Map<String, Object>> serviceBreakdown = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("tours", e.getValue());
                    m.put("hotels", 0);
                    m.put("restaurants", restaurantBookings.stream()
                            .filter(b -> b.getCreatedAt() != null)
                            .filter(b -> {
                                LocalDate d = b.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                                return (d.getYear() + "-" + String.format("%02d", d.getMonthValue())).equals(e.getKey());
                            }).count());
                    return m;
                }).collect(Collectors.toList());

        // Service pie
        long tourCount = tourBookings.size();
        long restCount = restaurantBookings.size();
        long hotelCount = hotelBookingRepository.count();
        List<Map<String, Object>> servicePie = List.of(
                Map.of("name", "Tour", "value", tourCount),
                Map.of("name", "Khách sạn", "value", hotelCount),
                Map.of("name", "Nhà hàng", "value", restCount)
        );

        // Top tours
        Map<Long, List<TourBooking>> byTour = tourBookings.stream()
                .filter(b -> b.getTour() != null)
                .collect(Collectors.groupingBy(b -> b.getTour().getId()));
        List<Map<String, Object>> topTours = byTour.entrySet().stream()
                .map(e -> {
                    TourBooking fb = e.getValue().get(0);
                    BigDecimal rev = e.getValue().stream()
                            .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                            .map(TourBooking::getFinalPrice).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getKey());
                    m.put("name", fb.getTour().getName());
                    m.put("bookings", e.getValue().size());
                    m.put("revenue", rev.longValue());
                    return m;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("bookings"), (int) a.get("bookings")))
                .limit(5).collect(Collectors.toList());

        // Top customers
        Map<String, List<TourBooking>> byCustomer = tourBookings.stream()
                .filter(b -> b.getContactEmail() != null)
                .collect(Collectors.groupingBy(TourBooking::getContactEmail));
        List<Map<String, Object>> topCustomers = byCustomer.entrySet().stream()
                .map(e -> {
                    TourBooking fb = e.getValue().get(0);
                    BigDecimal spent = e.getValue().stream()
                            .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                            .map(TourBooking::getFinalPrice).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getKey().hashCode());
                    m.put("name", fb.getContactName() != null ? fb.getContactName() : e.getKey());
                    m.put("bookings", e.getValue().size());
                    m.put("totalSpent", spent.longValue());
                    return m;
                })
                .sorted((a, b) -> Long.compare((long) b.get("totalSpent"), (long) a.get("totalSpent")))
                .limit(5).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpi", kpi);
        result.put("revenueTimeline", timeline);
        result.put("serviceBreakdown", serviceBreakdown);
        result.put("servicePie", servicePie);
        result.put("topTours", topTours);
        result.put("topCustomers", topCustomers);
        return ResponseEntity.ok(result);
    }
}
