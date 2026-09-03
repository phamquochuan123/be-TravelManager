package com.example.travelManager.domain.response.place;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlacePhotoDto {
    // Định dạng "places/{placeId}/photos/{photoReference}" — dùng làm tham số "name" khi gọi /admin/places/photo
    private String name;
    private int widthPx;
    private int heightPx;
}
