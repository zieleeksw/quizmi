# Task 041 - Quiz play locked previous answers

## Title
persist and enforce locked previous quiz answers

## Goal
Prevent learners from editing an already answered previous quiz question after moving forward, including after leaving and resuming the quiz.

## Why
During one live quiz session, the frontend blocked editing a checked previous question through in-memory `checkedQuestionIds`. After leaving the quiz and resuming it, that in-memory state was lost. A learner could then go back and edit a previously answered question.

This must be enforced by the backend, not only by the browser UI.

## Scope
- [x] persist the furthest reached question index in quiz sessions
- [x] persist checked question ids in quiz sessions
- [x] expose `furthestIndex` in `QuizSessionDto`
- [x] expose `checkedQuestionIds` in `QuizSessionDto`
- [x] reject backend session updates that change saved answers for answered questions before `furthestIndex`
- [x] reject backend session updates that change answers for previously checked questions
- [x] keep navigation back to previous questions allowed when answers are unchanged
- [x] lock previous answered questions in the quiz play UI after resume
- [x] add backend and frontend tests for the locked-answer behavior

## Steps
- [x] add `furthest_index` to `quiz_sessions`
- [x] add `checked_question_ids_json` to `quiz_sessions`
- [x] update `QuizSessionEntity` and `QuizSessionDto`
- [x] update `QuizSessionFacade.updateSession` to validate locked previous answers
- [x] update `QuizSessionFacade.updateSession` so checked question ids can only be preserved or added
- [x] update `QuizPlayPageComponent` to lock answered questions before `furthestIndex`
- [x] update `QuizPlayPageComponent` to persist checked question ids when `Check answer` is clicked
- [x] update `QuizSessionDto` frontend model
- [x] add an integration test that rejects editing a locked previous answer
- [x] add an integration test that rejects editing a checked single-question session after resume
- [x] add a frontend spec that verifies resumed locked answers cannot be toggled
- [x] add a frontend spec that verifies a resumed checked single-question session stays locked

## Decisions
- `currentIndex` remains the currently viewed question
- `furthestIndex` records the highest reached question index
- `checkedQuestionIds` records questions where feedback has already been revealed
- unanswered skipped previous questions remain editable
- answered previous questions must keep the same answer id set
- checked questions must keep the same answer id set even when they are the current or only question
- frontend locking mirrors backend behavior, but backend remains authoritative

## Verification
- [x] reviewed the diff statically
- [x] ran `git diff --check`
- [ ] run frontend quiz-play spec when ChromeHeadless can spawn outside the sandbox
- [ ] run backend compilation/tests when Gradle wrapper download/execution is allowed

## Date
2026-04-22
