package com.azizul.asenaki.report;

import com.azizul.asenaki.admin.AuditLog;
import com.azizul.asenaki.admin.AuditLogRepository;
import com.azizul.asenaki.common.AppException;
import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.notification.NotificationService;
import com.azizul.asenaki.user.RoleName;
import com.azizul.asenaki.user.UserAccount;
import com.azizul.asenaki.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UtilityReportRepository reportRepository;
    private final ReportConfirmationRepository confirmationRepository;
    private final AreaRepository areaRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final IncidentStatusHistoryRepository historyRepository;
    private final EvidenceService evidenceService;
    private final ConfidenceCalculator confidenceCalculator;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;

    @Value("${app.reports.duplicate-window-minutes}")
    private long duplicateWindowMinutes;

    @Value("${app.reports.expiry-hours}")
    private long expiryHours;

    @Value("${app.reports.restored-votes-required}")
    private long restoredVotesRequired;

    @Transactional(readOnly = true)
    public List<UtilityReport> findActive(Long areaId, Long utilityTypeId) {
        return reportRepository.findCards(
                ReportState.ACTIVE, areaId, utilityTypeId);
    }

    @Transactional(readOnly = true)
    public UtilityReport findDetailed(Long id) {
        return reportRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException("Report not found"));
    }

    @Transactional(readOnly = true)
    public List<UtilityReport> findAllForModeration() {
        return reportRepository.findAllWithDetails();
    }

    @Transactional
    public UtilityReport create(ReportForm form, String email) {
        UserAccount reporter = userService.findByEmail(email);
        Area area = areaRepository.findById(form.getAreaId())
                .orElseThrow(() -> new AppException("Area not found"));
        UtilityType utility = utilityTypeRepository
                .findById(form.getUtilityTypeId())
                .orElseThrow(() -> new AppException("Utility not found"));

        checkBusinessRules(form, reporter, area, utility);

        UtilityReport report = new UtilityReport();
        report.setReporter(reporter);
        report.setArea(area);
        report.setUtilityType(utility);
        report.setStatus(form.getStatus());
        report.setDescription(cleanDescription(form.getDescription()));
        report.setReportedAt(LocalDateTime.now());
        report.setExpiresAt(LocalDateTime.now().plusHours(expiryHours));

        if (form.getEvidence() != null && !form.getEvidence().isEmpty()) {
            ReportEvidence evidence =
                    evidenceService.buildEvidence(form.getEvidence(), report);
            report.getEvidenceFiles().add(evidence);
        }

        if (isOutage(form.getStatus())) {
            addIncident(report);
        }

        report.setConfidence(confidenceCalculator.calculate(report));
        UtilityReport saved = reportRepository.save(report);
        notificationService.notifySavedLocationUsers(saved);
        return saved;
    }

    @Transactional
    public void vote(Long reportId, ConfirmationChoice choice, String email) {
        UtilityReport report = findDetailed(reportId);
        UserAccount user = userService.findByEmail(email);

        if (report.getReporter().getId().equals(user.getId())) {
            throw new AppException("You cannot vote on your own report");
        }
        if (report.getState() != ReportState.ACTIVE) {
            throw new AppException("This report is no longer active");
        }

        ReportConfirmation vote = confirmationRepository
                .findByReportAndUser(report, user)
                .orElseGet(ReportConfirmation::new);

        boolean newVote = vote.getId() == null;
        vote.setReport(report);
        vote.setUser(user);
        vote.setChoice(choice);
        vote.setWeight(voteWeight(choice, user));
        confirmationRepository.save(vote);

        if (newVote) {
            report.getConfirmations().add(vote);
        }

        if (choice == ConfirmationChoice.RESTORED
                && confirmationRepository.countByReportAndChoice(
                report, ConfirmationChoice.RESTORED) >= restoredVotesRequired) {
            restoreReport(report, user);
        } else {
            report.setConfidence(confidenceCalculator.calculate(report));
        }
    }

    @Transactional
    public void moderate(
            Long reportId, ReportState state, String reason, String actorEmail) {
        if (state != ReportState.ACTIVE && state != ReportState.REMOVED) {
            throw new AppException("Moderators can activate or remove reports");
        }

        UtilityReport report = findDetailed(reportId);
        UserAccount actor = userService.findByEmail(actorEmail);
        ReportState previous = report.getState();
        report.setState(state);

        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction("MODERATE_REPORT");
        log.setEntityType("UtilityReport");
        log.setEntityId(report.getId());
        log.setDetails(previous + " -> " + state + ". "
                + (reason == null ? "" : reason.trim()));
        auditLogRepository.save(log);
    }

    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void expireOldReports() {
        List<UtilityReport> expired = reportRepository
                .findByStateAndExpiresAtBefore(
                        ReportState.ACTIVE, LocalDateTime.now());
        expired.forEach(report -> report.setState(ReportState.EXPIRED));
    }

    private void checkBusinessRules(
            ReportForm form,
            UserAccount reporter,
            Area area,
            UtilityType utility) {
        if (!utility.allows(form.getStatus())) {
            throw new AppException(
                    "This status is not valid for " + utility.getName());
        }

        boolean duplicate = reportRepository
                .existsByReporterAndAreaAndUtilityTypeAndReportedAtAfterAndState(
                        reporter,
                        area,
                        utility,
                        LocalDateTime.now().minusMinutes(duplicateWindowMinutes),
                        ReportState.ACTIVE
                );
        if (duplicate) {
            throw new AppException(
                    "Please wait before reporting this utility here again");
        }
    }

    private void addIncident(UtilityReport report) {
        OutageIncident incident = new OutageIncident();
        incident.setReport(report);
        incident.setState(IncidentState.ACTIVE);
        incident.setOpenedAt(report.getReportedAt());
        report.setIncident(incident);

        IncidentStatusHistory history = new IncidentStatusHistory();
        history.setIncident(incident);
        history.setChangedBy(report.getReporter());
        history.setState(IncidentState.ACTIVE);
        history.setNote("Community outage report opened");
        incident.getStatusHistory().add(history);
    }

    private void restoreReport(UtilityReport report, UserAccount user) {
        report.setState(ReportState.RESTORED);
        report.setStatus(UtilityStatus.RESTORED);
        report.setConfidence(confidenceCalculator.calculate(report));

        if (report.getIncident() != null) {
            report.getIncident().setState(IncidentState.RESOLVED);
            report.getIncident().setResolvedAt(LocalDateTime.now());

            IncidentStatusHistory history = new IncidentStatusHistory();
            history.setIncident(report.getIncident());
            history.setChangedBy(user);
            history.setState(IncidentState.RESOLVED);
            history.setNote("Restored after "
                    + restoredVotesRequired + " community votes");
            historyRepository.save(history);
        }
    }

    private int voteWeight(
            ConfirmationChoice choice, UserAccount user) {
        boolean trusted = user.hasRole(RoleName.TRUSTED_REPORTER);
        return switch (choice) {
            case CONFIRM -> trusted ? 8 : 5;
            case DISPUTE -> trusted ? 9 : 6;
            case RESTORED -> 4;
        };
    }

    private boolean isOutage(UtilityStatus status) {
        return status != UtilityStatus.AVAILABLE
                && status != UtilityStatus.RESTORED;
    }

    private String cleanDescription(String description) {
        return description == null || description.isBlank()
                ? null
                : description.trim();
    }
}
