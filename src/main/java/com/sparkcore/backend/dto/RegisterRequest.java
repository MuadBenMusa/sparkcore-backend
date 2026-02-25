package com.sparkcore.backend.dto;

import com.sparkcore.backend.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull Role role
) {}