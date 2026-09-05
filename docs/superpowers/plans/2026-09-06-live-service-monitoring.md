# Ase Naki? Live Service Monitoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add free automatic Bangladesh internet and national electricity monitoring to the existing Ase Naki? Spring Boot application without redesigning the approved UI or breaking the community-report flow.

**Architecture:** Add a focused `monitoring` subsystem that collects three external signals: Cloudflare Radar, IODA, and Power Grid Bangladesh. External calls happen only during an explicit refresh operation, snapshots are persisted with JPA, and the homepage reads only the latest database snapshots. A secret-protected refresh endpoint is called every 30 minutes by GitHub Actions so Render Free sleep does not prevent updates.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring MVC/RestClient, Spring Data JPA, Thymeleaf, PostgreSQL/H2, Jackson, Jsoup, Spring Security, JUnit 5, MockMvc, Mockito, GitHub Actions, Render Free, Neon Free.

**Spec:** `docs/superpowers/specs/2026-09-06-live-service-monitoring-design.md`

## Global Constraints

- Preserve the existing hero, navigation, report cards, typography, spacing system, color palette, login/register pages, and report forms.
- Community reports remain fully functional and visible.
- No IoT, ML, paid APIs, SMS, WebSockets, feeder-level prediction, Google Maps, or mobile application.
- Minimum deployment remains $0/month using Render Free, Neon Free, GitHub Actions, Cloudflare Radar free access, IODA public API, and Power Grid Bangladesh public data.
- Homepage requests must never call external providers directly.
- Provider failure must never be interpreted as healthy/normal.
- External API tests use fixtures/mocks and never require internet access.
- Secrets are read only from environment/application properties and are never committed.

---

## File map

New production files:

- `src/main/java/com/azizul/asenaki/monitoring/MonitoringState.java` — shared normalized state enum.
- `src/main/java/com/azizul/asenaki/monitoring/ProviderSignal.java` — immutable normalized provider result.
- `src/main/java/com/azizul/asenaki/monitoring/PowerSnapshot.java` — persisted national power reading.
- `src/main/java/com/azizul/asenaki/monitoring/InternetStatusSnapshot.java` — persisted combined internet status.
- `src/main/java/com/azizul/asenaki/monitoring/PowerSnapshotRepository.java` — latest/history queries.
- `src/main/java/com/azizul/asenaki/monitoring/InternetStatusSnapshotRepository.java` — latest internet snapshot query.
- `src/main/java/com/azizul/asenaki/monitoring/external/PowerGridService.java` — fetch/parse Power Grid HTML.
- `src/main/java/com/azizul/asenaki/monitoring/external/IodaService.java` — fetch IODA Bangladesh alerts.
- `src/main/java/com/azizul/asenaki/monitoring/external/CloudflareRadarService.java` — fetch Cloudflare Bangladesh outages.
- `src/main/java/com/azizul/asenaki/monitoring/MonitoringAggregationService.java` — deterministic status combination.
- `src/main/java/com/azizul/asenaki/monitoring/MonitoringRefreshService.java` — provider refresh orchestration/persistence.
- `src/main/java/com/azizul/asenaki/monitoring/MonitoringQueryService.java` — latest dashboard values for MVC.
- `src/main/java/com/azizul/asenaki/web/MonitoringController.java` — secret-protected refresh endpoint.
- `.github/workflows/monitoring-refresh.yml` — 30-minute cloud refresh trigger.

Modified production files:

- `pom.xml` — add Jsoup.
- `src/main/resources/application.yml` — monitoring URLs/token/secret defaults.
- `src/test/resources/application.yml` — deterministic test monitoring config.
- `src/main/java/com/azizul/asenaki/config/SecurityConfig.java` — allow secret-auth refresh endpoint and ignore CSRF only for it.
- `src/main/java/com/azizul/asenaki/web/HomeController.java` — load latest persisted automatic status.
- `src/main/resources/templates/home.html` — insert one automatic-status section using existing visual language.
- `src/main/resources/static/css/style.css` — only additive selectors for the new section using current variables.
- `render.yaml` — add generated refresh secret and optional Cloudflare token variable.
- `README.md` — document automatic monitoring and free setup.

New tests:

