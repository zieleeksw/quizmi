# Task 021 - Question explanation

## Title
add optional question explanation across backend and frontend

## Goal
Allow course managers to save an optional explanation together with a question so extra learning context can be attached without making it required, and make that explanation visible when learners review checked quiz answers.

## Why
Question prompts and answers were already versioned, but there was no place to store a short explanation that clarifies why an answer is correct or what the learner should remember after solving the question. Once explanation exists, it should also appear in the learner-facing quiz flows where correctness is evaluated and reviewed.

## Scope
- [x] add backend request and response support for optional `explanation`
- [x] store `explanation` on the versioned question model
- [x] add database schema for nullable question explanation
- [x] treat `explanation` as part of meaningful question version changes
- [x] add frontend question editor support for optional explanation
- [x] show saved explanation in the question bank and version history
- [x] show explanation in quiz play after checking a question
- [x] show explanation in quiz attempt review
- [x] support explanation fallback for older saved attempt reviews
- [x] keep explanation optional so empty values remain allowed

## Steps
- [x] extend `CreateQuestionRequest` and `UpdateQuestionRequest`
- [x] extend `QuestionDto` and `QuestionVersionDto`
- [x] add validator for maximum explanation length
- [x] add Liquibase migration for `question_versions.explanation`
- [x] normalize blank explanation values to `null`
- [x] include explanation in question create and update flows
- [x] include explanation in question history responses
- [x] update Angular question models and editor form
- [x] render explanation below answers in the question editor
- [x] remove helper copy below the explanation field
- [x] show explanation on question cards and version history
- [x] extend attempt review DTO and backend review snapshot with explanation
- [x] render explanation in quiz play only after the question is checked
- [x] add collapsible explanation panel on the attempt review page
- [x] fall back to current question explanation when an older attempt snapshot does not contain it yet
- [x] extend backend integration coverage for explanation responses

## Decisions
- explanation belongs to the question version, not the root question row
- explanation stays optional and may be omitted entirely
- blank explanation input is normalized to `null`
- changing only the explanation is enough to create a new question version
- the editor places explanation after answers to keep the authoring flow focused on solving the question first
- quiz play shows explanation only after the learner checks the current question
- attempt review shows explanation in a collapsible section when the question has one
- older attempt reviews may resolve explanation from the current question data when the original snapshot predates explanation support

## Date
2026-04-17
