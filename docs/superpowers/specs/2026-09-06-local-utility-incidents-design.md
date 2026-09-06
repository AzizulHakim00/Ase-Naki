# Ase Naki Local Utility Incidents Design

Date: 2026-09-06
Status: Approved design, pending implementation-plan review

## 1. Product Goal

Ase Naki should answer the practical question a user has during a service problem:

> Is this problem only mine, or are other people nearby experiencing it too?

The current automatic national monitoring and community report features remain useful, but they are not enough by themselves. This iteration adds a local incident layer that turns repeated community signals into a current area-level utility status.

The project remains suitable for an undergraduate Spring Boot class project. It must stay explainable, testable, free to run, and honest about uncertainty.

## 2. Hard UI Constraint

The current UI is approved and must not be redesigned.

The implementation must preserve:

- the current navbar;
- the current hero section;
- the current homepage section order;
- the current color palette;
- typography;
- spacing system;
- existing report cards;
- existing status pills;
- existing button styles;
- login and registration pages;
- the current report form;
- the current automatic-monitoring section;
- the current responsive behavior.

`style.css` must not be globally redesigned. New functionality should appear inside existing cards, metadata rows, action rows, and existing page patterns wherever possible.

The goal is to improve usefulness and engagement through backend behavior and small data-driven additions, not by changing the visual identity.

## 3. Utility Scope

All five current utilities remain supported.

### Electricity

Receives the full live-incident experience:

- area-level active incidents;
- one-tap confirmations;
- freshness;
- confidence;
- restoration signals;
- incident history;
- national grid context shown separately.

### Internet

Receives the full live-incident experience:

- area-level active incidents;
- optional provider-aware incidents;
- one-tap confirmations;
- freshness;
- confidence;
- restoration signals;
- incident history;
- IODA/Cloudflare context shown separately.

### Gas and Water

Remain community-driven because there is no reliable verified neighborhood-level public automatic source available for the project. They use recent-community summaries and existing status types such as low pressure and maintenance. They must not pretend to have real-time automatic detection.

### Mobile Network

Uses a simpler incident model with provider awareness. Supported normalized providers should initially include:

- Grameenphone;
- Robi;
- Banglalink;
- Teletalk.

The system should remain extensible to additional providers without changing the core incident model.

## 4. Existing Features Preserved

The following existing features remain valid and continue to work:

- `Area` as the single location entity;
- `UtilityReport` as the detailed community-report object;
- evidence image upload;
- registration and login;
- Spring Security and CSRF protections;
- user/account/profile relationships;
- national Power Grid monitoring;
- IODA monitoring;
- optional Cloudflare Radar monitoring;
- Neon PostgreSQL deployment;
- Render deployment;
- GitHub Actions monitoring refresh;
- existing report-detail pages.

The new incident system extends the existing architecture rather than replacing it.

## 5. Core Architecture

The new local status layer is centered on:

```text
Area
  ↓
UtilityIncident
  ↓
IncidentSignal
```

A `UtilityIncident` represents one local service event for an area and utility, optionally scoped to a provider.

An `IncidentSignal` represents one signed-in user's current observation for that incident.

`UtilityReport` remains the richer optional evidence object. A report may optionally reference the incident it supports.

Automatic national monitoring remains a separate source of broader context and never becomes the local truth object.

## 6. UtilityIncident Data Model

Add a new entity similar to:

```text
UtilityIncident
- id
- area
- utilityType
- provider (nullable)
- state
- confidence
- firstSeenAt
- lastSignalAt
- resolvedAt (nullable)
- dismissed
```

### Relationships

- many incidents belong to one `Area`;
- one incident has many `IncidentSignal` rows;
- one incident may have many detailed `UtilityReport` rows;
- a provider is optional and stored as a normalized value where needed.

### Active-Incident Identity

For Electricity, the active key is:

```text
Area + UtilityType
```

For provider-aware Internet or Mobile incidents, the active key is:

```text
Area + UtilityType + Provider
```

