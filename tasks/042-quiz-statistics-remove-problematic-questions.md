# Task 042 - Quiz statistics remove problematic questions

## Title
remove duplicate most problematic questions section from quiz statistics

## Goal
Simplify the quiz statistics page by removing the `Drill-down / Most problematic questions` section.

## Why
The page already shows `All questions` below, sorted from the weakest accuracy to the strongest. The separate `Most problematic questions` block repeated the same information in a shorter card list, which made the statistics screen noisier without adding new insight.

## Scope
- [x] remove the `Drill-down / Most problematic questions` section
- [x] keep the `All questions` table
- [x] keep the existing worst-to-best question sorting
- [x] keep progressive loading for the full table
- [x] remove CSS used only by the removed question cards

## Steps
- [x] delete the problematic questions section from `quiz-statistics-page.component.html`
- [x] remove unused `.quiz-statistics-question-card` styles
- [x] keep `.quiz-statistics-question-list__actions` and button styles for `Show more`

## Decisions
- `All questions` remains the single per-question analysis surface
- the lower table continues to be the source of truth for weakest questions
- no statistics calculations or API calls changed

## Verification
- [x] checked that removed text/classes are gone
- [x] ran `git diff --check` for the touched statistics files

## Date
2026-04-22
