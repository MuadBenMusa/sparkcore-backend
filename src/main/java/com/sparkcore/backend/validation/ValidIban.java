package com.sparkcore.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = IbanValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIban {
    String message() default "Ungültige IBAN. Die Prüfsumme stimmt nicht mit dem ISO 13616 Standard überein.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
