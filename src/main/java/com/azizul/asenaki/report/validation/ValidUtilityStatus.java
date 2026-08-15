package com.azizul.asenaki.report.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UtilityStatusValidator.class)
public @interface ValidUtilityStatus {

    String message() default "This status is not valid for the selected utility";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
