# Task 035 - Quiz attempt review details panel

## Title
add expandable question details with categories and explanation on attempt review

## Goal
Make the attempt review screen expose the same learning context as the live quiz flow by adding a single expandable question details panel with categories and explanation.

## Why
The review page already highlighted correct and incorrect answers, but it only exposed `explanation` through a narrow dedicated toggle and did not show the categories assigned to the reviewed question. That made the review experience less helpful than the live quiz flow, where both context pieces are visible after checking an answer.

## Scope
- [x] replace the explanation-only toggle with one expandable question details panel
- [x] show categories inside the expanded panel when the current question has any
- [x] show explanation inside the same panel when available
- [x] keep the panel hidden for questions that have neither categories nor explanation
- [x] add frontend component coverage for the new review interaction

## Steps
- [x] inspect the existing attempt review template and explanation toggle behavior
- [x] resolve categories from the current question data already loaded by the review page
- [x] add a unified expand / collapse state for question details
- [x] render categories above explanation inside the expanded body
- [x] update styles so the arrow toggle and inner badges match the quiz UI language
- [x] add spec coverage for expanded and hidden states

## Decisions
- the review page uses one generic `Question details` toggle instead of separate expanders for categories and explanation
- categories are taken from the current question snapshot loaded on the page, matching the existing explanation fallback approach
- categories are shown before explanation inside the panel so the learner first sees the domain and then the deeper explanation

## Date
2026-04-20
