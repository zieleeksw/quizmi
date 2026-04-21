# Task 037 - Quiz session and quiz fetch performance

## Title
reduce unnecessary full question loading in quiz fetch, quiz update, and quiz session flows

## Goal
Make quiz editing, quiz fetching, session creation, and quiz submit flows faster by avoiding full-course question loading when only question ids and category ids are required.

## Why
Controller timing logs showed slow quiz-related endpoints after editing a quiz:

- `PUT /courses/1/quizzes/4` took about `2513 ms`
- `GET /courses/1/quizzes/4` took about `1300 ms`
- `GET /courses/1/questions` took about `998 ms`
- `POST /courses/1/quizzes/4/session` took about `3895 ms`

The main issue was that several flows loaded every full question in the course, including prompts, answers, and categories, even when they only needed lightweight question summaries or only the selected quiz questions.

## Scope
- [x] replace full question loading with `fetchQuestionSummaries` in quiz create/update/fetch/version flows
- [x] keep quiz validation behavior based on available question ids and category ids
- [x] optimize session creation so manual quizzes load only quiz question ids
- [x] optimize random/category session creation by resolving selected ids from summaries first
- [x] load full question data only for the resolved session questions needed to build answer order
- [x] optimize quiz submit when an active session exists by loading only session questions
- [x] add an index for course/user session listing by latest update

## Steps
- [x] update `QuizFacade` to use `QuestionSummary` instead of full `QuestionDto` collections
- [x] remove redundant conversion from full questions to summaries
- [x] update quiz draft validation to use summary ids/category ids
- [x] update `QuizSessionFacade` to resolve session question ids before loading full question details
- [x] add a manual quiz fast path that loads only `quiz.questionIds`
- [x] update `QuizAttemptFacade` to reuse active session question ids when loading submit data
- [x] add Liquibase index `idx_quiz_sessions_course_user_updated_at`

## Decisions
- full question data is still loaded where answer content/order is needed
- quiz create/update/fetch flows should not load answers just to compute resolved counts
- manual quiz sessions should avoid scanning all course questions
- random/category quiz sessions still need course-wide summaries to resolve available questions
- `GET /courses/{courseId}/questions` remains a separate full-list endpoint and may still be costly for large courses

## Verification
- [x] reviewed diffs statically for the touched backend paths
- [ ] run backend compilation and tests when Gradle can access its wrapper distribution

## Date
2026-04-21
