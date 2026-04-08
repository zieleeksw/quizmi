# Task 003 - Testcontainers

## Title
configure testcontainers for postgres integration tests

## Goal
Prepare backend integration tests that start a real PostgreSQL container automatically.

## Why
This gives the project a reliable testing foundation before implementing user registration and other database-based features.

## Scope
- [x] add Testcontainers dependencies
- [x] configure PostgreSQL container for tests
- [x] connect Spring test configuration to the container
- [x] create a base integration test setup
- [x] verify that the application context starts with the test container

## Steps
- [x] add Testcontainers libraries to Gradle
- [x] create a shared integration test base
- [x] expose datasource properties from the container
- [x] start PostgreSQL automatically in tests
- [x] run at least one integration test against the container

## Decisions
- integration tests should use a real PostgreSQL instance
- Testcontainers will be prepared before user registration
- the setup should be reusable for future auth and domain tests

## Data
2026-04-08
