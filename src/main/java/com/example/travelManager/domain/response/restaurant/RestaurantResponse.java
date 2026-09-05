package com.example.travelManager.domain.response.restaurant;

import com.example.travelManager.util.constant.restaurant.CuisineType;
import com.example.travelManager.util.constant.restaurant.PriceRange;
import lombok.Data;

@Data
public class RestaurantResponse {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private CuisineType cuisineType;
    private PriceRange priceRange;
    private Integer capacity;
    private String openingHours;
    private String amenities;
    private boolean isActive;
    private Double averageRating;
    private byte[] photo;

    /** Toạ độ để ghim bản đồ ở trang chi tiết. Null khi chưa tra được. */
    private Double latitude;
    private Double longitude;

    /**
     * Giá trung bình mỗi người. Backend vẫn dùng nó để tính packageRestaurantPrice
     * khi đặt tour trọn gói, nhưng trước giờ không trả ra nên FE chỉ hiện được
     * priceRange chung chung, khách không biết bữa ăn tốn khoảng bao nhiêu.
     */
    private java.math.BigDecimal pricePerPerson;

    /**
     * Thực đơn — CHỈ điền ở endpoint chi tiết, để null ở danh sách.
     * Trả kèm món cho cả 44 nhà hàng trong một response danh sách là vô ích và nặng.
     */
    private java.util.List<MenuItemResponse> menuItems;
}
