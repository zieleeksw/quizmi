# Task 029 - Quiz session update performance

## Title
reduce backend work performed on every quiz session save

## Goal
Make `PUT /courses/{courseId}/quizzes/{quizId}/session` lighter so saving answers during quiz play no longer reloads far more course data than is needed.

## Why
Session saves were noticeably slower than simpler endpoints such as categories because each update did more than persist `answersJson`. The backend revalidated quiz visibility through a heavy quiz DTO path and reloaded the full course question set before synchronizing the current session answers.

## Scope
- [x] remove full quiz DTO loading from session update access validation
- [x] keep active quiz visibility checks for session saves
- [x] load only session question ids when synchronizing saved answers
- [x] avoid fetching the entire course question bank during every session update

## Steps
- [x] add a lightweight `QuizFacade.assertActiveQuizVisible` path for session saves
- [x] add `QuestionFacade.fetchQuestionsByIds` for scoped question loading
- [x] add repository support for `findAllByCourseIdAndIdIn`
- [x] update `QuizSessionFacade.updateSession` to use scoped question loading
- [x] update session synchronization to reuse the same scoped loading strategy

## Decisions
- session save requests should validate access and quiz activity, but they do not need the full `QuizDto` with resolved question counts
- answer synchronization should operate only on the question ids already stored in the session
- this change reduces backend work without changing the user-facing session contract
- local automated verification was limited here by sandbox constraints, so the improvement was checked statically in code

## Date
2026-04-19
