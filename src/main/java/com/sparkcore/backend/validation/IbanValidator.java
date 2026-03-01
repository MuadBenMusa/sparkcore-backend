package com.sparkcore.backend.validation;

import com.sparkcore.backend.util.IbanUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IbanValidator implements ConstraintValidator<ValidIban, String> {

    @Override
    public boolean isValid(String ibanField, ConstraintValidatorContext context) {
        if (ibanField == null || ibanField.isBlank()) {
            return false; // Null oder Leer ist keine gültige IBAN
        }

        return IbanUtils.isValid(ibanField);
    }
}
