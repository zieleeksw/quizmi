# Task 026 - Quiz play full-width layout

## Title
make the quiz play screen match the full workspace width

## Goal
Ensure the `/play` quiz screen uses the same horizontal width as the workspace top bar instead of collapsing to the narrower default `app-panel` width.

## Why
The quiz play route rendered its main sections inside `app-panel`, which globally caps panel width at `1040px`. The workspace top bar still used the full workspace width, so the play screen looked visibly narrower and misaligned below it.

## Scope
- [x] remove the narrow `app-panel` width cap for the `/play` screen only
- [x] keep the existing quiz play spacing, card styling, and interactions unchanged
- [x] avoid changing shared panel behavior for the rest of the app

## Steps
- [x] inspect the `/play` template and styles
- [x] confirm that `app-panel` was the source of the width limit
- [x] override panel width locally in `quiz-play-page.component.scss`
- [x] document the layout fix in a dedicated task file

## Decisions
- the fix stays local to `quiz-play-page.component.scss` so other screens can keep using the default `app-panel` width
- the quiz play page should align with the workspace shell width because it behaves like an active workspace surface, not a centered form card
- no further visual changes were made to the gameplay cards themselves

## Date
2026-04-19
