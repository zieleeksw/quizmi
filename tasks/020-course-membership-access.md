# Task 020 - Course membership access

## Title
add course join requests, member roles, and gated course access

## Goal
Allow courses to stay visible in the catalog while requiring an approved membership before a user can enter the full course workspace, and give the course team a way to manage members and roles.

## Why
Shared course visibility was already in place, but every authenticated user could still enter course content immediately. The product now needs a real membership layer with request-based access and course-level roles for ongoing moderation.

## Scope
- [x] add backend course membership model
- [x] add database schema for course memberships
- [x] support `ACTIVE` and `PENDING` membership states
- [x] support `OWNER`, `MODERATOR`, and `MEMBER` course roles
- [x] expose join request endpoint for non-members
- [x] expose member list and pending request list for course managers
- [x] allow approving pending requests
- [x] allow owner-level role changes between `MEMBER` and `MODERATOR`
- [x] gate course content access behind active membership
- [x] allow owner, moderator, and global admin to manage course content
- [x] update frontend course cards and course details for request-based access
- [x] add frontend `Members` page inside the course workspace
- [x] extend backend integration tests for membership flows and moderator permissions

## Steps
- [x] create course membership entity, repository, DTOs, and enums
- [x] add Liquibase migration for `course_memberships`
- [x] seed course owners as active `OWNER` members
- [x] enrich `CourseDto` with membership role, membership status, access flags, and pending request count
- [x] add `POST /courses/{courseId}/join-requests`
- [x] add `GET /courses/{courseId}/members`
- [x] add `POST /courses/{courseId}/members/{memberUserId}/approve`
- [x] add `PUT /courses/{courseId}/members/{memberUserId}/role`
- [x] split course access checks into preview, member access, and manager access
- [x] update categories, questions, quizzes, attempts, and sessions to require active membership for read flows
- [x] update manager-only flows to allow moderators in addition to owner/global admin
- [x] add request-to-join actions on the courses page and course details page
- [x] block the workspace UI when the user is not yet an active member
- [x] add a `Members` tab with pending request count for managers
- [x] add frontend member approval and member/moderator role switching

## Decisions
- course catalog visibility stays shared for authenticated users
- entering the actual course workspace now requires active membership
- course owner remains unique and is not reassigned through the new role update endpoint
- moderators can manage course content like the owner, but only the owner or global admin can change member roles
- pending requests are visible only to course managers
- global `ADMIN` still bypasses normal course membership restrictions for access and management

## Date
2026-04-15
