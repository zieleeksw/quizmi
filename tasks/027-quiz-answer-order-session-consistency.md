# Task 027 - Quiz answer order session consistency

## Title
persist randomized quiz answer order in the session and keep play/review aligned

## Goal
Make `answerOrder=random` behave like a real per-session answer shuffle instead of a frontend-only display trick, and keep the answer order consistent between quiz play and attempt review.

## Why
The original implementation did not persist randomized answer order anywhere in backend session state. The play screen derived a deterministic order locally from a hash in the frontend, while the review snapshot still rendered answers in the base `displayOrder`. That created two problems:
- `answerOrder=random` was not truly session-bound and could look "not random enough"
- the learner could see one answer order while solving the quiz and a different one in the saved review

During regression work an additional test issue showed up:
- one backend test tried to create a question with `8` answers even though validation allows at most `6`, so the test failed before reaching the answer-order logic
- another test compared two separate randomizations and assumed they must differ, which made the assertion flaky

## Scope
- [x] persist answer order per question inside `quiz_sessions`
- [x] expose stored `answerOrderByQuestion` in `QuizSessionDto`
- [x] render quiz play answers from the stored session order instead of frontend hash sorting
- [x] build attempt review snapshots using the same stored session order
- [x] recover old or invalid persisted answer orders when resuming a session
- [x] replace flaky answer-order assertions with stable session-based regression coverage
- [x] fix invalid test setup that exceeded the maximum number of allowed answers

## Steps
- [x] add `answer_order_json` to `quiz_sessions` with Liquibase
- [x] extend `QuizSessionEntity` and `QuizSessionDto` with persisted answer order data
- [x] generate answer order in `QuizSessionFacade.createSession`
- [x] synchronize and repair stored answer order in `QuizSessionFacade.createOrResumeSession`
- [x] remove the frontend `hashSeed` ordering logic from `quiz-play-page.component.ts`
- [x] reorder review answers in `QuizAttemptFacade` from session data
- [x] add backend integration coverage for session resume and review consistency
- [x] add frontend component coverage for using session-provided answer order
- [x] reduce regression test fixtures to `6` answers so they match backend validation

## Decisions
- randomized answer order is now a backend-owned session concern, not a frontend rendering concern
- the same persisted order must be used in both the active play flow and the saved attempt review
- if an existing session contains a base-order answer layout for a random quiz, session resume should repair it instead of preserving the broken state
- tests should verify stable contract behavior such as "different from base order" and "stable across resume", not compare two unrelated random shuffles
- local TypeScript compilation was verified, but full automated runtime verification remained partially blocked in this environment by sandbox restrictions around Gradle download and headless browser startup

## Date
2026-04-19
