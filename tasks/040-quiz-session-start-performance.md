# Task 040 - Quiz session start performance

## Title
make quiz session start and resume use lightweight runtime data

## Goal
Reduce latency for `POST /courses/{courseId}/quizzes/{quizId}/session` by avoiding full quiz DTO and full question DTO loading during session creation/resume.

## Why
Production timing logs showed:

- `POST /courses/1/quizzes/5/session` handled by `QuizAttemptController.createOrResumeSession` in about `4048 ms`

The session flow only needs quiz runtime settings, selected question/category ids, and current answer ids. It was still loading a full `QuizDto`, which required course-wide question summaries for `resolvedQuestionCount`, and then loading full `QuestionDto` objects with prompts, explanations, categories, and answer content even though only answer ids were needed.

## Scope
- [x] add a lightweight quiz session spec for session runtime data
- [x] stop using full `QuizDto` in `QuizSessionFacade.createOrResumeSession`
- [x] load current answer ids by question id instead of full question DTOs for session synchronization
- [x] keep stale saved answer cleanup behavior
- [x] keep randomized answer order persistence behavior
- [x] add an index for active session lookup by course, quiz, and user

## Steps
- [x] add `QuizSessionSpec`
- [x] add `QuizFacade.fetchQuizSessionSpec`
- [x] add `QuestionFacade.fetchCurrentAnswerIdsByQuestionIds`
- [x] update `QuizSessionFacade` to synchronize answers and answer order from answer-id maps
- [x] add Liquibase index `idx_quiz_sessions_course_quiz_user`

## Decisions
- full quiz DTOs remain used by quiz-facing read endpoints
- session start/resume should not compute `resolvedQuestionCount`
- session start/resume should not load question prompt, explanation, category names, or answer content
- random/category quizzes still need lightweight question summaries to resolve eligible question ids

## Verification
- [x] reviewed the touched backend diff statically
- [ ] run backend compilation when Gradle wrapper download/execution is allowed
- [ ] compare production timing logs after deploy

## Date
2026-04-22
