package com.example.travelManager.controller;

import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.constant.tour.TourDepartureStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final TourDepartureRepository departureRepository;
    private final TourBookingRepository bookingRepository;

    /**
     * GET /staff/my-tours
     * Staff xem danh sách lịch khởi hành được phân công cho mình (theo email đăng nhập).
     */
    @GetMapping("/my-tours")
    public ResponseEntity<List<DepartureScheduleResponse>> myTours() {
        String email = SecurityUtil.getCurrentUserLoginOrThrow();
        List<TourDeparture> departures = departureRepository.findByStaffEmailOrderByDepartureDateAsc(email);
        return ResponseEntity.ok(departures.stream().map(this::toDepartureSchedule).toList());
    }

    /**
     * GET /staff/my-tours/{departureId}/passengers
     * Staff xem danh sách hành khách trong chuyến được phân công.
     */
    @GetMapping("/my-tours/{departureId}/passengers")
    public ResponseEntity<List<PassengerResponse>> passengers(@PathVariable("departureId") Long departureId) {
        String email = SecurityUtil.getCurrentUserLoginOrThrow();

        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new ResourceNotFoundException("Departure not found: " + departureId));

        if (departure.getStaff() == null || !departure.getStaff().getEmail().equalsIgnoreCase(email)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Bạn không được phân công cho chuyến này");
        }

        List<TourBooking> bookings = bookingRepository.findByDepartureId(departureId);
        return ResponseEntity.ok(bookings.stream().map(this::toPassenger).toList());
    }

    @PatchMapping("/departures/{id}/status")
    public ResponseEntity<Map<String, String>> updateDepartureStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        TourDeparture departure = departureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departure not found: " + id));
        String newStatus = body.get("status");
        if (newStatus != null) {
            try {
                departure.setStatus(TourDepartureStatus.valueOf(newStatus));
            } catch (IllegalArgumentException ignored) {}
        }
        departureRepository.save(departure);
        return ResponseEntity.ok(Map.of("status", departure.getStatus().name()));
    }

    private DepartureScheduleResponse toDepartureSchedule(TourDeparture d) {
        DepartureScheduleResponse res = new DepartureScheduleResponse();
        res.setDepartureId(d.getId());
        res.setTourId(d.getTour() != null ? d.getTour().getId() : null);
        res.setTourName(d.getTour() != null ? d.getTour().getName() : "");
        res.setDestination(d.getTour() != null ? d.getTour().getDestination() : "");
        res.setDepartureDate(d.getDepartureDate());
        res.setAvailableSlots(d.getAvailableSlots());
        res.setMaxSlots(d.getTour() != null ? d.getTour().getMaxSlots() : 0);
        long bookedCount = bookingRepository.findByDepartureId(d.getId()).size();
        res.setBookedCount((int) bookedCount);
        res.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        return res;
    }

    private PassengerResponse toPassenger(TourBooking b) {
        PassengerResponse res = new PassengerResponse();
        res.setBookingId(b.getId());
        res.setContactName(b.getContactName());
        res.setContactPhone(b.getContactPhone());
        res.setContactEmail(b.getContactEmail());
        res.setNumAdults(b.getNumAdults());
        res.setNumChildren(b.getNumChildren());
        res.setFinalPrice(b.getFinalPrice());
        res.setStatus(b.getStatus() != null ? b.getStatus().name() : "");
        return res;
    }

    @Data
    public static class DepartureScheduleResponse {
        private Long departureId;
        private Long tourId;
        private String tourName;
        private String destination;
        private LocalDate departureDate;
        private int availableSlots;
        private int maxSlots;
        private int bookedCount;
        private String status;
    }

    @Data
    public static class PassengerResponse {
        private Long bookingId;
        private String contactName;
        private String contactPhone;
        private String contactEmail;
        private int numAdults;
        private int numChildren;
        private BigDecimal finalPrice;
        private String status;
    }
}