- `src/test/java/com/azizul/asenaki/monitoring/MonitoringAggregationServiceTest.java`
- `src/test/java/com/azizul/asenaki/monitoring/external/PowerGridServiceTest.java`
- `src/test/java/com/azizul/asenaki/monitoring/external/IodaServiceTest.java`
- `src/test/java/com/azizul/asenaki/monitoring/external/CloudflareRadarServiceTest.java`
- `src/test/java/com/azizul/asenaki/monitoring/MonitoringRefreshServiceTest.java`
- `src/test/java/com/azizul/asenaki/web/MonitoringControllerTest.java`

---

### Task 1: Monitoring model and persistence

**Files:**
- Create: `src/main/java/com/azizul/asenaki/monitoring/MonitoringState.java`
- Create: `src/main/java/com/azizul/asenaki/monitoring/ProviderSignal.java`
- Create: `src/main/java/com/azizul/asenaki/monitoring/PowerSnapshot.java`
- Create: `src/main/java/com/azizul/asenaki/monitoring/InternetStatusSnapshot.java`
- Create: `src/main/java/com/azizul/asenaki/monitoring/PowerSnapshotRepository.java`
- Create: `src/main/java/com/azizul/asenaki/monitoring/InternetStatusSnapshotRepository.java`
- Test: extend `src/test/java/com/azizul/asenaki/AseNakiApplicationTests.java`

**Interfaces:**
- Produces: `MonitoringState { NORMAL, POSSIBLE_DISRUPTION, LIKELY_DISRUPTION, UNAVAILABLE }`.
- Produces: `ProviderSignal(MonitoringState state, String summary, String affectedNetwork)`.
- Produces repository methods `Optional<PowerSnapshot> findTopByOrderByFetchedAtDesc()`, `List<PowerSnapshot> findTop24ByOrderByObservedAtDesc()`, and `Optional<InternetStatusSnapshot> findTopByOrderByCheckedAtDesc()`.

- [ ] **Step 1: Write a failing JPA persistence test**

Add a test that saves one `PowerSnapshot` and one `InternetStatusSnapshot`, then asserts that each repository's `findTop...` method returns the saved entity and state.

- [ ] **Step 2: Run the focused application test**

Run: `./mvnw --batch-mode -Dtest=AseNakiApplicationTests test`

Expected: FAIL because monitoring classes/repositories do not exist.

- [ ] **Step 3: Implement the model and repositories**

Use standard JPA entities with `@GeneratedValue(strategy = GenerationType.IDENTITY)`. Persist enum states using `@Enumerated(EnumType.STRING)`. Store `observedAt`, `fetchedAt`, and `checkedAt` as `LocalDateTime`. Use Lombok `@Getter/@Setter` to match the existing project style.

`ProviderSignal` is a Java record:

```java
public record ProviderSignal(
        MonitoringState state,
        String summary,
        String affectedNetwork) {

    public static ProviderSignal unavailable(String summary) {
        return new ProviderSignal(MonitoringState.UNAVAILABLE, summary, null);
    }
}
```

- [ ] **Step 4: Run the focused test again**

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add monitoring persistence model`

---

### Task 2: Power Grid Bangladesh parser

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/azizul/asenaki/monitoring/external/PowerGridService.java`
- Create: `src/test/java/com/azizul/asenaki/monitoring/external/PowerGridServiceTest.java`

**Interfaces:**
- Produces: `Optional<PowerSnapshot> parseLatest(String html, LocalDateTime fetchedAt)`.
- Produces: `Optional<PowerSnapshot> fetchLatest()`.
- Uses source URL `https://erp.powergrid.gov.bd/web/generations/view_demand_supply_loadshed?page=1`.

- [ ] **Step 1: Add failing parser tests**

Use an inline HTML fixture containing a table row with cells `04-09-2026`, `20:00:00 20:00`, `14,952`, `14738`, `214`, `Evening Peak`. Assert demand `14952`, supply `14738`, load shedding `214`, and observed time `2026-09-04T20:00`.

Add a malformed fixture without six usable cells and assert `Optional.empty()`.

- [ ] **Step 2: Run the parser test**

Run: `./mvnw --batch-mode -Dtest=PowerGridServiceTest test`

Expected: FAIL because service/Jsoup dependency do not exist.

- [ ] **Step 3: Add Jsoup and implement parsing**

