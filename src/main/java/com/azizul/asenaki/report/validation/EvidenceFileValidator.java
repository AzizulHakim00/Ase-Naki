package com.azizul.asenaki.report.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class EvidenceFileValidator
        implements ConstraintValidator<ValidEvidenceFile, MultipartFile> {

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );

    @Override
    public boolean isValid(MultipartFile file,
                           ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true;
        }
        return file.getSize() <= MAX_SIZE
                && ALLOWED_TYPES.contains(file.getContentType());
    }
}
