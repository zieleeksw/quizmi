# Task 008 - Course categories

## Title
implement course category management inside the course workspace

## Goal
Allow the owner of a course to create and browse categories inside a selected course so the course structure is ready for future question management.

## Why
Courses already exist as the top-level workspace entity. Categories are the next structural layer and will organize the question bank before question authoring is introduced.

## Scope
- [x] create category backend model
- [x] add category database schema with Liquibase
- [x] relate categories to courses
- [x] expose authenticated category endpoints
- [x] verify owner-only access to category management
- [x] add backend integration tests for category flows
- [x] add frontend category list page inside course workspace
- [x] add frontend category creation page
- [x] connect category management with course details navigation
- [x] add category rename flow
- [x] add category version history persistence
- [x] expose category version history endpoint
- [x] add frontend category edit/history view
- [x] add unsaved changes warning for category edit flow

## Steps
- [x] create category entity and repository
- [x] create category DTO and create request
- [x] add category validation
- [x] add `GET /courses/{courseId}/categories`
- [x] add `POST /courses/{courseId}/categories`
- [x] enforce that only course owner can manage categories
- [x] add backend integration tests for create/list and ownership rules
- [x] add Angular category service
- [x] add `/courses/:courseId/categories` page
- [x] add `/courses/:courseId/categories/create` page
- [x] add `Manage Categories` action on the course details page
- [x] render empty state and basic category grid in Angular
- [x] create `category_versions` backend model and table
- [x] seed version `1` when a category is created
- [x] add `PUT /courses/{courseId}/categories/{categoryId}`
- [x] append a new version entry when the category name changes
- [x] add `GET /courses/{courseId}/categories/{categoryId}/versions`
- [x] add backend integration tests for rename and history
- [x] add `/courses/:courseId/categories/:categoryId/edit` page
- [x] show current category details and version timeline in Angular
- [x] add reusable pending changes confirmation for category edit navigation

## Decisions
- categories belong to exactly one course
- category access follows course ownership
- category rename and history now belong to task 008
- category history should be append-only, not overwritten in place
- category edit should protect users from leaving with unsaved changes
- pending changes confirmation should be reusable across future edit screens
- archive/delete still stay outside this task
- questions will be added only after categories are in place

## Frontend Shape
- keep `/courses/:courseId` as the clean course overview page
- add a visible `Manage Categories` action next to `Edit Details`
- use a dedicated page for categories instead of tabs for now
- show a searchable category list with an empty state
- use a separate create page for adding a new category
- use a separate edit page for renaming a category and showing its version history
- show a custom in-app confirmation when the user leaves category edit with unsaved changes

## Data
2026-04-09
