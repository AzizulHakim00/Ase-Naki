# Ase Naki Local Utility Incidents Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` (or subagent-driven development where available) and follow test-driven development task-by-task. Do not merge until the complete Maven verification is green.

**Goal:** Add shared local utility incidents, one-tap confirmations, preferred-area prioritization, report-to-incident linking, and admin moderation so Ase Naki can answer whether a disruption is isolated or affecting nearby users, while preserving the current approved UI.

**Architecture:** Add a focused `incident` domain beside the existing `report`, `monitoring`, `location`, and `user` packages. `UtilityIncident` stores one current area/utility/provider event. `IncidentSignal` stores one current observation per user and incident. `IncidentAggregationService` deterministically calculates state/confidence from fresh unique-user signals. `IncidentService` owns write rules and moderation. `IncidentQueryService` owns read-side summaries and preferred-area ordering. Existing `UtilityReport` remains the detailed evidence object and gains only an optional incident relationship. Existing automatic national monitoring stays separate and can never create local incidents.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring MVC, Thymeleaf, Spring Data JPA, Spring Security, H2/PostgreSQL, JUnit 5, AssertJ, Mockito, Spring Security Test, GitHub Actions, Render, Neon.

**Spec:** `docs/superpowers/specs/2026-09-06-local-utility-incidents-design.md`

## Global Constraints

- **UI freeze:** preserve the current navbar, hero, homepage section order, colors, typography, spacing, cards, status pills, buttons, auth pages, report form structure, monitoring section, and responsive behavior.
- Do not globally redesign or replace `src/main/resources/static/css/style.css`. The preferred implementation leaves that file byte-for-byte unchanged and reuses existing classes.
- `UtilityType.BROADBAND` is the codebase's Internet utility.
- Electricity and Broadband get the full local incident workflow. Gas and Water remain detailed community reports. Mobile keeps provider-normalization/domain support without forcing a new report-form redesign.
- External Power Grid/IODA/Cloudflare status remains context only and never creates/confirms a local incident.
- Server owns timestamps, incident state, confidence, counts, and moderation state.
- Fresh signal window = 45 minutes; stale incident threshold = 60 minutes; signal-change cooldown = 2 minutes; duplicate detailed-report window = 10 minutes.
- Do not migrate old reports into incidents. Only new post-deployment activity creates/updates incidents.
- Keep normal CSRF protection. Only the existing monitoring refresh endpoint remains CSRF-exempt.
- No new runtime dependency is required.
- Provider/external-source tests must not call live Internet services.

---

## Task 1: Create the incident domain model and persistence

**Files:**
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentState.java`
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentConfidence.java`
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentSignalType.java`
- Create: `src/main/java/com/azizul/asenaki/incident/UtilityProvider.java`
- Create: `src/main/java/com/azizul/asenaki/incident/UtilityIncident.java`
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentSignal.java`
- Create: `src/main/java/com/azizul/asenaki/incident/UtilityIncidentRepository.java`
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentSignalRepository.java`
- Create: `src/test/java/com/azizul/asenaki/incident/IncidentPersistenceTest.java`
- Create: `src/test/java/com/azizul/asenaki/incident/UtilityProviderTest.java`

**Interfaces:**

```java
public enum IncidentState {
    POSSIBLE_ISSUE,
    LIKELY_OUTAGE,
    CONFIRMED_OUTAGE,
    MIXED_REPORTS,
    RESTORATION_REPORTED,
    RESOLVED,
    STALE
}

public enum IncidentConfidence { LOW, MEDIUM, HIGH }

public enum IncidentSignalType {
    SAME_PROBLEM,
    WORKING_FOR_ME,
    STILL_OUT,
    RESTORED
}
```

`IncidentState` must expose a user-facing label and one of the existing CSS classes (`status-warning`, `status-unavailable`, `status-available`) so templates need no new design system.

`UtilityIncident` maps to `app_utility_incidents` with:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "area_id", nullable = false)
private Area area;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 30)
private UtilityType utilityType;

@Column(length = 100)
private String provider;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 40)
private IncidentState state;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private IncidentConfidence confidence;

private LocalDateTime firstSeenAt;
private LocalDateTime lastSignalAt;
private LocalDateTime resolvedAt;
private boolean dismissed;
```

`IncidentSignal` maps to `app_incident_signals` and must enforce one signal per user/incident:

```java
@Table(
    name = "app_incident_signals",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_incident_signal_user",
        columnNames = {"incident_id", "user_id"}))
```

Repository contracts:

