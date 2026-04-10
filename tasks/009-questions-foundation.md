# Task 009 - Course questions

## Title
implement versioned question management inside the course workspace

## Goal
Allow the owner of a course to build and maintain a reusable question bank with category tagging, answer management, preview browsing, and version history.

## Why
Categories are already in place, so the next real product layer is the question bank. Questions become the reusable building blocks for future quiz composition and course practice flows.

## Scope
- [x] create question backend model
- [x] add question database schema with Liquibase
- [x] version question prompt, answers, and category assignments
- [x] expose authenticated question endpoints
- [x] verify owner-only access to question management
- [x] add backend integration tests for question flows
- [x] add frontend question bank page inside course workspace
- [x] add frontend question create flow
- [x] add frontend question edit flow with version history
- [x] add unsaved changes warning for question editor flow
- [x] connect question management with course details navigation
- [x] disable question creation in UI when the course has no categories
- [x] reject duplicate answers within a single question
- [x] show a soft warning when the same prompt already exists in the course

## Steps
- [x] create question entity and repositories
- [x] create question DTOs, preview DTO, and request models
- [x] add prompt, answers, and category validation
- [x] add `GET /courses/{courseId}/questions`
- [x] add `GET /courses/{courseId}/questions/preview`
- [x] add `GET /courses/{courseId}/questions/{questionId}`
- [x] add `POST /courses/{courseId}/questions`
- [x] add `PUT /courses/{courseId}/questions/{questionId}`
- [x] add `GET /courses/{courseId}/questions/{questionId}/versions`
- [x] enforce that only course owner can manage questions
- [x] add backend integration tests for create, preview, update, and history
- [x] add Angular question service
- [x] add `/courses/:courseId/questions` page
- [x] add `/courses/:courseId/questions/new` page
- [x] add `/courses/:courseId/questions/:questionId/edit` page
- [x] add `Manage Questions` action on the course details page
- [x] render searchable question preview with category filter and pagination
- [x] show question version timeline in Angular
- [x] add reusable pending changes confirmation for question editor navigation
- [x] disable `Add Question` when no categories exist and show an explanatory hover hint
- [x] block duplicate answers in both frontend and backend validation
- [x] show a non-blocking prompt duplication hint in the question editor

## Decisions
- questions belong to exactly one course
- question access follows course ownership
- answers and category assignments belong to the question version, not the root question row
- updating a question appends a new version instead of mutating history in place
- question create and question edit share one focused editor flow
- question editor should protect users from leaving with unsaved changes
- question creation should stay blocked until the course has at least one category
- answers inside one question should be unique after trim and case normalization
- identical prompts may exist across different questions, but the editor should warn softly within the course scope
- delete/archive question stays outside this task

## Frontend Shape
- keep `/courses/:courseId` as the clean course overview page
- use a dedicated question bank page instead of mixing question browsing into course details
- keep create and edit in a focused full-screen editor flow
- show question preview cards with category pills and answer overview
- keep the version timeline visible beside the editor when editing
- show a custom in-app confirmation when the user leaves question editing with unsaved changes
- keep `Add Question` visibly disabled until categories exist and explain why on hover
- prevent duplicate answers before submit and enforce the same rule on the backend
- warn when the same normalized prompt already exists in the course, without blocking save

## Data
2026-04-10
