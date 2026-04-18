# Task 023 - Quiz session stale answer recovery

## Title
recover active quiz sessions after question answers change

## Goal
Allow learners to resume and finish an active quiz even if one of its questions was edited after the session started and the previously saved answer ids no longer match the current question version.

## Why
Question updates create a new question version and new answer rows, but active quiz sessions still store the old selected `answerId` values. During session save and final attempt creation, backend validation compared those stale ids against the latest question version and rejected the quiz with errors about answers outside the selected quiz or answers not belonging to the selected question.

## Scope
- [x] detect stale saved answer ids when resuming an existing quiz session
- [x] remove invalid answer ids from session state instead of failing the request
- [x] apply the same stale-answer cleanup when persisting quiz progress
- [x] allow finishing a quiz attempt even if submitted answers still contain stale ids from an older question version
- [x] keep question-level validation for answers that truly belong to a different question
- [x] add backend regression coverage for session resume and finish after question update

## Steps
- [x] update `QuizSessionFacade.createOrResumeSession` to synchronize stored session answers with current question answers
- [x] update `QuizSessionFacade.updateSession` to filter out stale answer ids before saving
- [x] add shared answer synchronization logic based on current `QuestionDto` data
- [x] update `QuizAttemptFacade.createAttempt` to sanitize submitted answer ids before building the review snapshot
- [x] preserve the existing guard that submitted `questionId` values must still belong to the selected quiz
- [x] add an integration test for the flow: start session, save answer, edit question, resume session, finish quiz

## Decisions
- stale answer ids are treated as outdated session data, not as a fatal validation error
- if all previously selected answers become stale after a question update, that question becomes unanswered in the active session
- question membership in the quiz remains strict; only answer ids are auto-cleaned
- final attempt completion prefers graceful recovery over blocking the learner with an unrecoverable session state
- automated verification is prepared with an integration test, but local execution was blocked here because the Gradle wrapper could not be downloaded in the sandboxed environment

## Date
2026-04-18
