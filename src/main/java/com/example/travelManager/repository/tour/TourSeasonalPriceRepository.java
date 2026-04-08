package com.example.travelManager.repository.tour;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.travelManager.domain.tour.TourSeasonalPrice;

public interface TourSeasonalPriceRepository extends JpaRepository<TourSeasonalPrice, Long> {

    List<TourSeasonalPrice> findByTourId(Long tourId);

    @Query("SELECT s FROM TourSeasonalPrice s WHERE s.tour.id = :tourId " +
           "AND s.startDate <= :date AND s.endDate >= :date")
    List<TourSeasonalPrice> findActiveByTourIdAndDate(@Param("tourId") Long tourId,
                                                       @Param("date") LocalDate date);
}
