package com.example.travelManager.controller;

import com.example.travelManager.domain.IncidentReport;
import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.IncidentReportRepository;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.service.EmailService;
import com.example.travelManager.util.SecurityUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/incident-reports")
@RequiredArgsConstructor
public class IncidentReportController {

    private final IncidentReportRepository reportRepository;
    private final TourDepartureRepository departureRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * POST /incident-reports
     * Staff tạo báo cáo sự cố cho departure được phân công.
     */
    @PostMapping
    public ResponseEntity<ReportResponse> create(@Valid @RequestBody CreateReportRequest request) {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        UserEntity reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TourDeparture departure = departureRepository.findById(request.getDepartureId())
                .orElseThrow(() -> new ResourceNotFoundException("Departure not found: " + request.getDepartureId()));

        if (departure.getStaff() == null || !departure.getStaff().getEmail().equalsIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        IncidentReport report = new IncidentReport();
        report.setDeparture(departure);
        report.setReporter(reporter);
        report.setIncidentType(request.getIncidentType());
        report.setDescription(request.getDescription());

        IncidentReport saved = reportRepository.save(report);

        // Thông báo Admin qua email
        try {
            String tourName = departure.getTour() != null ? departure.getTour().getName() : "N/A";
            String depDate = departure.getDepartureDate() != null ? departure.getDepartureDate().toString() : "N/A";
            emailService.sendIncidentReportNotification(
                    reporter.getName(),
                    tourName,
                    depDate,
                    request.getIncidentType().name(),
                    request.getDescription());
        } catch (Exception ignored) {}

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /**
     * GET /incident-reports/my — Staff xem báo cáo của mình.
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReportResponse>> myReports() {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        return ResponseEntity.ok(
                reportRepository.findByReporterEmailOrderByCreatedAtDesc(email)
                        .stream().map(this::toResponse).toList());
    }

    /**
     * GET /admin/incident-reports — Admin xem tất cả báo cáo.
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<ReportResponse>> allReports() {
        return ResponseEntity.ok(
                reportRepository.findAllByOrderByCreatedAtDesc()
                        .stream().map(this::toResponse).toList());
    }

    /**
     * PATCH /incident-reports/{id}/status — Admin cập nhật trạng thái và ghi chú.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ReportResponse> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        IncidentReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
        if (body.containsKey("status")) {
            report.setStatus(IncidentReport.IncidentStatus.valueOf(body.get("status")));
        }
        if (body.containsKey("adminNote")) {
            report.setAdminNote(body.get("adminNote"));
        }
        return ResponseEntity.ok(toResponse(reportRepository.save(report)));
    }

    private ReportResponse toResponse(IncidentReport r) {
        ReportResponse res = new ReportResponse();
        res.setId(r.getId());
        res.setDepartureId(r.getDeparture() != null ? r.getDeparture().getId() : null);
        res.setTourName(r.getDeparture() != null && r.getDeparture().getTour() != null
                ? r.getDeparture().getTour().getName() : "");
        res.setDepartureDate(r.getDeparture() != null
                ? (r.getDeparture().getDepartureDate() != null ? r.getDeparture().getDepartureDate().toString() : "")
                : "");
        res.setReporterName(r.getReporter() != null ? r.getReporter().getName() : "");
        res.setReporterEmail(r.getReporter() != null ? r.getReporter().getEmail() : "");
        res.setIncidentType(r.getIncidentType() != null ? r.getIncidentType().name() : "");
        res.setDescription(r.getDescription());
        res.setStatus(r.getStatus() != null ? r.getStatus().name() : "");
        res.setAdminNote(r.getAdminNote());
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }

    @Data
    public static class CreateReportRequest {
        @NotNull
        private Long departureId;
        @NotNull
        private IncidentReport.IncidentType incidentType;
        @NotBlank
        @Size(min = 20, message = "Mô tả phải ít nhất 20 ký tự")
        private String description;
    }

    @Data
    public static class ReportResponse {
        private Long id;
        private Long departureId;
        private String tourName;
        private String departureDate;
        private String reporterName;
        private String reporterEmail;
        private String incidentType;
        private String description;
        private String status;
        private String adminNote;
        private Instant createdAt;
    }
}

