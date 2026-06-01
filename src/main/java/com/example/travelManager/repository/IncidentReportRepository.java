package com.example.travelManager.repository;

import com.example.travelManager.domain.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    List<IncidentReport> findByReporterEmailOrderByCreatedAtDesc(String email);

    List<IncidentReport> findByDepartureIdOrderByCreatedAtDesc(Long departureId);

    List<IncidentReport> findAllByOrderByCreatedAtDesc();

    List<IncidentReport> findByStatus(IncidentReport.IncidentStatus status);
}
