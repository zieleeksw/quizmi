# Task 007 - Courses foundation

## Title
implement authenticated course workspace with creation, details, and editing

## Goal
Allow a logged-in user to create courses, browse their own course workspace, open a course, and update its basic details in `quizmi`.

## Why
After registration, login, and token refresh, the next step is using auth in a real product feature. Courses are the natural root entity for later categories, questions, and quizzes.

## Scope
- [x] create course backend model
- [x] add course database schema with Liquibase
- [x] add course validation
- [x] expose authenticated `GET /courses`
- [x] expose authenticated `POST /courses`
- [x] expose authenticated `GET /courses/{courseId}`
- [x] expose authenticated `PUT /courses/{courseId}`
- [x] assign course ownership to current user
- [x] restrict course details and editing to the owner
- [x] add backend integration tests with JWT auth
- [x] add frontend course creation page after login
- [x] add frontend course list for current user
- [x] add frontend course details page
- [x] add frontend course edit page
- [x] redirect authenticated user to courses workspace

## Steps
- [x] create course entity and repository
- [x] create course DTO plus create/update requests
- [x] add name and description validators
- [x] implement course facade for create/list/details/update
- [x] add course controller secured by JWT
- [x] add Liquibase migration for `courses`
- [x] add integration tests for authenticated create/list/details/update
- [x] add Angular course service
- [x] add Angular `/courses` page with personal course list
- [x] add Angular `/courses/create` page with create form
- [x] add Angular `/courses/:courseId` page for course details
- [x] add Angular `/courses/:courseId/edit` page for course editing
- [x] make course tiles navigate directly to course details
- [x] update login and guest redirect flow

## Decisions
- each course belongs to exactly one owner user
- the first version lists only the current user's courses
- course details and basic editing belong to this task
- categories and questions stay outside this task

## Data
2026-04-09
