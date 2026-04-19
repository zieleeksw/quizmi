# Task 028 - Quiz play session save race guard

## Title
stabilize quiz answer selection when multiple session saves overlap

## Goal
Keep answer selection stable during quiz play even when the learner clicks multiple answers quickly and session save requests return out of order.

## Why
The quiz play screen saved session state on every answer toggle. For questions with multiple answers that meant several `PUT /session` requests could overlap. When an older response arrived after a newer click, the frontend could briefly unselect and reselect answers, making the UI feel broken even though the backend eventually stored a valid state.

## Scope
- [x] stop sending a save request on every single rapid answer click
- [x] batch quick answer changes into one debounced session save
- [x] ensure only one session save is in flight at a time
- [x] prevent stale save responses from overwriting newer optimistic answer state
- [x] keep explicit question navigation saves immediate
- [x] add frontend regression coverage for rapid toggles and stale responses

## Steps
- [x] add debounced session-save scheduling in `quiz-play-page.component.ts`
- [x] add a queued save payload so newer selection state waits behind the active request
- [x] keep local optimistic session updates while save requests are pending
- [x] ignore stale server payload for `answers` and `currentIndex` when a newer save is already queued
- [x] keep `openQuestion` persistence immediate so navigation still feels reliable
- [x] add component tests for batched rapid toggles and out-of-order save completion

## Decisions
- rapid answer toggles are treated as one short interaction burst and are saved after a `250 ms` debounce
- session saves are serialized on the frontend to avoid overlapping write races
- stale responses can still refresh server-owned fields, but they must not roll back newer local answer selection
- direct question navigation keeps immediate persistence because it represents an explicit step change, not noisy input
- local TypeScript compilation was verified, but full Angular runtime verification remained blocked here by sandbox restrictions around headless browser startup

## Date
2026-04-19
