package com.azizul.asenaki.report.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EvidenceFileValidator.class)
public @interface ValidEvidenceFile {

    String message() default "Upload a JPG, PNG, WebP, or PDF file up to 5 MB";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
