package com.example.travelManager.repository.restaurant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelManager.domain.restaurant.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /** Thực đơn của một nhà hàng, chỉ các món còn phục vụ, theo đúng thứ tự hiển thị. */
    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderBySortOrderAscIdAsc(Long restaurantId);

    List<MenuItem> findByRestaurantIdOrderBySortOrderAscIdAsc(Long restaurantId);
}