The service must avoid creating duplicate active incidents for the same key.

## 7. IncidentSignal Data Model

Add an entity similar to:

```text
IncidentSignal
- id
- incident
- user
- signalType
- createdAt
- updatedAt
```

The supported signal types are:

```text
SAME_PROBLEM
WORKING_FOR_ME
STILL_OUT
RESTORED
```

The database must enforce:

```text
UNIQUE(incident, user)
```

A user therefore contributes at most one current signal to an incident. Tapping another action updates the existing signal instead of incrementing the total again.

## 8. User Profile / Preferred Area

Reuse the existing `Area` entity.

Extend `UserProfile` with an optional `preferredArea` relationship rather than creating a second location model.

Signed-in users may save one preferred area in this iteration.

The preferred area affects prioritization/query ordering but does not require a homepage redesign.

Guests may read all incidents and reports. One-tap signal submission requires authentication.

## 9. Incident State Model

Use a small explainable state enum, for example:

```text
POSSIBLE_ISSUE
LIKELY_OUTAGE
CONFIRMED_OUTAGE
MIXED_REPORTS
RESTORATION_REPORTED
RESOLVED
STALE
```

The UI-facing labels may use clearer wording while preserving the underlying enum semantics.

The state must always be computed from server-owned data. Users cannot submit incident state directly.

## 10. Confidence Model

Use categorical confidence rather than a fake statistical percentage:

```text
LOW
MEDIUM
HIGH
```

Confidence is based on unique recent users, agreement, contradiction, and freshness.

Initial deterministic rules:

```text
1 fresh unique affected user
→ POSSIBLE_ISSUE + LOW

2–3 fresh unique affected users agreeing
→ LIKELY_OUTAGE + MEDIUM

4+ fresh unique affected users,
with affected signals clearly dominating working signals
→ CONFIRMED_OUTAGE + HIGH

Meaningful disagreement between affected and working users
→ MIXED_REPORTS

2+ restoration signals after an outage
→ RESTORATION_REPORTED

Restoration/working signals dominate,
and no newer affected signal exists
→ RESOLVED

No fresh signal for 60 minutes
→ STALE
```

The exact implementation should keep these thresholds explicit constants so they are simple to explain and test.

Silence never means service is available.

## 11. Freshness Rules

A signal is considered fresh for 45 minutes.

Incident calculations should prioritize only fresh signals for the current state.

If no fresh signal remains for 60 minutes, the incident becomes `STALE` rather than automatically `RESOLVED`.

Historical records remain stored for later area-history views.

## 12. Internet Provider Handling

Internet provider is optional.

If provided, provider participates in the incident key.

Example:

```text
Dhanmondi + Internet + Link3
```

must stay separate from:

```text
Dhanmondi + Internet + BTCL
```

and from an area-wide Internet incident with no provider selected.

The implementation should normalize provider names to prevent obvious duplicate spelling variants where practical, but must not attempt to build a comprehensive Bangladesh ISP registry in this iteration.

## 13. Mobile Provider Handling

Mobile provider is required for provider-specific mobile incidents.

Initial normalized values:

```text
GRAMEENPHONE
ROBI
BANGLALINK
TELETALK
```

The data model should remain extensible.

## 14. UtilityReport Integration

`UtilityReport` remains the detailed community-report object.

Add an optional relationship from `UtilityReport` to `UtilityIncident`.

Detailed report creation should:

1. save the existing report exactly as today;
2. detect whether the utility/status represents a disruption or restoration;
3. attach to an existing compatible active incident when appropriate;
4. create a compatible incident when appropriate;
5. optionally create/update the reporter's signal so the detailed report also counts once in incident aggregation.

Image evidence remains optional and unchanged.

The existing report form must remain available and visually consistent with the current UI.

## 15. One-Tap Participation

For active incidents, authenticated users can submit one of the supported signal types.

One-tap actions must:

- use existing button styles;
- require authentication;
- use CSRF protection;
- update an existing user signal when present;
- recalculate incident state immediately;
- never allow the client to submit confidence or state directly;
- avoid duplicate counting.

The detailed report form remains the secondary path for descriptions and evidence.

## 16. Abuse and Duplicate Protection

The design intentionally avoids a complex reputation algorithm.

Use simple deterministic controls:

- one current signal per user per incident;
- server-owned timestamps;
- short cooldown between repeated signal changes;
- recent duplicate detailed reports from the same user/area/utility rejected or merged when clearly redundant;
- authenticated participation for signals;
- admin-only dismissal/moderation;
- no client-submitted confidence/state;
- no raw anonymous vote inflation.

A public leaderboard is explicitly out of scope because it would create incentives for spam.

## 17. Moderation

Existing admin authorization should be extended so an admin can:

- dismiss an obviously false incident;
- mark an incident resolved when necessary;
- preserve historical data rather than deleting the incident record outright.

Normal users must not access moderation actions.

Dismissed incidents should not appear in normal active-incident queries.

## 18. External Monitoring Relationship

National/automatic monitoring is supporting context only.

### Power Grid Bangladesh

National demand/supply/load-shedding data may be shown alongside local electricity incidents, but it must never automatically mark a local area as unavailable.

### IODA and Cloudflare Radar

Bangladesh-wide Internet anomaly signals may be shown alongside local Internet incidents, but they must never automatically mark an area/provider incident as confirmed.

The separation must remain explicit:

```text
Local incident
→ what nearby users are currently experiencing

Automatic external status
→ whether there may also be a broader national/network issue
```

## 19. Homepage Integration With UI Frozen

The current homepage structure remains unchanged.

The implementation may enrich existing cards and metadata with incident information such as:

```text
8 affected · 2 working · updated 4 min ago
```

or:

```text
Likely outage · Medium confidence
```

Existing status-pill and card-action patterns should be reused.

No new hero selector, no dashboard redesign, no section reordering, and no new global visual system are allowed in this iteration.

Preferred-area data may be used by the backend to prioritize relevant incidents within the existing content patterns.

## 20. Engagement Features Included

Engagement must support the real utility-status problem rather than social-media behavior.

Include:

- one-tap confirmations;
- preferred-area saving;
- incident freshness;
- incident timeline/history;
- restoration signals;
- recent area incident history;
- personal contribution history/count;
- optional quiet helpful-contributor indicator if it can be implemented without a complex scoring system;
- on-site restoration information when users revisit relevant incident/detail pages.

## 21. Engagement Features Explicitly Excluded

Do not add:

- public leaderboards;
- follower counts;
- chatrooms;
- unrestricted public comment threads;
- likes unrelated to utility status;
- gamified coins;
- streak pressure;
- fake AI predictions;
- paid push/SMS infrastructure;
- a new mobile app;
- WebSockets for this iteration.

The product should feel useful, not noisy.

## 22. Query / Service Layer

Add focused services rather than placing aggregation logic in controllers.

Suggested structure:

```text
incident
├── UtilityIncident.java
├── IncidentSignal.java
├── IncidentState.java
├── IncidentConfidence.java
├── IncidentSignalType.java
├── UtilityProvider.java (or equivalent normalization helper)
├── UtilityIncidentRepository.java
├── IncidentSignalRepository.java
├── IncidentAggregationService.java
├── IncidentService.java
└── IncidentQueryService.java
```

Exact names may follow existing package conventions.

### IncidentAggregationService

Responsible only for deterministic state/confidence calculation.

### IncidentService

Responsible for:

- find/create compatible active incident;
- submit/update signal;
- enforce cooldown/duplicate rules;
- recalculate incident;
- resolve/dismiss where authorized;
- link detailed reports where appropriate.

### IncidentQueryService

Responsible for current active incidents, preferred-area prioritization, counts, and recent incident history.

Controllers should stay thin.

## 23. Controller / Route Scope

Routes may include patterns similar to:

