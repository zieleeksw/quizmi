# Task 012 - Bootstrap admin account

## Title
add configurable bootstrap admin creation with integration tests

## Goal
Allow `quizmi` to create and keep a base admin account in sync from configuration so a fresh environment always has an initial administrator.

## Why
`quiz-me` already has a base admin flow and `quizmi` needs the same operational capability. Without it, a new environment has no guaranteed admin entry point.

## Scope
- [x] add an `ADMIN` user role to the backend user model
- [x] add configuration for bootstrap admin email and password
- [x] create the bootstrap admin during application startup when enabled
- [x] keep the bootstrap admin idempotent across repeated startup runs
- [x] allow bootstrap admin authentication through the existing login flow
- [x] cover the feature with backend integration tests

## Steps
- [x] extend the user role enum with admin support
- [x] add a configuration properties class for bootstrap admin settings
- [x] implement a startup initializer that ensures the configured admin exists
- [x] reuse existing validators and password hashing for admin creation
- [x] add integration tests for startup creation, login, and idempotency

## Decisions
- bootstrap admin creation is opt-in and disabled by default
- bootstrap admin credentials come from application configuration and environment variables
- rerunning the initializer updates the configured account to `ADMIN` and keeps the password aligned
- user self-registration still creates `USER` accounts only

## Data
2026-04-12
