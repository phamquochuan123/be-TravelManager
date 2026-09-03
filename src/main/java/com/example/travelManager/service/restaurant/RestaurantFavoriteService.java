package com.example.travelManager.service.restaurant;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.restaurant.RestaurantFavorite;
import com.example.travelManager.repository.restaurant.RestaurantFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RestaurantFavoriteService {

    private final RestaurantFavoriteRepository favoriteRepository;

    public boolean toggle(UserEntity user, Restaurant restaurant) {
        Optional<RestaurantFavorite> existing =
                favoriteRepository.findByUserIdAndRestaurantId(user.getId(), restaurant.getId());
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }
        RestaurantFavorite fav = new RestaurantFavorite();
        fav.setUser(user);
        fav.setRestaurant(restaurant);
        favoriteRepository.save(fav);
        return true;
    }

    public boolean isFavorited(Long userId, Long restaurantId) {
        return favoriteRepository.existsByUserIdAndRestaurantId(userId, restaurantId);
    }

    public List<Long> getFavoriteRestaurantIds(Long userId) {
        return favoriteRepository.findByUserId(userId).stream().map(f -> f.getRestaurant().getId()).toList();
    }
}
