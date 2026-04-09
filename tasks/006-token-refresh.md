# Task 006 - Token refresh

## Title
implement automatic access token refresh in authenticated frontend flow

## Goal
Keep the user signed in when the access token expires by refreshing it with the stored refresh token and retrying the original request.

## Why
Task 005 introduced login, JWT access tokens, refresh tokens, and the backend refresh endpoint. The missing piece is frontend session continuity, so users are not forced to log in again after every access token expiration.

## Scope
- [x] use backend `POST /auth/refresh-token` from the frontend
- [x] detect expired access token responses
- [x] refresh tokens automatically before logging the user out
- [x] retry the original protected request after successful refresh
- [x] update stored session tokens in the frontend
- [x] prevent multiple refresh requests from running in parallel
- [x] log the user out when refresh token is invalid or expired
- [x] add frontend tests for refresh flow

## Steps
- [x] extend `AuthService` with refresh-session method
- [x] add refresh token request and response handling
- [x] update interceptor to handle `401 Unauthorized`
- [x] queue or share refresh work while one refresh is already in progress
- [x] repeat the failed request with the new access token
- [x] clear session only when refresh fails definitively
- [x] verify UX on login/session pages after refresh

## Decisions
- access token refresh should happen transparently for the user
- refresh logic belongs in the frontend auth layer, not in feature services
- only one refresh request should be active at a time
- logout should happen only after refresh failure, not on the first expired access token

## Data
2026-04-09
