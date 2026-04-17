# Task 022 - Question rich text formatting

## Title
add rich text formatting for question content and prepare the same flow for answers

## Goal
Allow course managers to format question content visually instead of writing raw HTML, so prompts and explanations can use bold, italic, underline, and color directly inside the editor while the learner-facing quiz views render that formatting safely.

## Why
The original question editor only supported plain text, which made styled prompts and explanations awkward to author and forced the user to think in markup instead of content. Once formatting support exists, it should be shared consistently across editor and preview surfaces, and the same approach should be ready to extend to answer content.

## Scope
- [x] replace plain text prompt editing with a shared rich text editor
- [x] replace plain text explanation editing with the same shared rich text editor
- [x] add toolbar support for bold, italic, underline, and text color
- [x] render saved formatting in question management and quiz-facing views instead of exposing raw HTML
- [x] sanitize saved rich text to a safe supported subset of inline formatting
- [x] keep duplicate prompt detection and validation working against readable text content
- [x] add a lightweight preset color palette instead of requiring raw color input
- [x] reuse one shared rich text rendering layer across prompt and explanation surfaces
- [x] extend the same rich text support to answer content

## Steps
- [x] add shared frontend rich text sanitization helpers
- [x] add a shared Angular rich text HTML pipe
- [x] add a reusable rich text editor component for form controls
- [x] replace question editor `textarea` fields with the rich text editor
- [x] normalize prompt validation to check readable text length instead of raw markup length
- [x] update duplicate prompt comparison to use normalized plain text
- [x] render rich text in question bank cards
- [x] render rich text in question version history
- [x] render rich text in quiz play
- [x] render rich text in attempt review
- [x] render rich text in quiz statistics and quiz editor question lists
- [x] replace native color picker flow with a small preset color palette
- [x] update answer authoring inputs to use the same rich text editor component
- [x] render rich text answer content everywhere answers are shown
- [x] decide whether answer formatting should share the same validation limits as prompt formatting
- [x] increase stored answer content length so sanitized rich text markup fits safely
- [x] validate answer uniqueness against readable text instead of raw markup

## Decisions
- rich text support is intentionally limited to a small safe subset instead of arbitrary HTML
- prompt and explanation formatting reuse one shared editor component and one shared rendering/sanitization path
- learner-facing screens render sanitized rich text rather than escaping it back to plain text
- validation and duplicate detection operate on human-readable content, not markup tokens
- color selection uses a small preset palette for simpler authoring
- answer formatting uses the same shared editor component and sanitization rules as prompt and explanation
- answer content storage and validation limits are increased to `1000` characters including formatting markup
- answer uniqueness is evaluated from readable text so cosmetic markup alone does not make two answers distinct

## Date
2026-04-17
