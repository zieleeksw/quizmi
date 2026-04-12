# Task 016 - Quiz attempt review clarity

## Title
make incorrect quiz review states clearly show the user's answer and the correct answer

## Goal
Improve the quiz attempt review screen so incorrect questions are immediately understandable by showing which answers the user selected, which answers were correct, and what was missed.

## Why
The current review UI can make an incorrect question look misleading because correct answers are highlighted, but the user's wrong selection is not obvious enough. That makes the result feel confusing and unintuitive.

## Scope
- [x] give wrong selected answers a distinct visual state
- [x] keep correct answers clearly visible
- [x] add explicit labels for user-selected and correct answers
- [x] add a short hint when a question was answered incorrectly

## Steps
- [x] extend the review component with helper methods for selected-wrong, selected-correct, and missed-correct states
- [x] update the answer row markup to show explicit state badges
- [x] add a review hint under incorrect questions
- [x] refine the review card styles for clearer visual separation

## Decisions
- selected wrong answers use a warm error-tinted state
- correct answers remain green-tinted
- selected correct answers show both the user's choice and the correctness state

## Data
2026-04-12
