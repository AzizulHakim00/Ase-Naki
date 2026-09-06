# Ase Naki Local Utility Incidents Design

Date: 2026-09-06
Status: Approved design, pending implementation-plan review

## 1. Product Goal

Ase Naki should answer the practical question a user has during a service problem:

> Is this problem only mine, or are other people nearby experiencing it too?

The existing national monitoring and community reports remain useful, but they do not by themselves answer that local question. This iteration adds a local incident layer that converts fresh, unique community observations into an explainable area-level service state.

The project must remain suitable for an undergraduate Spring Boot class project: explainable, testable, free to run, and honest about uncertainty.

## 2. Hard UI Constraint

The current UI is approved and frozen.

The implementation must preserve:

- current navbar;
- current hero section;
- current homepage section order;
- current color palette;
- typography;
- spacing system;
- existing report cards;
- existing status pills;
- existing button styles;
- login and registration pages;
- current report form;
- current automatic-monitoring section;
- current responsive behavior.

`style.css` must not be globally redesigned. New functionality should appear inside existing cards, metadata rows, action rows, and existing page patterns wherever possible.

No new hero selector, no dashboard redesign, no section reordering, and no new global visual system are allowed in this iteration.

## 3. Utility Scope

All five existing utilities remain supported.

### Electricity

Gets the full live-incident experience:

- area-level incidents;
- one-tap confirmations;
- confidence;
- freshness;
- restoration signals;
- incident history;
- Power Grid Bangladesh shown only as broader context.

### Internet

Gets the full live-incident experience:

- area-level incidents;
- optional provider-aware incidents;
- one-tap confirmations;
- confidence;
- freshness;
- restoration signals;
- incident history;
- IODA/Cloudflare shown only as broader context.

### Gas and Water

Remain community-driven. Existing states such as low pressure and maintenance stay valid. They may show recent-community summaries, but the application must not pretend they have neighborhood-level automatic live detection.

### Mobile Network

Uses the simpler incident model with provider awareness. Initial normalized providers:

```text
GRAMEENPHONE
ROBI
BANGLALINK
TELETALK
```

The model remains extensible.

## 4. Existing Features Preserved

Keep working:

- `Area` as the single location entity;
- `UtilityReport` as the detailed community-report object;
- image evidence upload;
- registration/login;
- Spring Security and CSRF;
- existing JPA relationships;
- Power Grid monitoring;
- IODA monitoring;
- optional Cloudflare Radar monitoring;
- Neon PostgreSQL;
- Render deployment;
- GitHub Actions monitoring refresh;
- current report/detail pages.

The new incident system extends the current architecture rather than replacing it.

## 5. Core Architecture

```text
Area
  ↓
UtilityIncident
  ↓
IncidentSignal
```

`UtilityIncident` represents one local utility event for an area and utility, optionally scoped to a provider.

`IncidentSignal` represents one signed-in user's current observation for that incident.

`UtilityReport` remains the richer optional evidence object and may reference an incident.

Automatic national monitoring remains separate and can never become local truth by itself.

## 6. UtilityIncident Data Model

Add an entity similar to:

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

Relationships:

- many incidents belong to one `Area`;
- one incident has many `IncidentSignal` rows;
- one incident may have many `UtilityReport` rows.

### Active incident identity

Electricity:

```text
Area + UtilityType
```

Provider-aware Internet or Mobile:

```text
Area + UtilityType + Provider
```

Internet without a provider uses a distinct area-wide key and must not be merged with provider-specific incidents.

The service must prevent duplicate active incidents for the same key.

## 7. IncidentSignal Data Model

Add:

```text
IncidentSignal
- id
- incident
- user
- signalType
- createdAt
- updatedAt
```

Signal types:

```text
SAME_PROBLEM
WORKING_FOR_ME
STILL_OUT
RESTORED
```

Database constraint:

```text
UNIQUE(incident, user)
```

One user therefore contributes at most one current signal to an incident. A new tap updates that user's existing row instead of increasing the unique-user count.

Signal meaning for aggregation:

```text
Affected signals:
SAME_PROBLEM, STILL_OUT

Non-affected signal:
WORKING_FOR_ME

Recovery signal:
RESTORED
```

## 8. Preferred Area

Reuse the existing `Area` entity.

