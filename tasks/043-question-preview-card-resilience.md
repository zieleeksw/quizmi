# Task 043 - Question preview card resilience

## Title
make question preview cards more compact and resilient

## Goal
Reduce the visual height of question preview cards and prevent long rich-text prompts, explanations, categories, or answers from stretching the question library layout.

## Why
Question previews were rendering full prompts, full explanations, and all answers as large stacked blocks. A single verbose question could take over most of the viewport, making the question bank harder to scan.

## Scope
- [x] make question preview cards denser
- [x] move metadata into the card header
- [x] clamp long prompts, explanations, and answer content
- [x] keep categories visible without allowing them to grow the card indefinitely
- [x] keep answer correctness styling
- [x] preserve owner edit links and locked-course read-only rendering

## Steps
- [x] update `course-questions-page.component.html` card structure
- [x] move created/updated/access metadata into the top row
- [x] update `course-questions-page.component.scss` card spacing, radius, and typography
- [x] add responsive constraints for prompt, explanation, badges, and answers
- [x] keep mobile answers full-width for readability

## Decisions
- previews favor scanability over showing full rich-text content
- the full question remains available through the existing edit/detail navigation path
- no API, DTO, or backend behavior changed
- CSS handles oversized content with line clamps and `overflow-wrap`

## Verification
- [x] reviewed the diff statically
- [x] checked that only the question preview template and styles changed
- [ ] run frontend build when Angular can spawn outside the sandbox

## Date
2026-04-22
