package com.example.travelManager.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRoleRequest {
    @NotNull(message = "roleId không được để trống")
    private Long roleId;
}
