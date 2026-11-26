# ExSplit

A Spring Boot REST backend for splitting shared expenses: users log in, create groups, record expenses and get the fewest payments needed to settle up.

> Work in progress. Full architecture notes, design decisions and API docs will land here as the project is built.

## Tech stack

Java 21 · Spring Boot · Spring Security (JWT) · Spring Data JPA · PostgreSQL 16 · Flyway · JUnit 5 · Testcontainers · Docker

## Running locally

Requirements: JDK 21+, Docker.

```bash
cp .env.example .env        # then set JWT_SECRET
./mvnw spring-boot:run      # starts Postgres + Mailpit via compose.yaml, then the app
```

- API: http://localhost:8080
- Mailpit inbox: http://localhost:8025
- Postgres is exposed on host port **5433** to avoid clashing with a locally installed Postgres.

## Tests

```bash
./mvnw test                 # needs Docker running (Testcontainers)
```
