# Task 017 - Quiz play check flow

## Title
align quiz solving flow with skip, check-answer, and next states

## Goal
Update the quiz solving experience so unanswered questions can be skipped, selected answers can be checked before moving on, and checked questions immediately show review-style feedback.

## Why
The previous quiz play flow forced the user to answer every question before finishing and did not provide immediate correctness feedback per question. That made the solving experience feel different from the expected learning flow shown in the reference example.

## Scope
- [x] show `Skip` when no answer is selected
- [x] show `Check answer` when at least one answer is selected and the question is not yet checked
- [x] show `Next` after a question has been checked
- [x] lock answer selection after checking a question
- [x] reuse review-style visual states for correct, wrong, and missed answers during quiz play
- [x] allow finishing an attempt with skipped questions
- [x] include skipped questions in the saved attempt review snapshot

## Steps
- [x] extend `quiz-play` state with per-question checked tracking
- [x] update the primary quiz action to switch between skip, check, next, and finish
- [x] redesign the quiz play layout into a single-question flow with progress feedback
- [x] mirror review-page answer states inside the play page after checking
- [x] relax backend attempt validation so skipped questions are stored as unanswered instead of rejected
- [x] add an integration test that verifies finishing a quiz with a skipped question

## Decisions
- checked state is handled on the frontend for the active play session
- skipped questions are saved as questions with empty selected answers in the final review snapshot
- finishing the last unanswered question goes straight to attempt completion instead of forcing a final selection

## Date
2026-04-14
