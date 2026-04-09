# Task 007 - Courses foundation

## Title
implement authenticated course creation and personal course list

## Goal
Allow a logged-in user to create courses and view their own course workspace in `quizmi`.

## Why
After registration, login, and token refresh, the next step is using auth in a real product feature. Courses are the natural root entity for later categories, questions, and quizzes.

## Scope
- [x] create course backend model
- [x] add course database schema with Liquibase
- [x] add course validation
- [x] expose authenticated `GET /courses`
- [x] expose authenticated `POST /courses`
- [x] assign course ownership to current user
- [x] add backend integration tests with JWT auth
- [x] add frontend course creation page after login
- [x] add frontend course list for current user
- [x] redirect authenticated user to courses workspace

## Steps
- [x] create course entity and repository
- [x] create course DTO and create request
- [x] add name and description validators
- [x] implement course facade for create/list
- [x] add course controller secured by JWT
- [x] add Liquibase migration for `courses`
- [x] add integration tests for authenticated create/list
- [x] add Angular course service
- [x] add Angular `/courses` page with create form and list
- [x] update login and guest redirect flow

## Decisions
- each course belongs to exactly one owner user
- the first version lists only the current user's courses
- course editing, categories, and questions stay outside this task

## Data
2026-04-09