Add dependency:

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
</dependency>
```

Parse the first usable `tbody tr`. Remove commas from numeric cells. Parse date with `dd-MM-uuuu`. From the time cell, take the first `HH:mm:ss` token. Reject rows that cannot parse all required numeric fields; never derive missing values.

`fetchLatest()` uses Spring `RestClient` with `Accept: text/html`, catches runtime HTTP/parsing errors, logs one concise warning, and returns `Optional.empty()`.

- [ ] **Step 4: Re-run the parser test**

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: collect Power Grid Bangladesh data`

---

### Task 3: IODA Bangladesh signal

**Files:**
- Create: `src/main/java/com/azizul/asenaki/monitoring/external/IodaService.java`
- Create: `src/test/java/com/azizul/asenaki/monitoring/external/IodaServiceTest.java`
- Modify: `src/main/resources/application.yml`

**Interfaces:**
- Produces: `ProviderSignal checkBangladesh()`.
- Uses: `GET https://api.ioda.inetintel.cc.gatech.edu/v2/outages/alerts`.
- Query: `from=<unix-now-minus-6h>&until=<unix-now>&limit=100&relatedTo=country/BD`.
- IODA response envelope: `{ "data": [ ...alerts... ] }` where each alert includes `level`, `datasource`, `entity`, and `time`.

- [ ] **Step 1: Write failing JSON-classification tests**

Test a response with `data: []` and expect `NORMAL`.

Test a response containing a recent alert with `"level":"critical"` and `entity.code:"BD"` and expect `POSSIBLE_DISRUPTION` with a short IODA summary.

Test malformed JSON/HTTP failure through a mocked transport seam and expect `UNAVAILABLE`, never `NORMAL`.

- [ ] **Step 2: Run the IODA tests**

Expected: FAIL because service does not exist.

- [ ] **Step 3: Implement IODA service**

Use Jackson `JsonNode` via the existing Spring Boot Jackson stack. Treat at least one `critical` alert in the six-hour Bangladesh window as `POSSIBLE_DISRUPTION`; zero critical alerts after a successful response is `NORMAL`; HTTP, timeout, or payload errors are `UNAVAILABLE`.

Configuration keys:

```yaml
app:
  monitoring:
    ioda-url: https://api.ioda.inetintel.cc.gatech.edu/v2/outages/alerts
```

- [ ] **Step 4: Re-run the IODA tests**

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add IODA outage monitoring`

---

### Task 4: Cloudflare Radar Bangladesh signal

**Files:**
- Create: `src/main/java/com/azizul/asenaki/monitoring/external/CloudflareRadarService.java`
- Create: `src/test/java/com/azizul/asenaki/monitoring/external/CloudflareRadarServiceTest.java`
- Modify: `src/main/resources/application.yml`

**Interfaces:**
- Produces: `ProviderSignal checkBangladesh()`.
- Uses: `GET https://api.cloudflare.com/client/v4/radar/annotations/outages?dateRange=1d&limit=100&format=json&location=BD`.
- Auth header: `Authorization: Bearer ${CLOUDFLARE_API_TOKEN}`.
- Successful response envelope: `{ "success": true, "result": { "annotations": [...] } }`.

- [ ] **Step 1: Write failing Cloudflare tests**

Test no configured token -> `UNAVAILABLE` without attempting a request.

Test successful empty Bangladesh annotations -> `NORMAL`.

Test one annotation with location `BD` and `endDate: null` -> `POSSIBLE_DISRUPTION`, carrying the first ASN/network name when present.

Test API failure -> `UNAVAILABLE`.

- [ ] **Step 2: Run the Cloudflare tests**

Expected: FAIL because service does not exist.

- [ ] **Step 3: Implement Cloudflare service**

Read the token from `${CLOUDFLARE_API_TOKEN:}`. Use `RestClient`, a one-day window, maximum 100 annotations, and Bangladesh location filter. An annotation is treated as active when its `endDate` is null. Never expose the token in logs.

Configuration keys:

```yaml
app:
  monitoring:
    cloudflare-url: https://api.cloudflare.com/client/v4/radar/annotations/outages
    cloudflare-token: ${CLOUDFLARE_API_TOKEN:}
```

