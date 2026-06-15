package com.edap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String username;

    @Email
    @Size(max = 200)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    private Set<@Pattern(regexp = "ROLE_ADMIN|ROLE_ENGINEER|ROLE_VIEWER") String> roles;
}
