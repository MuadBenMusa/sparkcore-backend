package com.sparkcore.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String fromIban,
        @NotBlank String toIban,
        @Positive BigDecimal amount
) {}
