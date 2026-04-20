# Task 031 - Quiz listing performance

## Title
reduce backend work for `GET /courses/{id}/quizzes`

## Goal
Make quiz listing noticeably faster in small test environments by removing avoidable query fan-out and heavy DTO assembly from `GET /courses/{id}/quizzes`.

## Why
Controller timing logs showed `GET /courses/{id}/quizzes` taking multiple seconds and sometimes over `15 s`, even though the dataset is small. That strongly suggests application-side inefficiency rather than legitimate payload size.

## Scope
- [x] analyze the end-to-end flow for `GET /courses/{id}/quizzes`
- [x] reduce N+1 style repository access while building quiz list DTOs
- [x] stop loading more quiz rows than preview users are allowed to see
- [x] replace full question DTO loading with lightweight current-question summaries for listing
- [x] add a small regression test for mixed quiz listing data
- [x] add supporting DB indexes for course-based list reads

## Steps
- [x] inspect `QuizController`, `QuizFacade`, question flow, and related repositories
- [x] bulk-load current quiz versions for the listed quizzes
- [x] bulk-load quiz question ids and category ids for current versions
- [x] bulk-load lightweight current question category summaries for resolved counts
- [x] limit locked-course preview quiz rows before DTO mapping
- [x] add indexes for `quizzes(course_id, active, created_at)` and `questions(course_id, created_at)`

## Decisions
- the response contract for `QuizDto` stays unchanged
- the optimization targets the list endpoint first, because logs showed it as the slowest consistent read in the quiz area
- question prompts, answers, and other heavy data are not needed for quiz listing and should not be loaded just to compute `resolvedQuestionCount`
- the endpoint should still respect preview limits, but it should not build DTOs for hidden quizzes first
- automated Gradle verification may still be blocked here by sandbox network limits around downloading Gradle

## Date
2026-04-20
