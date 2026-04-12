# Task 015 - Course workspace UI polish

## Title
redesign the course workspace into one shared, premium content system

## Goal
Make sure entering a course always opens visible workspace content and turn the lower course area into one cohesive, premium workspace with full-width tabs, shared section scaffolding, consistent pagination, and responsive content layouts.

## Why
Recent UI changes made the course workspace easy to break into an empty state, introduced nested-card noise, and duplicated nearly identical UI structure across quizzes, questions, and categories. The workspace should read as one strong surface with shared interaction patterns and clear hierarchy.

## Scope
- [x] ensure opening a course lands on visible workspace content instead of an empty shell
- [x] tighten the course workspace tab active-state behavior
- [x] soften the active tab styling and improve hover and focus feedback
- [x] move workspace tabs into the top of the lower course card and make them full width
- [x] remove inner panel wrappers from quizzes, questions, and categories
- [x] create a reusable shared workspace section component
- [x] support shared header, search, action, filter, body, and pagination patterns
- [x] migrate quizzes, questions, and categories to the shared workspace section
- [x] move pagination controls into the shared component
- [x] add paginated 3x3 browsing for categories
- [x] keep the updated layout consistent with the current course details redesign

## Steps
- [x] add a fallback navigation from bare course route to the default workspace section
- [x] make the course details outlet markup explicit
- [x] use exact active matching on workspace tabs
- [x] rebalance the tab colors, borders, and shadows for a calmer active state
- [x] replace the separate tab bar wrapper with a shared workspace shell card
- [x] convert the tabs to a full-width grid layout
- [x] flatten quizzes, questions, and categories so they render directly inside the workspace shell
- [x] add a shared workspace section component for repeated section structure
- [x] wire shared search handling through inputs and outputs
- [x] project action, filters, body, and pagination content from each section
- [x] move shared pagination controls into the reusable component
- [x] refactor categories to use 9 items per page in a 3 / 2 / 1 responsive grid

## Decisions
- the default workspace section remains `quizzes`
- active tabs should feel selected, not like a destructive or primary CTA
- course workspace navigation should remain compact and consistent with the warm visual language already used on the page
- the workspace remains organized around `quizzes`, `questions`, and `categories`
- the course details page owns the primary workspace surface
- the shared component owns only common section structure, not domain-specific content
- pagination controls live in one shared place alongside the shared section shell
- categories use 9 items per page on desktop to match the requested 3x3 rhythm

## Data
2026-04-12
