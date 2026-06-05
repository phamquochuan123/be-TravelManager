package com.example.travelManager.repository.hotel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelManager.domain.hotel.HotelFavorite;

public interface HotelFavoriteRepository extends JpaRepository<HotelFavorite, Long> {
    boolean existsByUserIdAndHotelId(Long userId, Long hotelId);
    Optional<HotelFavorite> findByUserIdAndHotelId(Long userId, Long hotelId);
    List<HotelFavorite> findByUserId(Long userId);
}
