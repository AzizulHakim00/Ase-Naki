package com.azizul.asenaki.report.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoDuplicateRecentReportValidator.class)
public @interface NoDuplicateRecentReport {

    String message() default "You recently reported this utility in this area";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
