# Task 013 - Question validation error contract cleanup

## Title
align question duplicate-answer test with the validation error response contract

## Goal
Keep question integration tests aligned with the actual validation error payload returned by the backend.

## Why
One question test was still asserting the old `fieldErrors` JSON path even though the global validation handler now returns errors under `errors`.

## Scope
- [x] inspect the current validation error payload shape
- [x] update the duplicate-answer integration test to use the live response contract
- [x] assert both the field name and the validation message for better coverage

## Steps
- [x] confirm the global exception handler returns `errors`
- [x] replace the stale JSON path in `QuestionIntegrationTest`
- [x] keep the domain validation message unchanged

## Decisions
- the backend response contract stays unchanged
- this task fixes the test expectation instead of changing runtime error serialization

## Data
2026-04-12