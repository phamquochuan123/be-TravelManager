package com.example.travelManager.domain.response.hotel;

import com.example.travelManager.util.constant.hotel.HotelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponse {

    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private int starRating;
    private HotelType hotelType;
    private String amenities;
    private boolean isActive;
    private int totalRooms;
    private byte[] photo;

    /**
     * Toạ độ để trang chi tiết ghim đúng vị trí khách sạn lên bản đồ. Cột đã có
     * sẵn trong bảng hotels từ trước nhưng chưa bao giờ được trả ra, nên FE không
     * có gì để vẽ và phần "Vị trí" chỉ là một ô xám ghi địa chỉ.
     * Null khi khách sạn chưa được gán toạ độ — FE phải xử lý được trường hợp đó.
     */
    private Double latitude;
    private Double longitude;
}
