package com.azizul.asenaki;

import static org.assertj.core.api.Assertions.assertThat;

import com.azizul.asenaki.report.ReportForm;
import com.azizul.asenaki.user.RegistrationForm;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class FormValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory().getValidator();

    @Test
    void registrationFormRejectsInvalidData() {
        RegistrationForm form = new RegistrationForm();
        form.setEmail("wrong-email");
        form.setPhone("123");
        form.setPassword("short");

        var fieldNames = validator.validate(form).stream()
                .map(error -> error.getPropertyPath().toString())
                .toList();

        assertThat(fieldNames).contains(
                "name", "email", "phone", "password", "acceptedRules");
    }

    @Test
    void reportFormRequiresTheMainFields() {
        ReportForm form = new ReportForm();

        var fieldNames = validator.validate(form).stream()
                .map(error -> error.getPropertyPath().toString())
                .toList();

        assertThat(fieldNames).contains(
                "areaId", "utilityType", "status", "description");
    }
}
