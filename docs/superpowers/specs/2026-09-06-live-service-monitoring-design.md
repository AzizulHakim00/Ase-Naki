# Ase Naki? Live Service Monitoring Design

Date: 2026-09-06

## Goal

Extend the existing Ase Naki? Spring Boot project so it remains useful even when no community member submits a report. The application will automatically collect a small set of free public service-status signals for Bangladesh while preserving the current community-report workflow and the current visual design.

The project remains an undergraduate-scale application. It must stay understandable, testable, deployable on free tiers, and easy to explain in a viva.

## Non-goals

This iteration will not add IoT devices, machine learning, paid APIs, SMS, WebSockets, feeder-level electricity prediction, nationwide utility-provider integrations, Google Maps, complex moderation, or a mobile application.

It will not redesign the existing homepage, navigation, report cards, typography, spacing system, color palette, hero, login/register pages, or report forms.

## Existing application preserved

The current Spring Boot MVC architecture remains intact:

- Thymeleaf frontend
- Bootstrap and the existing custom CSS
- Spring Security authentication
- JPA repositories
- PostgreSQL on production and H2 locally
- existing user, area, utility-report, and evidence entities
- existing community report creation and report-details pages
- Render deployment and Actuator health check

The existing UI is treated as approved and frozen. New status data will reuse existing visual patterns and CSS rather than introduce a new visual system.

## Automatic data sources

### 1. Cloudflare Radar

Purpose: obtain automatic internet/network outage information relevant to Bangladesh.

The integration is isolated behind a `CloudflareRadarClient`/service. Authentication is read from an environment variable, never committed to the repository. If no token is configured, Cloudflare monitoring is reported as unavailable rather than causing application startup failure.

### 2. IODA

Purpose: provide an independent internet-outage signal so Ase Naki? does not depend on one provider.

The integration is isolated behind an `IodaClient`/service. API timeouts, unavailable responses, and unexpected payloads must be handled without failing the homepage.

### 3. Power Grid Bangladesh

Purpose: display national electricity demand, supplied power, and load-shedding values from the public Power Grid Bangladesh data page.

Because a stable public JSON API cannot be assumed, the implementation will fetch the public page at a low frequency and parse only the latest required values. Parsing is isolated behind a `PowerGridService` so a future API can replace the parser without changing the rest of the application.

The parser must fail closed: if the page layout changes, the application keeps the previous successful snapshot and marks the data stale/unavailable instead of displaying guessed values.

## Data model

### PowerSnapshot

A small JPA entity stores successful power observations:

- id
- observedAt/source timestamp when available
- fetchedAt
- demandMw
- supplyMw
- loadSheddingMw
- source name

Only structured numeric/text fields are stored. Raw HTML is not stored.

### InternetStatusSnapshot

A small JPA entity stores the combined result of the internet checks:

- id
- checkedAt
- cloudflareState
- iodaState
- overallState
- short summary
- optional affected provider/network text when available

The overall state remains intentionally simple and explainable:

- both healthy -> NORMAL
- one source indicates disruption -> POSSIBLE_DISRUPTION
- both sources indicate disruption -> LIKELY_DISRUPTION
- sources unavailable -> UNKNOWN/STALE as appropriate

No machine-learning confidence score is introduced.

## Service boundaries

The new code is separated into focused packages so existing report/user code remains untouched as much as possible.

Suggested structure:

```text
com.azizul.asenaki
├── monitoring
│   ├── model
│   │   ├── PowerSnapshot.java
│   │   ├── InternetStatusSnapshot.java
│   │   └── MonitoringState.java
│   ├── repository
│   │   ├── PowerSnapshotRepository.java
│   │   └── InternetStatusSnapshotRepository.java
│   ├── external
│   │   ├── CloudflareRadarService.java
│   │   ├── IodaService.java
│   │   └── PowerGridService.java
│   ├── MonitoringAggregationService.java
│   └── MonitoringRefreshService.java
```

Exact class names may be adjusted to match existing code style, but responsibilities remain separated.

## Data flow

```text
Cloudflare Radar ----\
                      \
IODA -----------------> MonitoringRefreshService -> normalized snapshots -> database
                      /
Power Grid Bangladesh/

Database -> HomeController -> existing Thymeleaf homepage
```

The homepage never waits for all external providers during a normal user request. It reads the most recent persisted snapshot so normal page loading remains fast even if an external service is slow or down.

## Refresh strategy on free hosting

Render Free may suspend an inactive web service, so an in-process `@Scheduled` method is not the only refresh mechanism.

