# Task 014 - Shared course visibility and learning flow

## Title
allow every authenticated user to browse courses and use them in read-only learning mode

## Goal
Allow every logged-in user to browse the course catalog, open course content, play quizzes, and review only their own learning results while keeping editing and management actions restricted to the course owner.

## Why
The original implementation exposed course previews and deeper course content only to the owner, which blocked both workspace discovery and the learning flow for other authenticated users. The product needs shared visibility and read-only usage, not shared course management.

## Scope
- [x] expose the course preview list to every authenticated user
- [x] expose course detail read access to every authenticated user
- [x] expose category, question, and quiz read flows to every authenticated user
- [x] allow non-owners to create sessions, play quizzes, and review only their own attempts and statistics
- [x] keep create and update ownership rules unchanged
- [x] include the real owner email in course DTOs for preview rendering
- [x] update frontend course preview, detail, and learning pages for shared visibility
- [x] cover the backend changes with integration tests

## Steps
- [x] update the backend course list and detail fetch flows to stop filtering by actor ownership
- [x] split backend read access from owner-only management checks in categories, questions, quizzes, attempts, and sessions
- [x] enrich `CourseDto` with owner email
- [x] adjust course integration tests for shared visibility and owner-only updates
- [x] adjust category, question, and quiz integration tests for viewer read access and owner-only authoring
- [x] update the courses page search and card footer to use the real owner email
- [x] hide edit and management actions for non-owners in the frontend
- [x] open browse and play flows for viewers across course details, categories, questions, quizzes, and own statistics
- [x] show clear read-only messaging when a non-owner enters direct authoring routes by URL

## Decisions
- course preview is shared across authenticated users only
- course creation and updates remain owner-only
- non-owners can open course details, categories, questions, and quizzes in read-only mode
- non-owners can create quiz sessions and attempts, but only for themselves
- non-owners can see only their own quiz sessions, attempts, reviews, and derived statistics
- owner identity is shown via email on preview cards and details
- owner-specific create and edit actions are hidden in the UI but remain enforced in the backend

## Data
2026-04-12
