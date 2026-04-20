# Task 034 - Quiz play feedback order and categories

## Title
move quiz-play feedback below actions and show question categories after checking

## Goal
Adjust the quiz play screen so post-check feedback does not shift the action buttons upward, and show the current question categories together with the explanation after the learner checks the answer.

## Why
When `explanation` appeared above the action buttons, the controls changed position after checking an answer and the user had to scroll back to continue. The screen also did not expose the categories assigned to the current question, even though they are useful learning context once the answer has been checked.

## Scope
- [x] move the `explanation` block below the quiz play action buttons
- [x] show question categories only after the current question is checked
- [x] render categories above `explanation`
- [x] keep categories hidden when the question has none
- [x] keep the existing check / next / finish flow unchanged
- [x] add component coverage for the new feedback order

## Steps
- [x] inspect the `quiz-play` template and confirm where feedback is rendered
- [x] move the explanation panel below `.quiz-play-actions`
- [x] add a categories panel between actions and explanation
- [x] style the categories row to match existing category pills used elsewhere in the app
- [x] extend the component spec to verify the new render order and visibility conditions

## Decisions
- categories are shown only after checking the current question, just like `explanation`
- the categories block is rendered before `explanation` so the learner first sees the domain context and then the detailed explanation
- the change stays local to the `quiz-play` page and does not modify shared category components or global layout behavior

## Date
2026-04-20
