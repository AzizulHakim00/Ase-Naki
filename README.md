# Ase Naki?

**Ase Naki?** is a Spring Boot class project for checking electricity, gas, water, broadband, and mobile-network conditions in Bangladesh. It combines automatic public service-status signals with local community reports so the application remains useful even when nobody submits a new report.

![Ase Naki app icon](src/main/resources/static/images/app-icon.png)

## What the application monitors

The project keeps the original community-report workflow and adds a small automatic monitoring layer:

- **Power Grid Bangladesh PLC**: national electricity demand, supply, and reported load shedding.
- **IODA (Georgia Tech)**: recent Internet outage alerts related to Bangladesh.
- **Cloudflare Radar**: recent Bangladesh Internet outage annotations when a free Radar API token is configured.
- **Community reports**: local electricity, gas, water, broadband, and mobile-network updates from signed-in users.

A normal homepage request reads the most recent saved monitoring snapshot from the database. It does not wait for external APIs, so a slow or unavailable provider does not make the homepage slow or unavailable.

## Why this version is easy to follow

The community feature still follows the familiar flow:

```text
HTML form -> Controller -> Service -> Repository -> Database
```

Automatic monitoring follows a similarly simple flow:

```text
Free public source -> Monitoring service -> Repository -> Database -> Homepage
```

The project intentionally avoids IoT devices, machine learning, paid APIs, SMS, WebSockets, complex moderation, and feeder-level electricity prediction.

## Main features

- Spring Boot MVC and Thymeleaf
- Spring Data JPA repositories
- Spring Validation with `@Valid`, `BindingResult`, and normal validation annotations
- Database registration and login with Spring Security
- BCrypt password hashing
- Optional JPG, PNG, or WebP evidence upload up to 5 MB
- Uploaded images saved inside the database
- Automatic national power-status collection
- Automatic Internet-status checks using IODA and optional Cloudflare Radar
- Persisted monitoring snapshots with graceful unavailable states
- Protected monitoring-refresh endpoint for GitHub Actions
- H2 database for local classroom use
- Neon PostgreSQL for the deployed application
- Responsive Bootstrap interface and Bootstrap Icons
- Docker, Render Blueprint, and GitHub Actions

## Simple package guide

```text
com.azizul.asenaki
├── config       Security, database connection, and sample data
├── location     Area entity and repository
├── monitoring   Automatic status sources, snapshots, aggregation, and refresh
├── report       Report entity, form, repository, and service
├── user         User entity, registration, authentication, and service
└── web          Small MVC/controllers, including the refresh endpoint
```

## Database relationships

The project demonstrates the three requested JPA relationships:

```text
One-to-one
UserAccount 1 -------- 1 UserProfile

One-to-many / many-to-one
UserAccount 1 -------- * UtilityReport
Area        1 -------- * UtilityReport
UtilityReport 1 ------ * ReportEvidence
```

- `@OneToOne`: one user has one profile.
- `@OneToMany`: one user or area can have many reports; one report can have many images.
- `@ManyToOne`: many reports belong to one user and one area; many images belong to one report.

Monitoring snapshots are independent time-series records and do not change the classroom relationship examples above.

## Run locally

Requirement: Java 21 or newer.

Windows:

```powershell
./mvnw.cmd spring-boot:run
```

macOS or Linux:

```bash
./mvnw spring-boot:run
```

Open `http://localhost:8080`.

The application starts even when no Cloudflare token or refresh secret is configured. In that case Cloudflare monitoring is shown as unavailable and the protected refresh endpoint rejects requests until a secret is configured.

## Demo login

| Email | Password |
|---|---|
| `demo@asenaki.bd` | `Demo123!` |

## Important pages

| Page | URL | Access |
|---|---|---|
| Home, automatic status, and report list | `/` | Public |
| Register | `/register` | Public |
| Sign in | `/login` | Public |
| Report details | `/reports/{id}` | Public |
| Add report and image | `/reports/add` | Signed-in user |
| Health check | `/actuator/health` | Public |
| Monitoring refresh | `/internal/monitoring/refresh` | Secret-protected automation |

Spring Security automatically adds CSRF protection to normal changing form requests. CSRF is ignored only for the monitoring automation endpoint, which requires its own secret header.

## Automatic monitoring behavior

### Power Grid Bangladesh

`PowerGridService` reads the newest usable row from the public Power Grid Bangladesh demand/supply/load-shedding table and stores only the structured values:

