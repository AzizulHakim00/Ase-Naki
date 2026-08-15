package com.azizul.asenaki.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BangladeshPhoneValidator
        implements ConstraintValidator<ValidBangladeshPhone, String> {

    private static final String BANGLADESH_PHONE =
            "^(?:\\+?880|0)1[3-9]\\d{8}$";

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        return phone == null || phone.isBlank() || phone.matches(BANGLADESH_PHONE);
    }
}
