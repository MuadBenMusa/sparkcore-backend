package com.sparkcore.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                title = "SparkCore Banking API",
                description = "Core Banking Backend System - Enterprise Edition. Gebaut für Finanz Informatik Standards.",
                version = "1.0",
                contact = @Contact(
                        name = "Max Mustermann",
                        email = "maxmustermann@example.com"
                )
        ),
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Bitte füge hier dein JWT Token ein (ohne das Wort 'Bearer')",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    // Diese Klasse bleibt leer! Die Annotationen darüber machen die ganze Arbeit.
}