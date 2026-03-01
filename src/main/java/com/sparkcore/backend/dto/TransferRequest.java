package com.sparkcore.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import com.sparkcore.backend.validation.ValidIban;

import java.math.BigDecimal;

public record TransferRequest(
                @NotBlank String fromIban,
                @NotBlank @ValidIban String toIban,
                @Positive BigDecimal amount) {
}
