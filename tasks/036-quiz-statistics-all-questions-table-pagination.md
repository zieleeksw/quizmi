# Task 036 - Quiz statistics all questions table pagination

## Title
restore the all questions table on quiz statistics and keep progressive loading without row expansion

## Goal
Bring back the original table layout in the `All questions` section of quiz statistics, while still showing questions in batches so the list stays readable for larger quizzes.

## Why
The temporary switch away from the original table layout changed the visual structure of the section more than intended. The user wanted to keep the familiar table presentation, avoid any expand/collapse interaction, and still limit the initial amount of rendered rows.

## Scope
- [x] keep `All questions` as a table instead of a card list
- [x] remove any expand/collapse behavior from this section
- [x] show only the first 10 questions initially
- [x] add a `Show X more` button below the table using the real remaining count
- [x] keep sorting from worst accuracy to best accuracy
- [x] leave the other quiz statistics sections unchanged
- [x] update frontend tests to cover the restored table behavior

## Steps
- [x] add component state for visible question count in batches of `10`
- [x] render `All questions` from `visibleQuestionStats()` instead of the full collection
- [x] restore the original table markup and column layout for the section
- [x] show the progressive loading button only when rows remain hidden
- [x] restore the table-specific styles used by the section
- [x] add a component spec for initial 10 rows, ordering, and the `Show X more` button
- [x] update the spec to verify the remaining rows appear after clicking the button

## Decisions
- the `All questions` section keeps its original table layout rather than reusing the `Most problematic questions` card style
- progressive disclosure is handled only by appending more table rows, not by expanding individual questions
- the button label shows the actual remaining number of questions, for example `Show 3 more`
- existing accuracy ordering logic remains unchanged and continues to sort from lowest to highest

## Date
2026-04-21
