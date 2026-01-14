package com.example.travelManager.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class AuthResponse {
    private String email;
    private String token;
}
