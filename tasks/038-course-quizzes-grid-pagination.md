# Task 038 - Course quizzes grid pagination

## Title
show course quizzes as a 3 by 3 grid

## Goal
Make the course `quizzes` tab show up to nine quiz cards per page in a stable three-column desktop grid.

## Why
The quizzes tab only showed three quiz cards per page and the grid used automatic column filling. On wider screens this made the section behave like a single-row listing instead of the intended 3 by 3 library layout.

## Scope
- [x] show up to 9 quizzes per page
- [x] use a fixed 3-column desktop grid for quiz cards
- [x] keep the existing 2-column tablet layout
- [x] keep the existing 1-column mobile layout
- [x] avoid changing quiz search, navigation, or access behavior

## Steps
- [x] update `CourseQuizzesPageComponent.pageSize` from `3` to `9`
- [x] change `.course-quizzes-list` from `auto-fill` columns to `repeat(3, minmax(0, 1fr))`
- [x] leave existing media queries for `900px` and `640px` breakpoints intact

## Decisions
- the quizzes tab should match the category tab's desktop grid pattern
- pagination should treat one page as a full 3 by 3 grid
- responsive breakpoints should remain unchanged so smaller screens keep readable cards

## Verification
- [x] reviewed the changed frontend lines statically
- [ ] run frontend build when the local sandbox allows Angular to spawn build processes

## Date
2026-04-21
