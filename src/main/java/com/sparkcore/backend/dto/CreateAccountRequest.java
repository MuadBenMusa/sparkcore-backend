package com.sparkcore.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "Der Name darf nicht leer sein")
        String ownerName,

        @NotBlank(message = "Die IBAN darf nicht leer sein")
        String iban,

        @Positive(message = "Das Startguthaben muss größer als 0 sein")
        BigDecimal initialBalance
) {
}