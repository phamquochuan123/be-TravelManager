package com.example.travelManager.repository.tour;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.travelManager.domain.tour.TourDeparture;

public interface TourDepartureRepository extends JpaRepository<TourDeparture, Long> {

    List<TourDeparture> findByTourIdAndDepartureDateAfter(Long tourId, LocalDate date);

    List<TourDeparture> findByTourIdAndAvailableSlotsGreaterThan(Long tourId, int minSlots);

    List<TourDeparture> findByStaffEmailOrderByDepartureDateAsc(String staffEmail);

    List<TourDeparture> findByStaffId(Long staffId);

    Page<TourDeparture> findByTourId(Long tourId, Pageable pageable);

    List<TourDeparture> findByTourIdOrderByDepartureDateAsc(Long tourId);

    @Modifying
    @Query("UPDATE TourDeparture d SET d.availableSlots = d.availableSlots - :needed " +
           "WHERE d.id = :id AND d.availableSlots >= :needed")
    int decrementSlot(@Param("id") Long id, @Param("needed") int needed);

    @Modifying
    @Query("UPDATE TourDeparture d SET d.availableSlots = d.availableSlots + :returned " +
           "WHERE d.id = :id")
    int incrementSlot(@Param("id") Long id, @Param("returned") int returned);
}