A protected refresh endpoint will be added for automation, for example:

```text
POST /internal/monitoring/refresh
```

The request must include a secret token from an environment variable. The token is compared server-side and is never exposed in HTML or committed to GitHub.

A GitHub Actions scheduled workflow will call the endpoint periodically. A 30-minute cadence is the default target because it is sufficient for an undergraduate monitoring dashboard and avoids unnecessary external requests.

The refresh operation is idempotent enough for retries and handles each provider independently: failure of one provider does not discard successful data from the others.

## Homepage integration and UI preservation

The current UI is not redesigned.

The existing hero, utility strip, report cards, navigation, authentication pages, report form, and CSS style remain unchanged.

The homepage receives additional model attributes for the latest power and internet status. The automatic status information will be rendered using the project's existing card/pill/section visual language and existing typography. No new framework, theme, navigation concept, or major layout system is introduced.

Community reports remain visible and continue to work exactly as before. Automatic monitoring supplements the community information; it does not replace it.

If there is no current automatic data, the UI uses a restrained state such as "Live monitoring data is temporarily unavailable" or "No recent automatic reading" instead of fabricated values.

## Error handling and stale data

External services are untrusted dependencies.

Required behavior:

1. Apply reasonable connect/read timeouts.
2. Catch provider-specific failures at the integration boundary.
3. Log concise diagnostics without secrets.
4. Preserve the latest successful snapshot.
5. Store/update a last-checked timestamp.
6. Mark stale/unavailable data clearly.
7. Never make homepage rendering depend on a successful live API call.
8. Never infer "normal" merely because a provider request failed.

## Security

- Cloudflare token is stored only in an environment variable.
- Refresh endpoint secret is stored only in an environment variable.
- Normal users cannot call the protected refresh operation successfully without the token.
- Existing Spring Security/CSRF behavior for user forms stays intact.
- External error messages shown to users are generic; raw provider responses and secrets are not exposed.

## Free-tier deployment

The deployment remains based on the current free stack:

- Render Free web service
- Neon PostgreSQL Free
- GitHub public repository and GitHub Actions
- Cloudflare Radar free API access
- IODA public API
- Power Grid Bangladesh public data

No paid dependency is required for the minimum implementation.

## Image storage

The current evidence upload feature is not removed in this iteration because it is part of the existing approved project behavior. To avoid unrelated scope and UI changes, image storage remains unchanged initially.

A later optimization may reduce the upload limit or move evidence out of PostgreSQL if database usage becomes material, but this is explicitly outside the monitoring implementation.

## Tests

Implementation follows test-driven development for the new behavior.

Minimum automated coverage:

- Power Grid parser accepts a representative valid response.
- Power Grid parser rejects/makes unavailable malformed or changed markup without inventing values.
- Internet aggregation produces NORMAL when both sources are normal.
- Internet aggregation produces POSSIBLE_DISRUPTION when one source detects a problem.
- Internet aggregation produces LIKELY_DISRUPTION when both detect a problem.
- Provider failure produces UNKNOWN/STALE rather than NORMAL.
- Monitoring refresh persists successful snapshots.
- Refresh endpoint rejects an incorrect/missing secret.
- Existing form-validation and application-startup tests continue to pass.

External API tests use fixtures/mocks and do not depend on live internet access during Maven test execution.

## Success criteria

The iteration is complete when:

1. Existing pages and visual design still look and behave as before.
2. Existing community reports still work.
3. The application can automatically collect and persist national power data.
4. The application can collect internet-status signals from Cloudflare Radar when configured and IODA.
5. The homepage can display the latest automatic status without performing provider calls during the request.
6. Failures show stale/unavailable states instead of crashing or fabricating data.
7. A protected refresh endpoint exists.
8. A GitHub Actions schedule can trigger the refresh on Render Free.
9. Maven tests pass.
10. No paid service is required for the minimum version.

## Implementation order

1. Add monitoring enums/entities/repositories and tests.
2. Add Power Grid parser/service and parser tests.
3. Add IODA client/service and tests.
4. Add Cloudflare Radar client/service with optional token configuration and tests.
5. Add aggregation and refresh services with tests.
6. Add secure refresh endpoint and security tests.
7. Wire latest snapshots into `HomeController`.
8. Add minimal status markup using the existing homepage design language, without redesigning existing UI.
9. Add GitHub Actions scheduled refresh workflow and required configuration documentation.
10. Run the full Maven test suite and verify deployment configuration.
