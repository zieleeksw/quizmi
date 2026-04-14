# Task 019 - Frontend API base URL env

## Title
make frontend API base URL configurable for static deploys

## Goal
Allow the Angular frontend to use a deploy-specific backend URL instead of assuming the API is always available on the same host at port `8080`.

## Why
The frontend is being deployed separately from the backend, so hardcoded API host assumptions break authentication and all data requests in production.

## Scope
- [x] add a frontend runtime/build config source for API base URL
- [x] support a deploy environment variable named `QUIZMI_API_BASE_URL`
- [x] keep local development fallback to `http://localhost:8080`
- [x] route all frontend services through one shared API base URL token

## Steps
- [x] add a generated frontend config module
- [x] add a script that writes config from environment variables before build
- [x] inject one shared API base URL into frontend services
- [x] preserve current localhost behavior when no env is provided

## Decisions
- the frontend env name is `QUIZMI_API_BASE_URL`
- config is baked into the static build at build time
- trailing slashes are removed from the configured API base URL

## Date
2026-04-14
