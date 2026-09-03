package com.example.travelManager.domain.response.place;

import lombok.Data;

import java.util.List;

@Data
public class PlaceCandidateDto {
    private String placeId;
    private String displayName;
    private String formattedAddress;
    private String city;
    private Double latitude;
    private Double longitude;
    private List<PlacePhotoDto> photos;
    private Double rating;
    private Integer userRatingCount;
    private String editorialSummary;
}
