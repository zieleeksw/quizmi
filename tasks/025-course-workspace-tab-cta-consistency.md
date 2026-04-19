# Task 025 - Course workspace tab CTA consistency

## Title
align workspace tab action buttons and stabilize the questions tab background

## Goal
Make the `Create` actions inside the course workspace tabs feel identical across `categories`, `questions`, and `quizzes`, and prevent the `questions` tab from visually changing the whole workspace background when it opens.

## Why
The course workspace had subtle UI drift between tabs. In `quizzes`, the primary `Create` action turned overly white on hover. In `questions`, the same action lacked the expected hover and focus behavior seen in `categories`. The `questions` tab also made the lower workspace area look like its full background changed, which broke the shared-shell feel introduced earlier.

## Scope
- [x] unify the primary `Create` action styling across `categories`, `questions`, and `quizzes`
- [x] make `quizzes` use the same primary CTA interaction model as the other workspace tabs
- [x] preserve disabled create states and tooltip behavior in `questions` and `quizzes`
- [x] keep the shared course workspace shell visually stable when switching to `questions`
- [x] avoid changing quiz, question, or category business behavior outside these UI fixes

## Steps
- [x] compare the `Create` action markup and styles in `course-categories-page`, `course-questions-page`, and `course-quizzes-page`
- [x] update the `quizzes` create action to use the same link-based pattern as the other tabs
- [x] add matching hover and focus states for the `questions` primary CTA
- [x] stop the `quizzes` primary CTA from inheriting the base hover background that washed it out to white
- [x] verify that the page background shift came from the global page gradient reacting to document height
- [x] keep the final background fix in global `styles.scss` by pinning the page gradient to the viewport
- [x] remove exploratory workspace-shell and questions-surface tweaks after confirming they were not needed

## Decisions
- `categories` is treated as the visual source of truth for the workspace `Create` CTA behavior
- the `quizzes` primary `Create` action should navigate like the other tabs instead of relying on a button click for normal access
- disabled tooltip affordances remain in place because they explain why creation is blocked
- the real background issue came from the global page gradient moving as the document height changed, not from the tab shell itself
- the final fix is intentionally minimal: keep the button consistency work and the `styles.scss` background pinning, remove the intermediate local background experiments
- local verification was partially blocked because `ng build` failed in the sandbox with `spawn EPERM`, so the change was reviewed structurally in code instead

## Date
2026-04-19
