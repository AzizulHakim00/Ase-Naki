package com.azizul.asenaki.report;

import com.azizul.asenaki.user.RoleName;
import org.springframework.stereotype.Component;

@Component
public class ConfidenceCalculator {

    public int calculate(UtilityReport report) {
        int score = 20;

        if (report.getReporter().hasRole(RoleName.TRUSTED_REPORTER)) {
            score += 5;
        }
        if (report.hasEvidence()) {
            score += 10;
        }

        for (ReportConfirmation confirmation : report.getConfirmations()) {
            score += switch (confirmation.getChoice()) {
                case CONFIRM -> confirmation.getWeight();
                case DISPUTE -> -confirmation.getWeight();
                case RESTORED -> -4;
            };
        }

        int agePenalty = (int) (report.getAgeInMinutes() / 60) * 2;
        score -= agePenalty;

        return Math.max(0, Math.min(100, score));
    }
}