```java
Optional<IncidentSignal> findByIncidentIdAndUserId(Long incidentId, Long userId);
List<IncidentSignal> findAllByIncidentId(Long incidentId);
long countByUserId(Long userId);

List<UtilityIncident> findAllByDismissedFalseOrderByLastSignalAtDesc();
Optional<UtilityIncident> findByIdAndDismissedFalse(Long id);
```

For active-key lookup, use explicit repository methods/JPQL that distinguish `provider IS NULL` from a provider-specific incident and then let the service reject `RESOLVED`/`STALE` matches.

**TDD:**

- [ ] Write persistence test first: save incident + two users' signals; verify IDs and unique rows.
- [ ] Write provider normalization tests first: Broadband trims/canonicalizes case; mobile aliases normalize to `GRAMEENPHONE`, `ROBI`, `BANGLALINK`, `TELETALK`; invalid mobile provider throws.
- [ ] Run `./mvnw --batch-mode -Dtest=IncidentPersistenceTest,UtilityProviderTest test` and confirm RED because types do not exist.
- [ ] Implement entities/enums/helper/repositories.
- [ ] Run the same targeted test command and confirm GREEN.

---

## Task 2: Implement deterministic incident aggregation

**Files:**
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentAggregationResult.java`
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentAggregationService.java`
- Create: `src/test/java/com/azizul/asenaki/incident/IncidentAggregationServiceTest.java`

**Interface:**

```java
public record IncidentAggregationResult(
        IncidentState state,
        IncidentConfidence confidence,
        boolean resolved) {}

public IncidentAggregationResult calculate(
        IncidentState previousState,
        LocalDateTime lastSignalAt,
        LocalDateTime now,
        List<IncidentSignal> signals)
```

The service is pure: no repositories, no HTTP, no controller dependencies.

Implementation constants:

```java
static final Duration FRESH_SIGNAL_WINDOW = Duration.ofMinutes(45);
static final Duration STALE_INCIDENT_AFTER = Duration.ofMinutes(60);
```

Apply the approved order exactly:

1. no fresh signals + last signal 60+ minutes old -> `STALE/LOW`;
2. previous state was `LIKELY_OUTAGE`, `CONFIRMED_OUTAGE`, or `MIXED_REPORTS`, `R >= 2`, and no affected signal newer than newest recovery -> `RESTORATION_REPORTED` (`MEDIUM` for 2–3, `HIGH` for 4+);
3. `R + W >= 3`, `R + W > A`, and no affected signal newer than newest recovery/working -> `RESOLVED`;
4. `A >= 2`, `W >= 2`, neither side at least twice the other -> `MIXED_REPORTS/MEDIUM`;
5. `A >= 4` and `A >= 2 * max(W, 1)` -> `CONFIRMED_OUTAGE/HIGH`;
6. `A` is 2–3 and `A > W` -> `LIKELY_OUTAGE/MEDIUM`;
7. `A >= 1` -> `POSSIBLE_ISSUE/LOW`;
8. otherwise retain a safe non-confirmed state; working-only observations never create a new incident in the write service.

**TDD cases:**

- [ ] one affected -> possible/low;
- [ ] 2 and 3 agreeing affected -> likely/medium;
- [ ] 4+ affected with 2:1 dominance -> confirmed/high;
- [ ] 2 affected + 2 working -> mixed/medium;
- [ ] 2 recovery signals after prior outage -> restoration reported;
- [ ] newer affected signal blocks restoration;
- [ ] recovery/working dominance -> resolved;
- [ ] 60-minute silence -> stale/low;
- [ ] signals older than 45 minutes do not vote.
- [ ] Run targeted test and confirm RED, implement, rerun GREEN.

---

## Task 3: Implement incident creation, signal updates, cooldown, and moderation

**Files:**
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentService.java`
- Create: `src/test/java/com/azizul/asenaki/incident/IncidentServiceTest.java`

**Primary methods:**

```java
@Transactional
public UtilityIncident submitAffectedObservation(
        Area area,
        UtilityType utilityType,
        String provider,
        UserAccount user,
        IncidentSignalType signalType,
        LocalDateTime now)

@Transactional
public UtilityIncident submitSignal(
        Long incidentId,
        String email,
        IncidentSignalType signalType)

@Transactional
public void dismiss(Long incidentId)