Add an optional `preferredArea` relationship to `UserProfile`.

A signed-in user may save exactly one preferred area in this iteration. It changes query prioritization only and does not require a homepage redesign.

Guests can read incidents and reports. Signal submission and preferred-area writes require authentication.

## 9. Incident State and Confidence

States:

```text
POSSIBLE_ISSUE
LIKELY_OUTAGE
CONFIRMED_OUTAGE
MIXED_REPORTS
RESTORATION_REPORTED
RESOLVED
STALE
```

Confidence:

```text
LOW
MEDIUM
HIGH
```

Users never submit state or confidence directly. The server calculates both from current stored signals.

## 10. Freshness Constants

Use explicit constants:

```text
FRESH_SIGNAL_WINDOW = 45 minutes
STALE_INCIDENT_AFTER = 60 minutes
SIGNAL_CHANGE_COOLDOWN = 2 minutes
DUPLICATE_DETAILED_REPORT_WINDOW = 10 minutes
```

A signal older than 45 minutes does not participate in current-state voting.

If an unresolved incident has had no fresh signal for 60 minutes, it becomes `STALE`, not `RESOLVED`.

Silence never means the utility is working.

## 11. Deterministic Aggregation Rules

Calculate using fresh unique-user signals only.

Let:

```text
A = number of affected users
W = number of WORKING_FOR_ME users
R = number of RESTORED users
```

For recency comparisons, use each signal's `updatedAt`.

Apply rules in this order:

### 11.1 Stale

If the incident has no fresh signal and `lastSignalAt` is at least 60 minutes old:

```text
STALE + LOW
```

### 11.2 Restoration reported

If the incident previously reached `LIKELY_OUTAGE`, `CONFIRMED_OUTAGE`, or `MIXED_REPORTS`, and:

```text
R >= 2
```

then:

```text
RESTORATION_REPORTED
```

unless a newer affected signal exists after the newest recovery signal.

Confidence is `MEDIUM` when `R` is 2–3 and `HIGH` when `R >= 4`.

### 11.3 Resolved

An incident becomes `RESOLVED` when all are true:

```text
R + W >= 3
R + W > A
no affected signal is newer than the newest RESTORED or WORKING_FOR_ME signal
```

Set `resolvedAt` when this transition occurs.

### 11.4 Mixed reports

If:

```text
A >= 2
W >= 2
```

and neither side is at least twice the other side, then:

```text
MIXED_REPORTS + MEDIUM
```

### 11.5 Confirmed outage

If:

```text
A >= 4
A >= 2 * max(W, 1)
```

then:

```text
CONFIRMED_OUTAGE + HIGH
```

### 11.6 Likely outage

If:

```text
A is 2 or 3
A > W
```

then:

```text
LIKELY_OUTAGE + MEDIUM
```

### 11.7 Possible issue

If:

```text
A >= 1
```

but no stronger rule matches:

```text
POSSIBLE_ISSUE + LOW
```

### 11.8 Working-only observations

Fresh `WORKING_FOR_ME` observations without any affected signal do not create a new outage incident. They may be retained only when attached to an already-existing incident.

This prevents the application from inventing a healthy incident from silence or isolated positive reports.

## 12. Internet Provider Handling

Internet provider is optional.

If supplied, normalize by trimming whitespace and using a case-insensitive canonical value. Provider-specific incidents remain separate from area-wide incidents.

Examples that must remain separate:

```text
Dhanmondi + Internet + Link3
Dhanmondi + Internet + BTCL
Dhanmondi + Internet + no provider
```

Do not build a complete national ISP registry in this iteration.

## 13. Mobile Provider Handling

Mobile incidents use the normalized enum values:

```text
GRAMEENPHONE
ROBI
BANGLALINK
TELETALK
```

Provider is required when creating a provider-specific mobile incident.

## 14. Detailed Report Integration

Keep the existing `UtilityReport` flow and add an optional relationship from report to incident.

For Electricity and Internet detailed reports:

- `UNAVAILABLE` or `UNSTABLE` maps to an affected signal;
- `AVAILABLE` maps to `WORKING_FOR_ME` when a compatible active incident already exists;
- `AVAILABLE` by itself does not create a new incident;
- `MAINTENANCE` may attach to a compatible incident as detailed context but does not automatically count as an affected signal unless the submitted status also represents actual unavailability;
- one detailed report from a user can create/update at most one signal for the compatible incident.

