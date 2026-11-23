# Task 049 - Admin console for quiz attempt statistics

## Title
add an admin-only /ac console showing quiz attempts across all players, courses, and quizzes

## Goal
Let an ADMIN user see, in one place, every finished quiz attempt across the whole platform (who played, when, which course, which quiz, and the score), without opening each course individually.

## Why
There is currently no cross-course view of activity. Existing quiz statistics (`/courses/{courseId}/quizzes/{quizId}/statistics`) are scoped to a single quiz. The `quiz_attempts` table already stores everything needed (`courseId`, `quizId`, `userId`, `quizTitle`, `correctAnswers`, `totalQuestions`, `finishedAt`), but no endpoint exposes it globally, and there is no admin-only UI. An admin console gives a single overview for monitoring platform activity.

## Scope
- [x] backend: admin-only endpoint listing all quiz attempts across all users/courses, sorted by `finishedAt` desc, paginated via `page`/`size` query params (default size 20, max 100)
- [x] backend: enrich each row with course name and player email (join to `courses`/`users`, not just raw ids)
- [x] backend: optional filters - by `courseId`, `quizId`, `userId`, combinable with AND (date range still not implemented - not requested)
- [x] backend: endpoint restricted to `ROLE_ADMIN` (reuse existing `UserRole.ADMIN` / `ROLE_ADMIN` authority; `@EnableMethodSecurity` already on)
- [x] frontend: `/ac` route showing the list in a table (player, course, quiz, score, finished at), guarded so only ADMIN users can open it
- [x] frontend: admin route guard reusing `AuthSession.user.role` (already returned by login/register, no new backend endpoint needed for this check)
- [x] backend: `GET /admin/courses` and `GET /admin/quizzes` - distinct course/quiz options (only ones with at least one attempt) for the filter dropdowns
- [x] backend: `GET /admin/users?search=` - email-substring player lookup (min 2 chars, top 10) for the player typeahead
- [x] frontend: filter panel above the table - player typeahead (debounced search-as-you-type), course dropdown, quiz dropdown, combinable with AND, `Search` applies them and `Clear` resets
- [x] frontend: quiz dropdown is dependent on the selected course (only shows quizzes belonging to it; "All courses" shows every quiz) - fixes being able to pick a course+quiz combination that don't belong together and silently getting zero results
- [x] backend+frontend: course/quiz filter options form a drill-down chain player -> course -> quiz - selecting a player narrows the course dropdown to courses that player actually has attempts in (not every course), which in turn keeps narrowing the quiz dropdown; picking a player/course that invalidates the current course/quiz selection clears it instead of leaving a dead combination

## Decisions
- pagination follows the existing `QuestionPageDto` shape/convention (`items`, `pageNumber`, `pageSize`, `totalItems`, `totalPages`, `hasNext`, `hasPrevious`) even though `QuestionPageDto` itself paginates in-memory - here pagination is done at the SQL level (Spring Data `Pageable`/`Page` inside the repository only, never exposed past the facade) because the admin console lists attempts across the whole platform with no natural upper bound, unlike a single course's questions
- default page size 20, max 100 (`AdminStatsFacade.MAX_PAGE_SIZE`)
- course/quiz filter dropdown options are derived from `quiz_attempts` itself (distinct courses/quizzes that actually have at least one recorded attempt), not from a full course/quiz listing - avoids a separate "list every course/quiz in the system" endpoint and keeps the dropdowns relevant to what can actually be filtered
- player filter has no dropdown-of-everyone (could be a lot of users) - text input with debounced (300ms) typeahead against `GET /admin/users?search=`, minimum 2 characters, capped at 10 results
- filters are combined with AND at the SQL level (`:param IS NULL OR column = :param` per filter) inside the same paginated query, not applied in-memory
- `AdminQuizOptionDto` carries `courseId` (not just `courseName`) specifically so the frontend can filter the quiz dropdown by the selected course without a second network call; selecting a course clears the quiz selection if it no longer belongs to that course
- course/quiz options are no longer filtered client-side only - `GET /admin/courses` takes an optional `userId`, `GET /admin/quizzes` takes optional `userId`+`courseId`; both scope the distinct-options query server-side, since "courses/quizzes this player has attempts in" needs the same join/filter logic as the main listing query anyway

## Steps
- [x] write failing integration test `AdminIntegrationTest` (`backend/src/test/kotlin/pl/zieleeksw/quizmi/admin/AdminIntegrationTest.kt`) hitting `GET /admin/quiz-attempts` as an ADMIN user, and asserting a non-admin user gets `403` (also covers unauthenticated -> 401)
- [x] extend `AdminIntegrationTest` with a pagination test (`page`/`size` params, `hasNext`/`hasPrevious`/`totalPages`) before adding pagination to the endpoint
- [x] add a repository query joining `quiz_attempts` with `courses` and `users` (new `admin` package: `AdminQuizAttemptRepository`) returning `Page<AdminQuizAttemptDto>` via Spring Data `Pageable` + explicit `countQuery`
- [x] add `AdminQuizAttemptDto`, `AdminQuizAttemptPageDto`, `AdminStatsFacade`, `AdminController` under `backend/src/main/kotlin/pl/zieleeksw/quizmi/admin/` - `AdminStatsFacade` normalizes `page`/`size` and wraps the Spring Data `Page` into `AdminQuizAttemptPageDto` (same shape as `QuestionPageDto`: `items`/`pageNumber`/`pageSize`/`totalItems`/`totalPages`/`hasNext`/`hasPrevious`)
- [ ] confirm the integration test goes green - **NOT VERIFIED**: this environment has no Docker/network to run `./gradlew test`, run it locally to confirm red-then-green
- [x] add Angular `AdminConsolePageComponent` under `features/admin/`, `AdminStatsService` under `core/admin/`, now server-paginated (Previous/Next, page size 20) instead of a single unpaginated list (`tsc --noEmit` passes; full `ng build` not confirmed to finish in this sandbox)
- [x] add `adminGuard` under `core/auth/`, wire the `/ac` route in `app.routes.ts`
- [x] extend `AdminIntegrationTest` again (filter combinations, `/admin/courses`, `/admin/quizzes`, `/admin/users` search) before adding filters
- [x] add `courseId`/`quizId`/`userId` params to `AdminQuizAttemptRepository.findAllForAdminConsole`, add `findDistinctCourseOptions`/`findDistinctQuizOptions`
- [x] add `AdminUserRepository` (`findTop10ByEmailContainingIgnoreCaseOrderByEmailAsc`) for the player search
- [x] add `AdminCourseOptionDto`, `AdminQuizOptionDto`, `AdminUserOptionDto`, wire new `AdminController` endpoints
- [x] add filter panel to `AdminConsolePageComponent`/`.html`/`.scss`: player typeahead with debounce + suggestions dropdown, course/quiz `<select>`, Search/Clear buttons
- [x] add `courseId` to `AdminQuizOptionDto`/`findDistinctQuizOptions` (test-first: updated the dropdown-options test to assert it), filter `filteredQuizOptions` by `selectedCourseId` in the component, clear an invalid quiz selection on course change
- [x] extend `AdminIntegrationTest` (test-first) with course/quiz options scoped by `userId`/`courseId`, then add the `userId` param to `findDistinctCourseOptions` and `userId`+`courseId` params to `findDistinctQuizOptions`, thread through `AdminStatsFacade`/`AdminController`
- [x] rework `AdminConsolePageComponent`: `refreshCourseOptions()` (scoped by selected player) always cascades into `refreshQuizOptions()` (scoped by player+course); both drop the current selection if it no longer appears in the refreshed options
