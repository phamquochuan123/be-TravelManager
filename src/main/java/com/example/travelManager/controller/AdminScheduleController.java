package com.example.travelManager.controller;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.util.InputValidator;
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
    private final com.example.travelManager.service.tour.TourDepartureScheduler departureScheduler;

    /**
     * Chạy ngay 2 job lịch khởi hành thay vì chờ tới lượt định kỳ (trạng thái mỗi
     * giờ, sinh lịch 02:30 hằng ngày). Dùng sau khi vừa sửa cấu hình lặp bằng SQL,
     * hoặc để demo mà không phải đợi. Đã nằm dưới /admin/** nên chỉ ADMIN gọi được.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh() {
        int soChuyenDoiTrangThai = departureScheduler.capNhatTrangThaiChuyen();
        int soChuyenMoi = departureScheduler.sinhLichKhoiHanhTheoMau();
        return ResponseEntity.ok(Map.of(
                "soChuyenDoiTrangThai", soChuyenDoiTrangThai,
                "soChuyenMoiSinhRa", soChuyenMoi));
    }

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
        d.setDepartureDate(docNgayKhoiHanh(req.getDepartureDate(), true));
        int slots = req.getMaxSlots() > 0 ? req.getMaxSlots() : tour.getMaxSlots();
        InputValidator.trongKhoang(slots, 1, 100_000, "Số chỗ");
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
            // Cho phép sửa chuyến đã qua (admin còn phải chỉnh lịch sử), chỉ chặn
            // ngày quá khứ khi TẠO MỚI.
            d.setDepartureDate(docNgayKhoiHanh(req.getDepartureDate(), false));
        }
        if (req.getMaxSlots() > 0) {
            InputValidator.trongKhoang(req.getMaxSlots(), 1, 100_000, "Số chỗ");
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

    /**
     * Đọc ngày khởi hành. LocalDate.parse trần sẽ ném DateTimeParseException và
     * rơi xuống handler cuối cùng thành lỗi 500 — admin gõ sai định dạng không
     * đáng nhận lỗi hệ thống.
     */
    private LocalDate docNgayKhoiHanh(String giaTri, boolean chanNgayQuaKhu) {
        if (giaTri == null || giaTri.isBlank()) {
            throw new IllegalArgumentException("Ngày khởi hành không được để trống");
        }
        LocalDate ngay;
        try {
            ngay = LocalDate.parse(giaTri.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Ngày khởi hành phải theo định dạng yyyy-MM-dd");
        }
        if (chanNgayQuaKhu && ngay.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Ngày khởi hành (" + ngay + ") không được nằm trong quá khứ");
        }
        return ngay;
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

