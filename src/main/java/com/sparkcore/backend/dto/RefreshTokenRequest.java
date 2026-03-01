package com.sparkcore.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh Token darf nicht leer sein") String refreshToken) {
}
