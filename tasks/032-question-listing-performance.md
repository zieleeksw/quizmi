# Task 032 - Question listing performance

## Title
reduce backend work for `GET /courses/{id}/questions`

## Goal
Make question listing faster by removing per-question query fan-out while keeping the existing `QuestionDto` response contract unchanged.

## Why
Controller timing logs showed `GET /courses/{id}/questions` taking multiple seconds and sometimes over `13 s` in a small test environment. That points to backend data loading inefficiency rather than legitimate payload size.

## Scope
- [x] analyze the `GET /courses/{id}/questions` flow
- [x] batch-load current question versions for the listed questions
- [x] batch-load question categories and answers for current versions
- [x] stop building hidden preview questions before applying the preview limit
- [x] add regression coverage for listing with multiple questions

## Steps
- [x] inspect `QuestionController`, `QuestionFacade`, and related repositories
- [x] add repository methods for batched answer and current-version loading
- [x] refactor question listing assembly to use grouped data instead of N+1 reads
- [x] reuse the same grouped loading for `fetchQuestionsByIds` and single-question DTO mapping helpers
- [x] add integration coverage for listing multiple questions with categories and answers

## Decisions
- the API contract for `QuestionDto` stays unchanged, including embedded `categories` and `answers`
- optimization focuses on batching current question data instead of introducing a separate lightweight DTO for now
- the locked-course preview limit should be applied before building DTOs for hidden questions
- automated Gradle verification may still be blocked here by sandbox network limits around downloading Gradle

## Date
2026-04-20
