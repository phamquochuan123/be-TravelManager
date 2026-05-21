package com.example.travelManager.domain.request.destination;

import com.example.travelManager.util.constant.destination.DestinationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DestinationRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private DestinationType destinationType;

    private String province;

    @NotBlank
    private String city;

    private Boolean isActive;
}
