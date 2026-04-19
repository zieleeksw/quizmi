# Task 024 - Course role change test alignment

## Title
align moderator role-change test with owner-only permission rules

## Goal
Keep backend integration coverage consistent with the current authorization contract for course member role changes.

## Why
`CourseFacade.updateCourseMemberRole` now rejects non-owner role changes before the moderator-specific guard logic runs. The integration test still expected a moderator to demote another moderator to `MEMBER`, which no longer matches the actual permission model and fails with `403 AccessDeniedException`.

## Scope
- [x] update the outdated course integration test for moderator role demotion
- [x] assert the current forbidden response contract instead of a successful role update
- [x] document why the test expectation changed

## Steps
- [x] rename the test to describe the forbidden moderator flow
- [x] change the expected HTTP status from `200 OK` to `403 Forbidden`
- [x] verify the response body still returns the standard access-denied payload

## Decisions
- role changes remain owner-only at the API level, even if lower-level helper logic still contains moderator-specific validation branches
- the regression test should protect the externally visible contract, not an outdated earlier expectation

## Date
2026-04-19
