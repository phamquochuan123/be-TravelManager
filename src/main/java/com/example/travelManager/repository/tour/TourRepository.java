package com.example.travelManager.repository.tour;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.util.constant.tour.TourStatus;

public interface TourRepository extends JpaRepository<Tour, Long>, JpaSpecificationExecutor<Tour> {

    List<Tour> findAllByDeletedFalseAndStatus(TourStatus status);

    List<Tour> findAllByDeletedFalse();
}
