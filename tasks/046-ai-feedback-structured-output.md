# Task 046 - AI feedback structured output

## Title
replace raw ai feedback text with structured output for controlled quiz ui rendering

## Goal
Make personalized AI feedback easier to read and better aligned with the thesis by requiring a structured model response, parsing it in the backend, and rendering dedicated sections in the quiz UI instead of showing raw model text directly.

## Why
The previous implementation displayed the model response as a single free-form text block. In practice this produced output such as numbered English sections embedded directly in one paragraph, which was not user-friendly and did not reflect the output-structuring ideas described in thesis chapter 2.3.3.

The thesis explicitly emphasizes:

- controlled syntax for downstream IT systems
- structured output that is easier to process programmatically
- reduced dependence on raw free-form text in the UI layer

This improvement brings the implementation closer to that design.

## Scope
- [x] change AI feedback DTO from a single `feedback` string to structured fields
- [x] ask the model to return JSON only
- [x] parse JSON output in the backend
- [x] keep a secondary parser for previously seen labeled text responses
- [x] keep a structured local fallback when model output is invalid or unavailable
- [x] render three separate sections in the quiz play UI
- [x] preserve the existing personalized feedback button flow

## Steps
- [x] update `AiFeedbackDto` to expose `misunderstanding`, `reasoning`, and `hint`
- [x] change the prompt so the model returns only valid JSON with the expected schema
- [x] add backend parsing for direct JSON responses
- [x] add backend fallback parsing for labeled text responses such as `What was misunderstood`
- [x] update the local fallback builder to return the same structured DTO
- [x] update frontend TypeScript models to match the structured response
- [x] replace the single AI feedback paragraph in `quiz-play-page.component.html` with three explicit UI sections
- [x] keep frontend verification via `tsc --noEmit` and `ngc -p tsconfig.app.json`

## Decisions
- the backend remains responsible for normalizing model output before it reaches the browser
- JSON was chosen as the target format because it best matches the thesis discussion on structured outputs for IT systems
- a tolerant parser was retained for older or non-compliant responses so the UI remains resilient
- the UI labels are rendered by the frontend, not by the model, which keeps the visual presentation stable and more user-friendly
- the model still receives one `user` message only, because some routed providers rejected separate `system` instructions

## Verification
- [x] reviewed the backend parser and DTO changes statically
- [x] confirmed the prompt now requests JSON-only output
- [x] ran `frontend/node_modules/.bin/tsc.cmd -p tsconfig.app.json --noEmit`
- [x] ran `frontend/node_modules/.bin/ngc.cmd -p tsconfig.app.json`
- [ ] verify live model behavior against OpenRouter to confirm stable JSON adherence across routed free providers

## Date
2026-04-23
