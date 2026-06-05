package com.example.travelManager.repository.tour;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelManager.domain.tour.TourFavorite;

public interface TourFavoriteRepository extends JpaRepository<TourFavorite, Long> {
    boolean existsByUserIdAndTourId(Long userId, Long tourId);
    Optional<TourFavorite> findByUserIdAndTourId(Long userId, Long tourId);
    List<TourFavorite> findByUserId(Long userId);
}