- [ ] **Step 4: Re-run Cloudflare tests**

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add Cloudflare Radar monitoring`

---

### Task 5: Aggregation and refresh orchestration

**Files:**
- Create: `src/main/java/com/azizul/asenaki/monitoring/MonitoringAggregationService.java`
- Create: `src/main/java/com/azizul/asenaki/monitoring/MonitoringRefreshService.java`
- Create: `src/main/java/com/azizul/asenaki/monitoring/MonitoringQueryService.java`
- Create: `src/test/java/com/azizul/asenaki/monitoring/MonitoringAggregationServiceTest.java`
- Create: `src/test/java/com/azizul/asenaki/monitoring/MonitoringRefreshServiceTest.java`

**Interfaces:**
- Produces: `MonitoringState aggregate(ProviderSignal cloudflare, ProviderSignal ioda)`.
- Produces: `void refreshAll()`.
- Produces: `Optional<PowerSnapshot> latestPower()`, `Optional<InternetStatusSnapshot> latestInternet()`, `List<PowerSnapshot> recentPower()`.

- [ ] **Step 1: Write failing aggregation tests**

Assert:

```text
NORMAL + NORMAL -> NORMAL
POSSIBLE + NORMAL -> POSSIBLE_DISRUPTION
NORMAL + POSSIBLE -> POSSIBLE_DISRUPTION
POSSIBLE + POSSIBLE -> LIKELY_DISRUPTION
UNAVAILABLE + NORMAL -> NORMAL only for the successful source's explicit result, while snapshot records Cloudflare UNAVAILABLE
UNAVAILABLE + UNAVAILABLE -> UNAVAILABLE
```

The summary must state when a source is unavailable instead of implying two-source confirmation.

- [ ] **Step 2: Write failing refresh tests**

Mock all three provider services and repositories. Assert a successful Power Grid result is saved. Assert an `InternetStatusSnapshot` is always saved for a completed refresh even if one internet provider is unavailable. Assert a Power Grid failure does not delete/overwrite the previous snapshot.

- [ ] **Step 3: Run focused monitoring tests**

Expected: FAIL because services do not exist.

- [ ] **Step 4: Implement aggregation/refresh/query services**

`refreshAll()` calls each provider independently, persists successful power data, always persists the normalized internet snapshot, and never throws solely because one provider failed.

`MonitoringQueryService` reads repositories only and performs no network calls.

- [ ] **Step 5: Run focused monitoring tests again**

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: aggregate and persist live monitoring`

---

### Task 6: Protected refresh endpoint

**Files:**
- Create: `src/main/java/com/azizul/asenaki/web/MonitoringController.java`
- Modify: `src/main/java/com/azizul/asenaki/config/SecurityConfig.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/resources/application.yml`
- Create: `src/test/java/com/azizul/asenaki/web/MonitoringControllerTest.java`

**Interfaces:**
- Produces: `POST /internal/monitoring/refresh`.
- Required header: `X-Monitoring-Secret`.
- Config property: `app.monitoring.refresh-secret=${MONITORING_REFRESH_SECRET:}`.

- [ ] **Step 1: Write failing MockMvc tests**

Assert missing/wrong secret returns `403 Forbidden` and does not call `MonitoringRefreshService`.

Assert correct test secret returns `204 No Content` and calls `refreshAll()` exactly once.

- [ ] **Step 2: Run endpoint tests**

Expected: FAIL because controller/security exception does not exist.

- [ ] **Step 3: Implement controller and narrow security exception**

Permit only `POST /internal/monitoring/refresh` through Spring Security, ignore CSRF only for that path, and enforce the secret in the controller using constant-time `MessageDigest.isEqual` on UTF-8 bytes. If the configured secret is blank, always return 403.

Do not alter CSRF protection for any existing form route.

