package com.example.travelManager.repository.restaurant;

import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.util.constant.restaurant.CuisineType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /**
     * Khoá bi quan khi đặt bàn: kiểm tra sức chứa rồi mới ghi mà không khoá thì
     * 2 request đồng thời cùng khung giờ đều thấy "còn chỗ" → nhận quá sức chứa.
     * Cùng mẫu với RoomRepository.findByIdForUpdate.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Restaurant r WHERE r.id = :id")
    Optional<Restaurant> findByIdForUpdate(@Param("id") Long id);

    List<Restaurant> findByIsActiveTrue();
    List<Restaurant> findByCityIgnoreCaseAndIsActiveTrue(String city);
    List<Restaurant> findByCuisineTypeAndIsActiveTrue(CuisineType cuisineType);
}
