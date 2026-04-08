package com.example.travelManager.repository.tour;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelManager.domain.tour.TourDeparture;

public interface TourDepartureRepository extends JpaRepository<TourDeparture, Long> {

    List<TourDeparture> findByTourIdAndDepartureDateAfter(Long tourId, LocalDate date);

    List<TourDeparture> findByTourIdAndAvailableSlotsGreaterThan(Long tourId, int minSlots);
}