- [ ] **Step 4: Re-run endpoint tests**

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: secure monitoring refresh endpoint`

---

### Task 7: Homepage integration without redesign

**Files:**
- Modify: `src/main/java/com/azizul/asenaki/web/HomeController.java`
- Modify: `src/main/resources/templates/home.html`
- Modify: `src/main/resources/static/css/style.css`

**Interfaces:**
- Home model adds: `powerStatus`, `internetStatus`, and `powerHistory`.
- No existing model attribute is removed or renamed.

- [ ] **Step 1: Add controller/template regression coverage**

Add/extend a Spring MVC test to assert `/` still returns `home`, existing `reports/utilities/areaCount` attributes remain, and monitoring attributes are present even when their optionals are empty.

- [ ] **Step 2: Run the homepage test**

Expected: FAIL because monitoring attributes are absent.

- [ ] **Step 3: Wire `MonitoringQueryService` into `HomeController`**

Only repository-backed query methods are called.

- [ ] **Step 4: Add one automatic-status section to `home.html`**

Insert it between the existing utility strip and the existing `#reports` section. Keep all existing hero/utility/report/how-it-works markup unchanged.

The section shows two cards:

- National Grid: demand, supply, load shedding, and source timestamp when data exists; otherwise `No recent automatic reading`.
- Internet: overall state, Cloudflare state, IODA state, summary, and checked time; otherwise `Live monitoring data is temporarily unavailable`.

Reuse current Bootstrap Icons and existing `status-pill` semantics. Add only additive CSS selectors such as `.monitoring-section`, `.monitoring-grid`, `.monitoring-card`, and `.monitoring-metrics`, using the existing CSS variables (`--navy`, `--green`, `--amber`, `--red`, `--line`, `--paper`, `--shadow`). Do not modify existing selector values.

- [ ] **Step 5: Run homepage and full MVC tests**

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: show automatic service status on homepage`

---

### Task 8: Free refresh automation and deployment config

**Files:**
- Create: `.github/workflows/monitoring-refresh.yml`
- Modify: `render.yaml`
- Modify: `README.md`

**Interfaces:**
- GitHub secret: `ASE_NAKI_REFRESH_URL`, containing the full endpoint URL such as `https://<render-service>/internal/monitoring/refresh`.
- GitHub secret: `MONITORING_REFRESH_SECRET`, matching Render's environment value.
- Render env: `CLOUDFLARE_API_TOKEN` is user-provided and optional.
- Render env: `MONITORING_REFRESH_SECRET` is generated/set securely.

- [ ] **Step 1: Add scheduled workflow**

Use:

```yaml
name: Refresh monitoring data
on:
  schedule:
    - cron: "*/30 * * * *"
  workflow_dispatch:
jobs:
  refresh:
    runs-on: ubuntu-latest
    steps:
      - name: Trigger monitoring refresh
        env:
          REFRESH_URL: ${{ secrets.ASE_NAKI_REFRESH_URL }}
          REFRESH_SECRET: ${{ secrets.MONITORING_REFRESH_SECRET }}
        run: |
          test -n "$REFRESH_URL"
          test -n "$REFRESH_SECRET"
          curl --fail --show-error --silent \
            --retry 2 --retry-delay 10 \
            -X POST "$REFRESH_URL" \
            -H "X-Monitoring-Secret: $REFRESH_SECRET"
```

- [ ] **Step 2: Extend `render.yaml`**

Add `CLOUDFLARE_API_TOKEN` with `sync: false` and `MONITORING_REFRESH_SECRET` with `generateValue: true`.

- [ ] **Step 3: Update README**

Document the three automatic sources, the monitoring endpoint, required GitHub secrets, optional Cloudflare token, 30-minute refresh, and the fact that the site still works when a provider is unavailable.

- [ ] **Step 4: Commit**

Commit message: `chore: automate free monitoring refresh`

---

### Task 9: Full verification

**Files:**
- No new feature files unless verification reveals a defect.

- [ ] **Step 1: Run the complete Maven build**

Run: `./mvnw --batch-mode verify`

Expected: all existing and new tests PASS.

- [ ] **Step 2: Run a production-style package build**

Run: `./mvnw --batch-mode -DskipTests package`

Expected: executable Spring Boot jar builds successfully.

- [ ] **Step 3: Inspect changed files against UI freeze**

Confirm existing hero, nav, utility strip, report cards, login/register pages, and report form markup/CSS were not redesigned. Only the new monitoring section and its additive CSS may differ visually.

- [ ] **Step 4: Inspect secrets/config**

Search the diff for real API tokens, database URLs, or refresh secrets. Expected: none are committed.

- [ ] **Step 5: Final commit if verification fixes were needed**

Commit message: `fix: finalize live monitoring integration`