For Gas and Water, keep the current detailed-report behavior and recent-community summary; do not force all status types into the full outage aggregation model.

For Mobile, provider is used when matching or creating the compatible incident.

Image evidence stays unchanged.

## 15. Duplicate Detailed-Report Rule

Reject a new detailed report when all are true:

```text
same authenticated user
same area
same utility
same provider scope where applicable
submitted within the previous 10 minutes
same status
```

Return a useful validation message instead of saving another duplicate row.

A different status inside that window is allowed because it may represent a real change, such as restoration.

## 16. One-Tap Participation

Authenticated users can submit one of the supported signal types on an active incident.

Requirements:

- reuse current button styles;
- retain CSRF protection;
- update the user's existing incident signal when present;
- reject signal changes made less than 2 minutes after the previous change, except an immediate `RESTORED` transition is allowed;
- recalculate incident state after every accepted change;
- never accept client-submitted state, confidence, counts, or timestamps;
- stale, resolved, or dismissed incidents reject normal signal submissions.

The detailed report form remains available for descriptions and evidence.

## 17. Abuse Protection

Use deterministic controls only:

- one current signal per user per incident;
- authenticated signal participation;
- server timestamps;
- 2-minute signal-change cooldown;
- 10-minute duplicate detailed-report rule;
- database uniqueness constraint;
- admin-only moderation;
- no anonymous voting;
- no client-owned confidence or incident state.

Do not add a public leaderboard or complicated reputation score.

## 18. Moderation

Admins may:

- dismiss a false incident;
- mark an incident resolved;
- preserve the record for history rather than deleting it.

Normal users cannot access moderation actions.

Dismissed incidents do not appear in active-incident queries.

## 19. External Monitoring Relationship

Automatic sources are context only.

### Power Grid Bangladesh

National demand, supply, and load-shedding data may accompany local Electricity information but must never automatically mark Mirpur, Dhanmondi, or any other area as unavailable.

### IODA and Cloudflare Radar

Bangladesh-level Internet signals may accompany local Internet information but must never automatically confirm an area/provider incident.

The conceptual separation is fixed:

```text
Local incident
→ what nearby users report now

Automatic external status
→ whether a broader problem may also exist
```

## 20. Homepage Integration With UI Frozen

The current homepage layout stays unchanged.

Existing cards may gain metadata such as:

```text
8 affected · 2 working · updated 4 min ago
```

Existing status pills may show:

```text
Likely outage
Confirmed outage
Mixed reports
Restoration reported
Stale
```

Existing action areas may use current button classes for one-tap actions.

Preferred-area data may prioritize matching incident/report cards through backend ordering only.

No section reordering or new homepage visual system is permitted.

## 21. Engagement Features Included

Include only engagement tied to utility usefulness:

- one-tap confirmations;
- preferred-area saving;
- freshness labels;
- incident timeline/history;
- restoration signals;
- recent area incident history;
- personal contribution count/history;
- a simple quiet “helpful contributor” indicator only if it can be derived from corroborated contributions without a scoring subsystem;
- restoration information on subsequent visits to relevant incident/detail pages.

If the helpful-contributor indicator would require a new scoring/reputation subsystem, omit it from this iteration.

## 22. Engagement Features Excluded

Do not add:

- public leaderboards;
- followers;
- chatrooms;
- unrestricted comments;
- unrelated likes/reactions;
- coins;
- streak pressure;
- fake AI prediction;
- paid SMS/push infrastructure;
- a mobile app;
- WebSockets.

## 23. Service Boundaries

Suggested package:

```text
incident
├── UtilityIncident.java
├── IncidentSignal.java
├── IncidentState.java
├── IncidentConfidence.java
├── IncidentSignalType.java
├── UtilityProvider.java
├── UtilityIncidentRepository.java
├── IncidentSignalRepository.java
├── IncidentAggregationService.java
├── IncidentService.java
└── IncidentQueryService.java
```

Exact names may follow current package conventions.

### IncidentAggregationService

Pure deterministic calculation from incident/signal data. It must not perform HTTP calls or controller work.

### IncidentService

Responsible for:

