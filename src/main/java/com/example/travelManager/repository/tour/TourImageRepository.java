package com.example.travelManager.repository.tour;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelManager.domain.tour.TourImage;

public interface TourImageRepository extends JpaRepository<TourImage, Long> {

    List<TourImage> findByTourIdOrderBySortOrderAsc(Long tourId);

    List<TourImage> findByTourIdInOrderByTourIdAscSortOrderAsc(List<Long> tourIds);
}
