package com.example.travelManager.repository.hotel;

import com.example.travelManager.domain.Hotel;
import com.example.travelManager.domain.HotelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByIsActiveTrue();
    List<Hotel> findByCityIgnoreCaseAndIsActiveTrue(String city);
    List<Hotel> findByHotelTypeAndIsActiveTrue(HotelType hotelType);
    List<Hotel> findByStarRatingAndIsActiveTrue(int starRating);
}
