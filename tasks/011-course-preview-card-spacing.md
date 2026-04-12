# Task 011 - Course preview card spacing polish

## Title
improve course preview card spacing and readability

## Goal
Give the course preview cards in the authenticated courses workspace more breathing room so titles, descriptions, and metadata do not feel cramped.

## Why
The existing cards are technically padded, but the internal layout still feels too dense. A small UI polish improves scanability and makes the workspace feel more intentional.

## Scope
- [x] review the current course preview card layout
- [x] separate main card content from footer metadata
- [x] increase visual breathing room inside the card
- [x] keep the existing routing and paging behavior unchanged
- [x] preserve responsive behavior for smaller screens

## Steps
- [x] add dedicated content and meta wrappers in the courses page template
- [x] adjust card spacing, footer separation, and text clamping styles
- [x] verify the frontend still builds successfully

## Decisions
- this stays a UI-only polish with no DTO or API changes
- the course list remains the same feature entry point
- metadata stays pinned to the bottom of each card for easier scanning

## Data
2026-04-12
