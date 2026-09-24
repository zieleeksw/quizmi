# Task 049 - Admin console for quiz attempt statistics

## Title
add an admin-only /ac console showing quiz attempts across all players, courses, and quizzes

## Goal
Let an ADMIN user see, in one place, every finished quiz attempt across the whole platform (who played, when, which course, which quiz, and the score), without opening each course individually.

## Why
There is currently no cross-course view of activity. Existing quiz statistics (`/courses/{courseId}/quizzes/{quizId}/statistics`) are scoped to a single quiz. The `quiz_attempts` table already stores everything needed (`courseId`, `quizId`, `userId`, `quizTitle`, `correctAnswers`, `totalQuestions`, `finishedAt`), but no endpoint exposes it globally, and there is no admin-only UI. An admin console gives a single overview for monitoring platform activity.

## Scope
- [x] backend: admin-only endpoint listing all quiz attempts across all users/courses, sorted by `finishedAt` desc (no pagination yet - see note below)
- [x] backend: enrich each row with course name and player email (join to `courses`/`users`, not just raw ids)
- [ ] backend: optional filters - by `courseId`, `quizId`, `userId`, date range (not implemented - not requested for MVP)
- [x] backend: endpoint restricted to `ROLE_ADMIN` (reuse existing `UserRole.ADMIN` / `ROLE_ADMIN` authority; `@EnableMethodSecurity` already on)
- [x] frontend: `/ac` route showing the list in a table (player, course, quiz, score, finished at), guarded so only ADMIN users can open it
- [x] frontend: admin route guard reusing `AuthSession.user.role` (already returned by login/register, no new backend endpoint needed for this check)

## Steps
- [x] write failing integration test `AdminIntegrationTest` (`backend/src/test/kotlin/pl/zieleeksw/quizmi/admin/AdminIntegrationTest.kt`) hitting `GET /admin/quiz-attempts` as an ADMIN user, and asserting a non-admin user gets `403` (also covers unauthenticated -> 401)
- [x] add a repository query joining `quiz_attempts` with `courses` and `users` (new `admin` package: `AdminQuizAttemptRepository`) returning `AdminQuizAttemptDto` rows (not paginated yet)
- [x] add `AdminQuizAttemptDto`, `AdminStatsFacade`, `AdminController` under `backend/src/main/kotlin/pl/zieleeksw/quizmi/admin/`
- [ ] confirm the integration test goes green - **NOT VERIFIED**: this environment has no Docker/network to run `./gradlew test`, run it locally to confirm red-then-green
- [x] add Angular `AdminConsolePageComponent` under `features/admin/`, `AdminStatsService` under `core/admin/` (`tsc --noEmit` passes; full `ng build` not confirmed to finish in this sandbox)
- [x] add `adminGuard` under `core/auth/`, wire the `/ac` route in `app.routes.ts`
