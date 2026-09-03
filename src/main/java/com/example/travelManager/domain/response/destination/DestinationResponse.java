package com.example.travelManager.domain.response.destination;

import com.example.travelManager.util.constant.destination.DestinationType;
import lombok.Data;

@Data
public class DestinationResponse {
    private Long id;
    private String name;
    private String description;
    private DestinationType destinationType;
    private String province;
    private String city;
    private String address;
    private Double latitude;
    private Double longitude;
    private boolean isActive;
    private byte[] photo;
}
