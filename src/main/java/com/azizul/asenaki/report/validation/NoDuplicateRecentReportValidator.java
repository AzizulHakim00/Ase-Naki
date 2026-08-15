package com.azizul.asenaki.report.validation;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.ReportForm;
import com.azizul.asenaki.report.ReportState;
import com.azizul.asenaki.report.UtilityReportRepository;
import com.azizul.asenaki.report.UtilityTypeRepository;
import com.azizul.asenaki.user.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class NoDuplicateRecentReportValidator
        implements ConstraintValidator<NoDuplicateRecentReport, ReportForm> {

    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final UtilityReportRepository reportRepository;

    @Value("${app.reports.duplicate-window-minutes}")
    private long duplicateWindowMinutes;

    @Override
    public boolean isValid(ReportForm form,
                           ConstraintValidatorContext context) {
        if (form == null || form.getAreaId() == null
                || form.getUtilityTypeId() == null) {
            return true;
        }

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var user = userRepository.findByEmailIgnoreCase(email);
        var area = areaRepository.findById(form.getAreaId());
        var utility = utilityTypeRepository.findById(form.getUtilityTypeId());

        if (user.isEmpty() || area.isEmpty() || utility.isEmpty()) {
            return true;
        }

        return !reportRepository
                .existsByReporterAndAreaAndUtilityTypeAndReportedAtAfterAndState(
                        user.get(),
                        area.get(),
                        utility.get(),
                        LocalDateTime.now().minusMinutes(duplicateWindowMinutes),
                        ReportState.ACTIVE
                );
    }
}
