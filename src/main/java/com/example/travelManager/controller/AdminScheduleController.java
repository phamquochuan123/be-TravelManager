package com.example.travelManager.controller;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.util.constant.tour.TourDepartureStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/schedules")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final TourDepartureRepository departureRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourBookingRepository bookingRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "tourId", required = false) Long tourId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("departureDate").ascending());
        Page<TourDeparture> departurePage;
        if (tourId != null) {
            departurePage = departureRepository.findByTourId(tourId, pageable);
        } else {
            departurePage = departureRepository.findAll(pageable);
        }
        List<ScheduleResponse> content = departurePage.getContent().stream()
                .map(d -> toResponse(d)).toList();
        return ResponseEntity.ok(Map.of(
                "content", content,
                "totalPages", departurePage.getTotalPages(),
                "totalElements", departurePage.getTotalElements()
        ));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(@RequestBody ScheduleRequest req) {
        var tour = tourRepository.findById(req.getTourId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found"));
        TourDeparture d = new TourDeparture();
        d.setTour(tour);
        d.setDepartureDate(LocalDate.parse(req.getDepartureDate()));
        int slots = req.getMaxSlots() > 0 ? req.getMaxSlots() : tour.getMaxSlots();
        d.setMaxSlots(slots);
        d.setAvailableSlots(slots);
        d.setStatus(parseStatus(req.getStatus()));
        if (req.getStaffId() != null) {
            userRepository.findById(req.getStaffId()).ifPresent(d::setStaff);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(departureRepository.save(d)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> update(@PathVariable("id") Long id, @RequestBody ScheduleRequest req) {
        TourDeparture d = departureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        if (req.getDepartureDate() != null) {
            d.setDepartureDate(LocalDate.parse(req.getDepartureDate()));
        }
        if (req.getMaxSlots() > 0) {
            int bookedCount = bookingRepository.findByDepartureId(id).size();
            d.setMaxSlots(req.getMaxSlots());
            d.setAvailableSlots(Math.max(0, req.getMaxSlots() - bookedCount));
        }
        if (req.getStatus() != null) {
            d.setStatus(parseStatus(req.getStatus()));
        }
        if (req.getStaffId() != null) {
            userRepository.findById(req.getStaffId()).ifPresent(d::setStaff);
        } else if (req.getStaffId() == null && req.isUnassignStaff()) {
            d.setStaff(null);
        }
        return ResponseEntity.ok(toResponse(departureRepository.save(d)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        TourDeparture d = departureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        int bookedCount = bookingRepository.findByDepartureId(id).size();
        if (bookedCount > 0) {
            throw new IllegalStateException("Không thể xóa lịch đã có đặt chỗ");
        }
        departureRepository.delete(d);
        return ResponseEntity.noContent().build();
    }

    private ScheduleResponse toResponse(TourDeparture d) {
        ScheduleResponse r = new ScheduleResponse();
        r.setId(d.getId());
        r.setTourId(d.getTour() != null ? d.getTour().getId() : null);
        r.setDepartureDate(d.getDepartureDate() != null ? d.getDepartureDate().toString() : null);
        r.setMaxSlots(d.getMaxSlots());
        int bookedCount = bookingRepository.findByDepartureId(d.getId()).size();
        r.setBookedSlots(bookedCount);
        r.setStatus(d.getStatus() != null ? d.getStatus().name() : TourDepartureStatus.SCHEDULED.name());
        UserEntity staff = d.getStaff();
        if (staff != null) {
            r.setStaffId(staff.getId());
            r.setStaffName(staff.getName());
            if (staff.getAvatar() != null) {
                r.setStaffAvatar("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(staff.getAvatar()));
            }
        }
        return r;
    }

    private TourDepartureStatus parseStatus(String s) {
        if (s == null) return TourDepartureStatus.SCHEDULED;
        try { return TourDepartureStatus.valueOf(s); }
        catch (IllegalArgumentException e) { return TourDepartureStatus.SCHEDULED; }
    }

    @Data
    public static class ScheduleRequest {
        private Long tourId;
        private String departureDate;
        private int maxSlots;
        private Long staffId;
        private String status;
        private boolean unassignStaff;
    }

    @Data
    public static class ScheduleResponse {
        private Long id;
        private Long tourId;
        private String departureDate;
        private int maxSlots;
        private int bookedSlots;
        private Long staffId;
        private String staffName;
        private String staffAvatar;
        private String status;
    }
}

