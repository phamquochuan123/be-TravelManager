package com.example.travelManager.domain.response.restaurant;

import java.math.BigDecimal;

import com.example.travelManager.util.constant.restaurant.MenuCategory;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Một món ăn trả về cho trang chi tiết nhà hàng.
 *
 * Tên trường bám đúng interface MenuItem của RestaurantDetailPage.tsx: FE đọc
 * `isBestSeller` và `isNew`, nên hai trường boolean phải serialize ra đúng hai tên
 * đó — mặc định Jackson sẽ đặt là "bestSeller"/"newItem" và FE không thấy nhãn.
 */
@Data
public class MenuItemResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private MenuCategory category;
    private String photo;

    @JsonProperty("isBestSeller")
    private boolean bestSeller;

    @JsonProperty("isNew")
    private boolean newItem;
}
