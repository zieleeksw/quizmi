# Task 018 - Backend Docker deploy

## Title
containerize backend for Java 24 deployment

## Goal
Prepare a production-ready backend Docker image so the Spring Boot API can be deployed consistently in container environments.

## Why
The project already targets Java 24 in Gradle, but it did not include a backend Dockerfile. Without a dedicated image definition, deployment is slower, more manual, and easier to misconfigure.

## Scope
- [x] add a dedicated `backend/Dockerfile`
- [x] align the container image with Java 24
- [x] support building the app directly from the backend module
- [x] expose the backend on port `8080`
- [x] keep runtime configuration externalized via environment variables

## Steps
- [x] inspect Gradle backend build configuration
- [x] prepare a multi-stage Docker build
- [x] use a Java 24 builder image for the Gradle build
- [x] use a lightweight Java 24 runtime image for the final container
- [x] document that the frontend still expects the backend on the same host at port `8080`

## Decisions
- the Dockerfile lives in `backend/Dockerfile`
- the container build context should target the `backend` directory
- `SPRING_PROFILES_ACTIVE` defaults to `dev` inside the container because datasource settings currently live in `application-dev.yaml`
- deploy-specific secrets such as DB credentials and JWT secret stay outside the image

## Date
2026-04-14
