package com.azizul.asenaki.report;

import com.azizul.asenaki.user.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConfidenceCalculatorTest {

    private final ConfidenceCalculator calculator =
            new ConfidenceCalculator();

    @Test
    void startsAtTwentyPoints() {
        UtilityReport report = basicReport();

        int score = calculator.calculate(report);

        assertThat(score).isEqualTo(20);
    }

    @Test
    void confirmationRaisesTheScore() {
        UtilityReport report = basicReport();
        ReportConfirmation vote = new ReportConfirmation();
        vote.setChoice(ConfirmationChoice.CONFIRM);
        vote.setWeight(5);
        report.getConfirmations().add(vote);

        int score = calculator.calculate(report);

        assertThat(score).isEqualTo(25);
    }

    @Test
    void confidenceAlwaysStaysBetweenZeroAndOneHundred() {
        UtilityReport report = basicReport();
        for (int index = 0; index < 30; index++) {
            ReportConfirmation vote = new ReportConfirmation();
            vote.setChoice(ConfirmationChoice.DISPUTE);
            vote.setWeight(6);
            report.getConfirmations().add(vote);
        }

        int score = calculator.calculate(report);

        assertThat(score).isZero();
    }

    private UtilityReport basicReport() {
        UtilityReport report = new UtilityReport();
        report.setReporter(new UserAccount());
        report.setReportedAt(LocalDateTime.now());
        return report;
    }
}
