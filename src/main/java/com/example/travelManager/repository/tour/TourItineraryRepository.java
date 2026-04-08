package com.example.travelManager.repository.tour;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelManager.domain.tour.TourItinerary;

public interface TourItineraryRepository extends JpaRepository<TourItinerary, Long> {

    List<TourItinerary> findByTourIdOrderByDayNumberAsc(Long tourId);
}
