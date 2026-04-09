# Task 005 - Login and authentication

## Title
implement login flow and JWT-based authentication

## Goal
Allow registered users to sign in and prepare the application for protected backend endpoints and authenticated frontend flows.

## Why
Registration is already in place, so the next natural step is letting users authenticate and carry an application session across future modules like courses, questions, and quizzes.

## Scope
- [x] add backend login endpoint
- [x] generate JWT access and refresh tokens
- [x] configure Spring Security for stateless API authentication
- [x] expose a protected endpoint for current user session
- [x] add backend integration tests for login and token flow
- [x] add frontend login page
- [x] persist authenticated session in the frontend
- [x] attach bearer token to future protected API calls

## Steps
- [x] create authentication request and response DTOs
- [x] implement user credential authentication
- [x] generate and validate JWT tokens
- [x] configure a JWT authentication filter
- [x] expose `POST /auth/login`
- [x] expose `POST /auth/refresh-token`
- [x] expose `GET /auth/me`
- [x] add Angular auth service and interceptor
- [x] add Angular login route and authenticated session page

## Decisions
- authentication is stateless and uses bearer JWT tokens
- refresh tokens are available from the start for future session renewal
- `/api/health` stays public, while all other non-auth endpoints are prepared to require authentication

## Data
2026-04-09
