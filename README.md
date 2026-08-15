# Ase Naki?

**Ase Naki?** is a simple Spring Boot class project for checking electricity, gas, water, broadband, and mobile-network conditions in Bangladesh.

![Ase Naki app icon](src/main/resources/static/images/app-icon.png)

## Why this version is easy to follow

The project follows the same familiar flow as the supplied class examples:

```text
HTML form -> Controller -> Service -> Repository -> Database
```

Each class has one clear job. The project does not use advanced voting, notification, moderation, audit, or custom-validator systems.

## Main features

- Spring Boot MVC and Thymeleaf
- Spring Data JPA repositories
- Spring Validation with `@Valid`, `BindingResult`, and normal validation annotations
- Database registration and login with Spring Security
- BCrypt password hashing
- Optional JPG, PNG, or WebP evidence upload up to 5 MB
- Uploaded images saved inside the database
- H2 database for local classroom use
- Neon PostgreSQL for the deployed application
- Responsive Bootstrap interface and Bootstrap Icons
- Docker, Render Blueprint, and GitHub Actions

## Simple package guide

```text
com.azizul.asenaki
├── config      Security, database connection, and sample data
├── location    Area entity and repository
├── report      Report entity, form, repository, and service
├── user        User entity, registration, authentication, and service
└── web         Small MVC controllers
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

The five main tables are `app_users`, `app_user_profiles`, `app_areas`, `app_reports`, and `app_report_files`.

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

## Demo login

| Email | Password |
|---|---|
| `demo@asenaki.bd` | `Demo123!` |

## Important pages

| Page | URL | Access |
|---|---|---|
| Home and report list | `/` | Public |
| Register | `/register` | Public |
| Sign in | `/login` | Public |
| Report details | `/reports/{id}` | Public |
| Add report and image | `/reports/add` | Signed-in user |
| Health check | `/actuator/health` | Public |

Spring Security automatically adds CSRF protection to changing form requests.

## Validation example

The controllers use the same style as the Product Shop example:

```java
@PostMapping("/add")
public String submit(
        @Valid @ModelAttribute("form") ReportForm form,
        BindingResult result) {
    if (result.hasErrors()) {
        return "reports/form";
    }

    reportService.saveReport(form, email);
    return "redirect:/";
}
```

## Run tests

```bash
./mvnw verify
```

The tests check application startup, form validation, and Neon connection-string conversion.

## Deploy with Render and Neon

The repository includes `Dockerfile` and `render.yaml`.

1. Create a Neon PostgreSQL project and copy its pooled connection string.
2. Create a Render Blueprint from this GitHub repository.
3. Enter the Neon connection string for `DATABASE_URL`.
4. Wait for `/actuator/health` to return `UP`.

`DatabaseConfig` accepts Neon's normal `postgresql://user:password@host/database` connection string and converts it to the JDBC format used by Spring Boot.

## Responsible use

This is a classroom community-information project, not an emergency service. Do not upload private documents, phone numbers, bills, or recognisable faces as evidence.
