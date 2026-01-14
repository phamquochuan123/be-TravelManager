package com.example.travelManager.domain.io;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {

    @NotBlank(message = "Name should not be empty")
    private String name;

    @Email(message = "Enter a valid email address")
    @NotNull(message = "Email should not be empty")
    private String email;

    @Size(min = 6, message = "Password should be at least 6 characters")
    private String passWord;

}