```text
POST /incidents/{id}/signals
POST /profile/preferred-area
POST /admin/incidents/{id}/dismiss
POST /admin/incidents/{id}/resolve
```

Exact route names may be adjusted to existing controller conventions.

All modifying user routes retain CSRF protection.

Signal and profile writes require authenticated users.

Admin routes require admin authorization.

## 24. Error Handling

- invalid incident id → normal not-found handling;
- stale/dismissed incident signal attempt → reject with a useful message;
- invalid provider → validation error;
- repeated signal inside cooldown → do not duplicate count;
- concurrent submissions must respect the database uniqueness constraint;
- aggregation failure must not corrupt existing incident data;
- automatic-source failure must not affect local incident submission.

The user-facing site should fail gracefully and preserve the existing UI conventions.

## 25. Data Migration / Compatibility

Hibernate `ddl-auto: update` is currently used, so new tables/columns can be introduced without deleting existing data.

Existing reports remain valid even when they have no incident reference.

The implementation should not attempt to retroactively convert every historical report into an incident unless a small deterministic bootstrap is clearly safe. Historical report migration is not required for the first version.

## 26. Required Tests

The implementation must add tests covering at least:

1. first affected signal creates/produces a possible incident;
2. duplicate user signal updates instead of double-counting;
3. 2–3 agreeing users produce medium confidence;
4. 4+ agreeing users produce high confidence;
5. contradictory affected/working reports produce mixed state;
6. restoration transition behavior;
7. silence/staleness produces `STALE`, not `RESOLVED`;
8. provider-specific Internet incidents remain separate;
9. Mobile provider normalization;
10. signal cooldown behavior;
11. duplicate detailed-report protection;
12. detailed report links to compatible incident where appropriate;
13. preferred-area prioritization;
14. admin moderation authorization;
15. normal users cannot call admin incident actions;
16. guests can read but not submit signals;
17. CSRF remains enabled for normal modifying routes;
18. existing report/auth/monitoring tests continue to pass;
19. existing homepage structure and CSS are not globally redesigned.

Tests should use local fixtures/mocks and must not depend on live external APIs.

## 27. Success Criteria

This iteration succeeds when:

- a real local outage can be represented as one shared incident instead of many disconnected posts;
- users can confirm or contradict an incident in one action;
- repeated taps from one account do not inflate counts;
- incident state is explainable from unique recent signals;
- stale data is never presented as confirmed availability;
- restoration can be represented and resolved cleanly;
- Electricity and Internet provide the strongest live experience;
- Gas, Water, and Mobile stay honest about their weaker data sources;
- national automatic monitoring remains contextual rather than pretending to be local truth;
- the current approved UI remains visually intact;
- existing detailed reports/evidence continue to work;
- all automated tests pass;
- the feature remains deployable on the current free Render + Neon stack.

## 28. Implementation Order

1. Add incident enums/entities/repositories and aggregation tests.
2. Implement aggregation logic with RED→GREEN tests.
3. Implement signal submission/update and cooldown protection.
4. Add provider normalization and provider-specific incident tests.
5. Add optional `preferredArea` to `UserProfile` and query prioritization.
6. Link detailed reports to incidents without breaking existing report flow.
7. Add admin moderation endpoints/security tests.
8. Wire incident metadata/actions into existing templates using existing styles only.
9. Add recent-history/contribution queries.
10. Run full Maven verification.
11. Review diff specifically for UI-freeze compliance.
12. Merge only after CI is green.
13. Deploy to the existing working Render service and verify the production behavior.

## 29. Non-Goals

This iteration does not attempt to provide:

- feeder-level electricity telemetry;
- true household availability detection;
- guaranteed provider outage feeds for every ISP/operator;
- predictive machine learning;
- GPS tracking;
- paid mapping;
- SMS alerts;
- push-notification infrastructure;
- live chat;
- IoT sensors;
- a native mobile app;
- a full moderation/reputation platform.

These are intentionally excluded to keep the system credible and appropriate for the project scope.
