package com.example.travelManager.domain.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private String userId;
    private String name;
    private String email;
    private Boolean isAccountVerified;
    private String roleName;
    private String phone;
    private LocalDate birthDate;
    private String gender;
    private String avatar;
    private Boolean isActive;
    private String lockReason;

}
