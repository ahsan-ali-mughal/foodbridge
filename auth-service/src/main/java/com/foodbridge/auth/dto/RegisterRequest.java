package com.foodbridge.auth.dto;

import com.foodbridge.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. {@code role} determines whether the resulting account
 * requires Admin verification before it may claim donations (see {@link com.foodbridge.auth.entity.User}).
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters") String password,
        @NotBlank @Size(max = 120) String displayName,
        @NotNull Role role
) {
}
