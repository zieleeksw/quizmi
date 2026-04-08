# Task 002 - Backend dev config

## Title
configure backend development profile for local postgres

## Goal
Connect the backend to the local PostgreSQL database prepared for development.

## Why
After preparing the database container, the next step is making the backend use it in a clean local development profile.

## Scope
- [x] add backend dependencies required for API and database connection
- [x] prepare `application-dev.yaml`
- [x] configure datasource url, username, and password
- [x] set up a local development profile
- [x] add a simple test endpoint

## Steps
- [x] add web dependency
- [x] add database dependency
- [x] add local datasource config
- [x] make the backend start with the dev profile
- [x] expose `GET /api/health`

## Decisions
- local database config will live in the dev profile
- backend should be ready for future auth and domain modules
- the first backend endpoint should only confirm that the app starts correctly

## Data
2026-04-08
