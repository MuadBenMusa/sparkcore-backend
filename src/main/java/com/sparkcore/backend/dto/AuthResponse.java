package com.sparkcore.backend.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken) {
}