```text
observed time
demand MW
supply MW
load-shedding MW
source
```

Raw HTML is not stored. If the page becomes unavailable or its structure changes, the application keeps the last successful database snapshot instead of inventing values.

### IODA

The application checks IODA's public v2 outage-alert endpoint for signals related to `country/BD` in a recent time window.

- Successful response with no critical alert -> `NORMAL`
- One or more critical alerts -> `POSSIBLE_DISRUPTION`
- Unavailable/unreadable response -> `UNAVAILABLE`

### Cloudflare Radar

Cloudflare Radar is optional because its outage API uses an API token. When `CLOUDFLARE_API_TOKEN` is configured, Ase Naki checks recent Bangladesh outage annotations.

- No active Bangladesh outage annotation -> `NORMAL`
- One or more active annotations -> `POSSIBLE_DISRUPTION`
- Missing token/API failure -> `UNAVAILABLE`

The Cloudflare token is never committed to the repository.

### Combined Internet state

The aggregation remains simple and explainable:

```text
Cloudflare normal + IODA normal
-> NORMAL

One source detects disruption
-> POSSIBLE_DISRUPTION

Both sources detect disruption
-> LIKELY_DISRUPTION

Both sources unavailable
-> UNAVAILABLE
```

If one source is unavailable and the other explicitly reports normal, the page clearly states that only the available source was checked. Provider failure is never silently treated as a healthy signal.

## Manually refresh monitoring data

Set a local secret before starting the app:

Windows PowerShell:

```powershell
$env:MONITORING_REFRESH_SECRET="replace-with-a-long-random-secret"
```

macOS/Linux:

```bash
export MONITORING_REFRESH_SECRET="replace-with-a-long-random-secret"
```

Then call:

```bash
curl -X POST http://localhost:8080/internal/monitoring/refresh \
  -H "X-Monitoring-Secret: replace-with-a-long-random-secret"
```

A successful refresh returns HTTP `204 No Content`.

## Run tests

```bash
./mvnw verify
```

Tests cover application startup, form validation, database behavior, monitoring aggregation, provider parsing, refresh persistence, and refresh-secret validation. Provider tests use local fixtures and do not require live Internet access.

## Deploy with Render and Neon for $0/month

The repository includes `Dockerfile`, `render.yaml`, and a scheduled GitHub Actions monitoring workflow.

1. Create a Neon PostgreSQL project and copy its pooled connection string.
2. Create/update the Render service from this repository.
3. Set `DATABASE_URL` in Render.
4. Set `CLOUDFLARE_API_TOKEN` in Render if you want Cloudflare Radar monitoring. The rest of the application works without it.
5. Set a long random value for `MONITORING_REFRESH_SECRET` in Render. If the Blueprint generated one, copy that generated value.
6. Wait for `/actuator/health` to return `UP`.
7. In GitHub repository Actions secrets, add:
   - `ASE_NAKI_REFRESH_URL` = `https://YOUR-RENDER-SERVICE/internal/monitoring/refresh`
   - `MONITORING_REFRESH_SECRET` = exactly the same secret used by Render.
8. Run the **Refresh monitoring data** workflow manually once to verify the setup.

After the workflow is on the default branch, GitHub Actions triggers the endpoint approximately every 30 minutes. This wakes a sleeping Render Free web service, refreshes external monitoring data, saves successful snapshots to Neon, and then allows the service to sleep again when unused.

`DatabaseConfig` accepts Neon's normal `postgresql://user:password@host/database` connection string and converts it to the JDBC format used by Spring Boot.

## Required and optional environment values

| Variable | Required? | Purpose |
|---|---|---|
| `DATABASE_URL` | Yes in production | Neon PostgreSQL connection |
| `MONITORING_REFRESH_SECRET` | Yes for automatic refresh | Protects the refresh endpoint |
| `CLOUDFLARE_API_TOKEN` | No | Enables Cloudflare Radar outage checks |
| `APP_ADMIN_PASSWORD` | Existing deployment setting | Seed/admin account password |

No paid API or paid hosting service is required for the minimum project.

## Responsible use

This is a classroom community-information project, not an emergency service. Automatic sources may be delayed, incomplete, or temporarily unavailable. National grid statistics do not prove that electricity is available in a particular building or street. Internet signals also do not guarantee the status of every local ISP connection.

Do not upload private documents, phone numbers, bills, or recognisable faces as evidence.
