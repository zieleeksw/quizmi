# Task 002 - Backend dev config

## Title
configure backend development profile for local postgres

## Goal
Connect the backend to the local PostgreSQL database prepared for development.

## Why
After preparing the database container, the next step is making the backend use it in a clean local development profile.

## Scope
- [ ] add backend dependencies required for API and database connection
- [ ] prepare `application-dev.yaml`
- [ ] configure datasource url, username, and password
- [ ] set up a local development profile
- [ ] add a simple test endpoint

## Steps
- [ ] add web dependency
- [ ] add database dependency
- [ ] add local datasource config
- [ ] make the backend start with the dev profile
- [ ] expose `GET /api/health`

## Decisions
- local database config will live in the dev profile
- backend should be ready for future auth and domain modules
- the first backend endpoint should only confirm that the app starts correctly

## Data
2026-04-08
