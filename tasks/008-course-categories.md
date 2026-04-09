# Task 008 - Course categories

## Title
implement course category management inside the course workspace

## Goal
Allow the owner of a course to create and browse categories inside a selected course so the course structure is ready for future question management.

## Why
Courses already exist as the top-level workspace entity. Categories are the next structural layer and will organize the question bank before question authoring is introduced.

## Scope
- [ ] create category backend model
- [ ] add category database schema with Liquibase
- [ ] relate categories to courses
- [ ] expose authenticated category endpoints
- [ ] verify owner-only access to category management
- [ ] add backend integration tests for category flows
- [ ] add frontend category list page inside course workspace
- [ ] add frontend category creation page
- [ ] connect category management with course details navigation

## Steps
- [ ] create category entity and repository
- [ ] create category DTO and create request
- [ ] add category validation
- [ ] add `GET /courses/{courseId}/categories`
- [ ] add `POST /courses/{courseId}/categories`
- [ ] enforce that only course owner can manage categories
- [ ] add backend integration tests for create/list and ownership rules
- [ ] add Angular category service
- [ ] add `/courses/:courseId/categories` page
- [ ] add `/courses/:courseId/categories/create` page
- [ ] add `Manage Categories` action on the course details page
- [ ] render empty state and basic category grid in Angular

## Decisions
- categories belong to exactly one course
- category access follows course ownership
- task 008 covers create and list only
- category rename, delete, and archive stay outside this task
- questions will be added only after categories are in place

## Frontend Shape
- keep `/courses/:courseId` as the clean course overview page
- add a visible `Manage Categories` action next to `Edit Details`
- use a dedicated page for categories instead of tabs for now
- show a searchable category list with an empty state
- use a separate create page for adding a new category

## Data
2026-04-09