@Transactional
public void resolve(Long incidentId)
```

Rules:

- Full incident creation supports `ELECTRICITY`, `BROADBAND`, and provider-aware `MOBILE_NETWORK`; Gas/Water are not forced into this service.
- `WORKING_FOR_ME` alone never creates a new incident.
- New affected incident starts `POSSIBLE_ISSUE/LOW` and gets its first signal in the same transaction.
- Find an existing active incident by `Area + Utility + normalized provider scope`; `null` provider is distinct from provider-specific.
- `RESOLVED`, `STALE`, or dismissed incidents are not reused for new outage creation.
- Signal timestamps come from the server.
- Existing `(incident,user)` row is updated, not duplicated.
- A signal change within 2 minutes throws a useful `IllegalArgumentException`, except a transition to `RESTORED` is allowed immediately.
- Same signal submitted repeatedly does not inflate counts; it may be treated as an idempotent no-op inside cooldown.
- Recalculate and persist incident state/confidence after every accepted signal.
- `lastSignalAt` follows the accepted signal time.
- On aggregation result `RESOLVED`, set `resolvedAt`; clear it for non-resolved active states.
- `dismiss()` sets `dismissed=true`; `resolve()` sets `RESOLVED`, confidence `HIGH`, and server `resolvedAt`.

**TDD:**

- [ ] first affected observation creates one incident and one signal;
- [ ] second action by same user updates same signal row;
- [ ] cooldown blocks changing to another non-restored signal;
- [ ] immediate restored transition is allowed;
- [ ] working-only request cannot create incident;
- [ ] Broadband provider-specific incidents stay separate from area-wide incidents;
- [ ] stale/resolved/dismissed incident rejects normal one-tap submission;
- [ ] moderation changes preserve the row rather than deleting it.

---

## Task 4: Add preferred area and read-side summaries

**Files:**
- Modify: `src/main/java/com/azizul/asenaki/user/UserProfile.java`
- Modify: `src/main/java/com/azizul/asenaki/user/UserService.java`
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentSummary.java`
- Create: `src/main/java/com/azizul/asenaki/incident/IncidentQueryService.java`
- Create: `src/main/java/com/azizul/asenaki/web/ProfileController.java`
- Create: `src/test/java/com/azizul/asenaki/incident/IncidentQueryServiceTest.java`
- Create: `src/test/java/com/azizul/asenaki/user/UserServicePreferredAreaTest.java`

`UserProfile` addition:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "preferred_area_id")
private Area preferredArea;
```

`UserService`:

```java
@Transactional
public void setPreferredArea(String email, Long areaId)
```

`IncidentSummary` should provide template-ready values without putting counting logic in Thymeleaf:

```java
public record IncidentSummary(
        UtilityIncident incident,
        long affected,
        long working,
        long restored,
        long total,
        LocalDateTime lastUpdated) {}
```

`IncidentQueryService` methods:

```java
public Optional<IncidentSummary> summary(Long incidentId);
public List<IncidentSummary> activeSummaries(String emailOrNull);
public long contributionCount(String email);
public List<UtilityIncident> recentAreaHistory(Long areaId, int limit);
```

Ordering: if authenticated user has a preferred area, matching active summaries come first, then newest `lastSignalAt`; guests use newest first.

`ProfileController`:

```java
@PostMapping("/profile/preferred-area")
public String preferredArea(Long areaId, Authentication authentication,
                            @RequestParam(defaultValue = "/") String returnTo,
                            RedirectAttributes redirectAttributes)
```

Validate `returnTo` as an application-local path (`/…`) before redirecting; otherwise redirect `/`.

**TDD:**

- [ ] preferred area persists to existing profile;
- [ ] invalid area rejects cleanly;
- [ ] preferred-area active incidents sort first;
- [ ] guest ordering remains newest-first;
- [ ] contribution count uses unique signal rows.

---

## Task 5: Integrate detailed reports with incidents and duplicate protection

**Files:**
- Modify: `src/main/java/com/azizul/asenaki/report/UtilityReport.java`
- Modify: `src/main/java/com/azizul/asenaki/report/UtilityReportRepository.java`
- Modify: `src/main/java/com/azizul/asenaki/report/ReportService.java`
- Modify: `src/main/java/com/azizul/asenaki/web/ReportController.java`
- Create: `src/test/java/com/azizul/asenaki/report/ReportIncidentIntegrationTest.java`

`UtilityReport` addition:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "incident_id")
private UtilityIncident incident;
```

Repository duplicate query:

```java
Optional<UtilityReport> findFirstByReporterIdAndAreaIdAndUtilityTypeAndStatusAndReportedAtAfterOrderByReportedAtDesc(
        Long reporterId,
        Long areaId,
        UtilityType utilityType,
        UtilityStatus status,
        LocalDateTime reportedAt);
```

Before saving a new report, reject same user + area + utility + status in previous 10 minutes with:

```text
You recently submitted the same status for this area. Update it after the situation changes.
```

Incident mapping for new reports:

- Electricity/Broadband `UNAVAILABLE` or `UNSTABLE` -> create/find compatible incident and create/update `SAME_PROBLEM`.
- Electricity/Broadband `AVAILABLE` -> if compatible active incident exists, link report and create/update `WORKING_FOR_ME`; if no incident exists, save normal report with no incident.
- `MAINTENANCE` may link to an existing compatible incident but does not generate an affected signal.
- Gas/Water stay unlinked by default in this version.
- Mobile detailed reports remain normal reports unless a provider-aware incident is explicitly supplied by the incident service; do not redesign the current report form to collect provider.
- Existing evidence validation/upload behavior must remain unchanged.

Expose one package-level/service method for `ReportService` to ask `IncidentService` for compatible active incident without duplicating lookup logic.

Controller error handling must show duplicate/incident validation messages without pretending they are image errors. Use a general form error or description field error while preserving the current form layout.

**TDD:**

- [ ] affected Electricity report creates/links an incident and counts reporter once;
- [ ] affected Broadband report behaves the same;
- [ ] available report does not create a new incident by itself;
- [ ] available report links to an active compatible incident and records working signal;
- [ ] same user/area/utility/status within 10 minutes is rejected;
- [ ] changed status inside 10 minutes is allowed;
- [ ] existing evidence upload test still passes.

---

## Task 6: Add one-tap and admin MVC routes with security

**Files:**
- Create: `src/main/java/com/azizul/asenaki/web/IncidentController.java`
- Create: `src/main/java/com/azizul/asenaki/web/IncidentAdminController.java`
- Modify: `src/main/java/com/azizul/asenaki/config/SecurityConfig.java`
- Create: `src/test/java/com/azizul/asenaki/web/IncidentSecurityIntegrationTest.java`
- Create: `src/test/java/com/azizul/asenaki/web/IncidentControllerTest.java`

User signal route:

```java
@PostMapping("/incidents/{id}/signals")
public String signal(@PathVariable Long id,
                     @RequestParam IncidentSignalType signalType,
                     Authentication authentication,
                     @RequestParam(defaultValue = "/") String returnTo,
                     RedirectAttributes redirectAttributes)
```

Admin routes:

```java
@PostMapping("/admin/incidents/{id}/dismiss")
@PostMapping("/admin/incidents/{id}/resolve")
```

`SecurityConfig` ordering:

```java
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.GET, "/reports/{id:[0-9]+}", "/evidence/**").permitAll()
.requestMatchers(HttpMethod.POST, "/internal/monitoring/refresh").permitAll()
.anyRequest().authenticated()
```

Do **not** add incident/profile/admin routes to CSRF ignore list.

Integration security test builds MockMvc from the real `WebApplicationContext` with Spring Security and verifies:

- [ ] guest POST `/incidents/{id}/signals` is not allowed;
- [ ] authenticated POST without CSRF is forbidden;
- [ ] authenticated POST with CSRF reaches route/service behavior;
- [ ] normal user cannot POST `/admin/**`;
- [ ] admin with CSRF can moderate;
- [ ] existing public monitoring refresh remains allowed.

---

## Task 7: Enrich existing pages without redesigning them

**Files:**
- Modify: `src/main/java/com/azizul/asenaki/web/HomeController.java`
- Modify: `src/main/java/com/azizul/asenaki/web/ReportController.java`
- Modify: `src/main/resources/templates/home.html`
- Modify: `src/main/resources/templates/reports/details.html`
- Test: `src/test/java/com/azizul/asenaki/web/HomeControllerTest.java`
- Create: `src/test/java/com/azizul/asenaki/web/UiFreezeTest.java`
- **Do not modify:** `src/main/resources/static/css/style.css`

HomeController gains `IncidentQueryService`. Keep every existing model attribute and add only read-side incident information. To avoid changing homepage section structure, do not add a new section. Existing report cards may display linked incident state/count metadata inside their existing `report-meta` block.

Example additive markup inside the current report card:

```html
<span th:if="${report.incident != null}">
  <i class="bi bi-people"></i>
  <span th:text="${incidentSummaries.get(report.incident.id).affected + ' affected · ' + incidentSummaries.get(report.incident.id).working + ' working'}"></span>
</span>
```

Prefer preparing a map/view model in the controller/service so Thymeleaf stays simple.

On `reports/details.html`, if the report has a linked incident, append within the existing `details-card`:

- current incident state using the existing `status-pill` class;
- confidence/freshness/counts inside existing `details-meta` / `report-meta` patterns;
- authenticated one-tap forms using current `btn btn-soft`, `btn btn-brand`, and `btn btn-outline-secondary` classes;
- preferred-area save action using an existing button class;
- admin moderation controls only for `ROLE_ADMIN`;
- no new visual shell/section architecture.

`UiFreezeTest` must compare/inspect the tracked stylesheet and template anchors:

```java
assertThat(styleCss).doesNotContain("incident-dashboard", "new-hero");
assertThat(homeHtml).contains("<section class=\"hero\">", "id=\"reports\"", "class=\"how-section\"");
```

Additionally use Git diff before merge to verify `style.css` is unchanged from main SHA `00cb39f86cdb1ee7f10e4b56e5d96d6c374a2c16`.

**TDD:**

- [ ] HomeController test verifies old attributes still present plus incident summary attributes;
- [ ] report details controller adds incident summary only when linked;
- [ ] UI-freeze test protects the current major structure;
- [ ] no stylesheet edit is committed.

---

## Task 8: Documentation, complete verification, PR, merge, and production deployment

**Files:**
- Modify: `README.md`
- No Render env-var changes required.
- No monitoring workflow changes required.

README additions:

- explain local incidents vs national automatic monitoring;
- explain one-user-one-signal rule;
- explain freshness/stale behavior;
- explain preferred area and admin moderation;
- keep deployment/free-tier instructions intact.

**Verification:**

- [ ] Run `./mvnw --batch-mode verify` in GitHub Actions/CI; require zero failures/errors.
- [ ] Inspect test summary and record total tests.
- [ ] Compare feature branch against base and verify `src/main/resources/static/css/style.css` has no diff.
- [ ] Verify current hero/navbar/home section order remain intact in template diff.
- [ ] Verify no `TODO`, `TBD`, placeholder secrets, or disabled TLS/CSRF changes were introduced.
- [ ] Open PR against `main` with feature summary + verification evidence.
- [ ] Merge only after CI is green and PR is mergeable.
- [ ] Verify fresh `main` CI is green after merge.
- [ ] Confirm Render auto-deploys the exact merged commit to service `ase-naki-x5ie`.
- [ ] Confirm Render logs show Spring Boot startup, successful Neon connection, and no schema-migration/startup errors.
- [ ] Use a real browser/cloud request path where available to verify `/actuator/health`, public homepage/report read, authenticated incident writes via tests, and the existing scheduled monitoring path.
- [ ] Do not claim production completion until Render reports the merged commit `live`.

## Expected File Map

New production files:

```text
src/main/java/com/azizul/asenaki/incident/
├── IncidentAggregationResult.java
├── IncidentAggregationService.java
├── IncidentConfidence.java
├── IncidentQueryService.java
├── IncidentService.java
├── IncidentSignal.java
├── IncidentSignalRepository.java
├── IncidentSignalType.java
├── IncidentState.java
├── IncidentSummary.java
├── UtilityIncident.java
├── UtilityIncidentRepository.java
└── UtilityProvider.java

src/main/java/com/azizul/asenaki/web/
├── IncidentController.java
├── IncidentAdminController.java
└── ProfileController.java
```

Modified production files:

```text
src/main/java/com/azizul/asenaki/user/UserProfile.java
src/main/java/com/azizul/asenaki/user/UserService.java
src/main/java/com/azizul/asenaki/report/UtilityReport.java
src/main/java/com/azizul/asenaki/report/UtilityReportRepository.java
src/main/java/com/azizul/asenaki/report/ReportService.java
src/main/java/com/azizul/asenaki/config/SecurityConfig.java
src/main/java/com/azizul/asenaki/web/HomeController.java
src/main/java/com/azizul/asenaki/web/ReportController.java
src/main/resources/templates/home.html
src/main/resources/templates/reports/details.html
README.md
```

`src/main/resources/static/css/style.css` is deliberately **not** in the modified-file list.

## Plan Self-Review Checklist

- [x] Covers every required incident state and confidence rule from the approved spec.
- [x] Covers 45/60/2/10 minute freshness/cooldown/duplicate constants.
- [x] Covers one-current-signal-per-user database uniqueness.
- [x] Covers Broadband provider separation and mobile provider normalization.
- [x] Covers preferred area, detailed-report integration, moderation, CSRF, guest/admin authorization.
- [x] Keeps Power Grid/IODA/Cloudflare separate from local truth.
- [x] Explicitly freezes the UI and keeps `style.css` unchanged.
- [x] Does not require historical incident migration.
- [x] Contains no placeholder implementation steps.
