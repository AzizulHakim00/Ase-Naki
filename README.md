# Ase Naki? - MongoDB CRUD branch

This branch converts the existing Spring Boot application from JPA/PostgreSQL/H2 to **MongoDB Atlas** and removes the external monitoring APIs so the project stays simple and focused on CRUD.

## What this version includes

- Spring Boot 4.1 + Java 21
- Spring Data MongoDB
- MongoDB collections for users, areas, and utility reports
- User registration and login with Spring Security
- Demo user and demo admin credentials shown directly on the login page
- Create, read, update, and delete operations for utility reports
- Admin dashboard for user status management and access to all reports
- Dockerfile for Render deployment
- Image upload intentionally disabled in this version; it can be added later

## Demo accounts

- User: `demo@asenaki.bd` / `Demo123!`
- Admin: `admin@asenaki.bd` / `Admin123!`

These accounts are intentionally public because this repository is a university/demo project.

## MongoDB configuration

The application reads one runtime environment variable:

```text
MONGODB_URI=mongodb+srv://<user>:<password>@<cluster>/<database>?appName=Ase-Naki-Mongo
```

The credential is not committed to this public repository. Set it in Render and in your local environment.

## Local Docker

```bash
export MONGODB_URI='your MongoDB Atlas URI'
docker compose up --build
```

Open http://localhost:8080.

## Render

Deploy branch `mongodb-crud-docker` as a Docker web service and set `MONGODB_URI` in Render environment variables. The application exposes `/actuator/health` for the Render health check.
