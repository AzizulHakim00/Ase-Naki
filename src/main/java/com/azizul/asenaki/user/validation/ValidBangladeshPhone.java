package com.azizul.asenaki.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BangladeshPhoneValidator.class)
public @interface ValidBangladeshPhone {

    String message() default "Enter a valid Bangladesh mobile number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
