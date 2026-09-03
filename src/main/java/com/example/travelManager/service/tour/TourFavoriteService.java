package com.example.travelManager.service.tour;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourFavorite;
import com.example.travelManager.repository.tour.TourFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TourFavoriteService {

    private final TourFavoriteRepository favoriteRepository;

    public boolean toggle(UserEntity user, Tour tour) {
        Optional<TourFavorite> existing = favoriteRepository.findByUserIdAndTourId(user.getId(), tour.getId());
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }
        TourFavorite fav = new TourFavorite();
        fav.setUser(user);
        fav.setTour(tour);
        favoriteRepository.save(fav);
        return true;
    }

    public boolean isFavorited(Long userId, Long tourId) {
        return favoriteRepository.existsByUserIdAndTourId(userId, tourId);
    }

    public List<Long> getFavoriteTourIds(Long userId) {
        return favoriteRepository.findByUserId(userId).stream().map(f -> f.getTour().getId()).toList();
    }
}
