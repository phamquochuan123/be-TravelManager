package com.example.travelManager.domain.response.tour;

import lombok.Data;

@Data
public class TourImageResponse {
    private Long id;
    private byte[] photo;
    private int sortOrder;
}
