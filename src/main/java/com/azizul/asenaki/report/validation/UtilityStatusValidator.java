package com.azizul.asenaki.report.validation;

import com.azizul.asenaki.report.ReportForm;
import com.azizul.asenaki.report.UtilityTypeRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UtilityStatusValidator
        implements ConstraintValidator<ValidUtilityStatus, ReportForm> {

    private final UtilityTypeRepository utilityTypeRepository;

    @Override
    public boolean isValid(ReportForm form,
                           ConstraintValidatorContext context) {
        if (form == null || form.getUtilityTypeId() == null
                || form.getStatus() == null) {
            return true;
        }

        boolean valid = utilityTypeRepository.findById(form.getUtilityTypeId())
                .map(type -> type.allows(form.getStatus()))
                .orElse(true);

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Choose a status offered for this utility")
                    .addPropertyNode("status")
                    .addConstraintViolation();
        }
        return valid;
    }
}