- find/create compatible active incident;
- submit/update signal;
- enforce cooldown and duplicate rules;
- recalculate state;
- link detailed reports;
- moderation state transitions when authorized.

### IncidentQueryService

Responsible for:

- active incident queries;
- preferred-area prioritization;
- counts;
- recent history;
- personal contribution summaries.

Controllers remain thin.

## 24. Routes

Expected route patterns may include:

```text
POST /incidents/{id}/signals
POST /profile/preferred-area
POST /admin/incidents/{id}/dismiss
POST /admin/incidents/{id}/resolve
```

Exact names may follow existing controller conventions.

All normal modifying routes retain CSRF.

Signal/profile writes require authentication. Admin routes require admin role.

## 25. Error Handling

- invalid incident id → normal not-found behavior;
- stale/resolved/dismissed signal attempt → reject with useful message;
- invalid provider → validation error;
- cooldown violation → do not modify or duplicate the signal;
- concurrent duplicate signal submissions → database uniqueness remains final protection;
- aggregation failure → transaction rolls back;
- automatic-source failure → local incident submission remains available.

## 26. Data Compatibility

The current application uses Hibernate `ddl-auto: update`, so new tables/columns can be introduced without deleting current data.

Existing reports remain valid with `incident_id = null`.

Do not retroactively convert all historical reports into incidents in the first implementation. New incidents begin from new activity after deployment.

## 27. Required Tests

Add tests for at least:

1. one affected signal → possible/low;
2. duplicate user signal updates, not double-counts;
3. 2–3 agreeing affected users → likely/medium;
4. 4+ affected with 2:1 dominance → confirmed/high;
5. 2+ affected and 2+ working without 2:1 dominance → mixed;
6. 2+ recovery signals → restoration reported when no newer affected signal exists;
7. recovery/working dominance → resolved under the defined rule;
8. 60-minute silence → stale, not resolved;
9. working-only observation does not create a new outage incident;
10. provider-specific Internet incidents remain separate;
11. mobile provider normalization;
12. 2-minute signal cooldown;
13. immediate restored transition exception;
14. 10-minute duplicate detailed-report rejection;
15. changed status inside duplicate window is allowed;
16. detailed report links to compatible incident;
17. available detailed report does not create an outage incident by itself;
18. preferred-area prioritization;
19. admin moderation authorization;
20. normal user cannot moderate;
21. guest cannot submit signal;
22. CSRF remains enabled for user writes;
23. existing report/auth/monitoring tests continue to pass;
24. homepage structure and CSS are not globally redesigned.

Provider and monitoring tests must use fixtures/mocks, not live external APIs.

## 28. Success Criteria

This iteration succeeds when:

- repeated local reports become one shared incident instead of disconnected posts;
- users can confirm, contradict, or report restoration in one action;
- one account cannot inflate counts by repeated tapping;
- current state is explainable from unique fresh signals;
- contradictory data is visible rather than hidden;
- stale data is never presented as availability;
- Electricity and Internet provide the strongest local live experience;
- Gas, Water, and Mobile remain honest about weaker data sources;
- national automatic monitoring stays contextual;
- current approved UI remains visually intact;
- current detailed reports and evidence still work;
- full tests pass;
- the current free Render + Neon deployment remains viable.

## 29. Implementation Order

1. Add incident enums/entities/repositories and failing aggregation tests.
2. Implement deterministic aggregation RED→GREEN.
3. Implement incident creation and one-signal-per-user updates.
4. Add cooldown and duplicate-report protection.
5. Add provider normalization/separation.
6. Add `preferredArea` and query prioritization.
7. Integrate `UtilityReport` with incidents.
8. Add moderation endpoints/security tests.
9. Add incident metadata/actions to existing templates with existing styles only.
10. Add history/contribution queries.
11. Run full Maven verification.
12. Review diff specifically for UI-freeze compliance.
13. Merge only after CI is green.
14. Deploy to the existing working Render service and verify production behavior.

## 30. Non-Goals

Do not build in this iteration:

- feeder-level electricity telemetry;
- household-level automatic availability detection;
- guaranteed provider feeds for every ISP/operator;
- predictive ML;
- GPS tracking;
- paid maps;
- SMS alerts;
- push-notification infrastructure;
- live chat;
- IoT sensors;
- native mobile app;
- full moderation/reputation platform.
