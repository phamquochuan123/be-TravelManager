package com.example.travelManager.service.hotel;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.hotel.HotelFavorite;
import com.example.travelManager.repository.hotel.HotelFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HotelFavoriteService {

    private final HotelFavoriteRepository favoriteRepository;

    public boolean toggle(UserEntity user, Hotel hotel) {
        Optional<HotelFavorite> existing = favoriteRepository.findByUserIdAndHotelId(user.getId(), hotel.getId());
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }
        HotelFavorite fav = new HotelFavorite();
        fav.setUser(user);
        fav.setHotel(hotel);
        favoriteRepository.save(fav);
        return true;
    }

    public boolean isFavorited(Long userId, Long hotelId) {
        return favoriteRepository.existsByUserIdAndHotelId(userId, hotelId);
    }

    public List<Long> getFavoriteHotelIds(Long userId) {
        return favoriteRepository.findByUserId(userId).stream().map(f -> f.getHotel().getId()).toList();
    }
}
