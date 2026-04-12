# Task 015 - Course workspace UI polish

## Title
fix empty course workspace state and improve active tab readability

## Goal
Make sure entering a course always opens visible workspace content and adjust the tab styling so the active state feels calmer, clearer, and more user-friendly.

## Why
Recent UI changes made the course workspace easy to break into an empty state and pushed the active tab styling too close to a bright call-to-action button, which hurts readability and navigation comfort.

## Scope
- [x] ensure opening a course lands on visible workspace content instead of an empty shell
- [x] tighten the course workspace tab active-state behavior
- [x] soften the active tab styling and improve hover and focus feedback
- [x] keep the updated layout consistent with the current course details redesign

## Steps
- [x] add a fallback navigation from bare course route to the default workspace section
- [x] make the course details outlet markup explicit
- [x] use exact active matching on workspace tabs
- [x] rebalance the tab colors, borders, and shadows for a calmer active state

## Decisions
- the default workspace section remains `quizzes`
- active tabs should feel selected, not like a destructive or primary CTA
- course workspace navigation should remain compact and consistent with the warm visual language already used on the page

## Data
2026-04-12
