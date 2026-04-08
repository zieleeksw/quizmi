# Task 001 - Dev database

## Title
setup docker compose for local development database

## Goal
Prepare a local database for `quizmi` development so the next modules can be built on a stable development environment.

## Why
The backend will need a persistent database for auth, courses, questions, and quizzes. It is better to prepare it now before entities and migrations appear.

## Scope
- [ ] update `tools/docker-compose.yaml`
- [ ] configure a PostgreSQL container
- [ ] set database name, user, and password
- [ ] add a data volume
- [ ] expose a port for local development
- [ ] add a short run instruction

## Steps
- [ ] choose a PostgreSQL version
- [ ] prepare `tools/docker-compose.yaml`
- [ ] define variable names and access data
- [ ] make sure the config matches future `application-dev.yaml`
- [ ] add a note on how to start and stop the database

## Decisions
- local development will use Docker
- the development database will stay separate from application logic
- the setup should stay simple and readable, without extra services at the start

## Data
2026-04-08
