# Task 010 - Course quizzes

## Title
implement quiz composition, play sessions, and attempt reviews inside the course workspace

## Goal
Allow the owner of a course to create reusable quizzes from the question bank, play them inside the app, and review exact-match results after each attempt.

## Why
Questions already exist as reusable building blocks. Quizzes are the first learner-facing flow that turns the course structure into an actual playable experience.

## Scope
- [x] create quiz backend model
- [x] add quiz database schema with Liquibase
- [x] version quiz title, mode, ordering, and source selection
- [x] expose authenticated quiz endpoints
- [x] add quiz sessions and quiz attempts on the backend
- [x] enforce exact-match scoring for questions with multiple correct answers
- [x] add backend integration tests for quiz creation and multi-answer attempt scoring
- [x] add frontend quiz library page inside course workspace
- [x] add frontend quiz create and edit flow
- [x] add frontend quiz play flow
- [x] add frontend attempt review page
- [x] connect quiz management with course details navigation

## Steps
- [x] create quiz entities, repositories, DTOs, and request models
- [x] add `GET /courses/{courseId}/quizzes`
- [x] add `GET /courses/{courseId}/quizzes/{quizId}`
- [x] add `GET /courses/{courseId}/quizzes/{quizId}/versions`
- [x] add `POST /courses/{courseId}/quizzes`
- [x] add `PUT /courses/{courseId}/quizzes/{quizId}`
- [x] add `DELETE /courses/{courseId}/quizzes/{quizId}`
- [x] support `manual`, `random`, and `category` quiz modes
- [x] add `GET /courses/{courseId}/sessions`
- [x] add `POST /courses/{courseId}/quizzes/{quizId}/session`
- [x] add `PUT /courses/{courseId}/quizzes/{quizId}/session`
- [x] add `GET /courses/{courseId}/attempts`
- [x] add `GET /courses/{courseId}/attempts/reviews`
- [x] add `GET /courses/{courseId}/attempts/{attemptId}`
- [x] add `POST /courses/{courseId}/quizzes/{quizId}/attempts`
- [x] store quiz session answers as selected answer id sets per question
- [x] score a question only when the selected answer set exactly matches the full correct set
- [x] add Angular quiz service and attempt service
- [x] add `/courses/:courseId/quizzes` page
- [x] add `/courses/:courseId/quizzes/new` page
- [x] add `/courses/:courseId/quizzes/:quizId/edit` page
- [x] add `/courses/:courseId/quizzes/:quizId/play` page
- [x] add `/courses/:courseId/attempts/:attemptId` page
- [x] add `Manage Quizzes` action on the course details page

## Decisions
- quiz access follows course ownership
- quiz updates append a new version instead of mutating history in place
- quiz editor supports the same three composition modes as the existing product flow: `manual`, `random`, and `category`
- quiz sessions store selected answers per question as a list of answer ids, not a single answer id
- questions with multiple correct answers use exact-match scoring: every correct answer must be selected, and no wrong answer may be selected
- quiz play and quiz review stay as dedicated full-screen flows
- quiz creation should remain blocked when the course has no questions
- insights and analytics stay outside this task

## Frontend Shape
- keep `/courses/:courseId` as the clean course overview page and add a third `Manage Quizzes` card
- use a dedicated quiz library page instead of mixing quiz browsing into course details
- keep create and edit in one focused quiz editor with a history column on edit
- let quiz cards open the edit flow, while play and resume stay available as direct actions
- keep quiz play inside the app with multi-select answer controls
- show a dedicated attempt review page after finishing a quiz

## Data
2026-04-10
