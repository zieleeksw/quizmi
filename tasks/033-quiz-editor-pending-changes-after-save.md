# Task 033 - Quiz editor pending changes after save

## Title
stop false unsaved-changes warnings after a successful quiz save

## Goal
Make the quiz editor stop showing the leave-page pending-changes warning right after a successful save when the quiz state already matches what was just persisted.

## Why
The quiz editor could show a success toast and still warn about unsaved changes on the next navigation. Two frontend mismatches caused that behavior: `category` quizzes compared the saved state against the wrong `randomCount`, and the create flow navigated away while the component still treated the form like a fresh unsaved draft.

## Scope
- [x] fix false pending-changes detection after saving an existing `category` quiz
- [x] fix false pending-changes detection after creating a new quiz and redirecting to its overview
- [x] make pending-changes comparison rely on the last UI-saved draft instead of route mode inference alone
- [x] keep the current pending-changes dialog and save UX unchanged
- [x] verify the updated component still passes local TypeScript compilation

## Steps
- [x] inspect `quiz-editor-page.component.ts` save flow, `hasPendingChanges`, and the pending-changes guard wiring
- [x] identify the `category` mode snapshot mismatch around `randomCount`
- [x] identify the create-flow redirect case where the component still behaved like an unsaved new draft
- [x] add a `savedDraft` snapshot in `quiz-editor-page.component.ts`
- [x] refresh `savedDraft` after initial quiz load and after a successful save
- [x] compare the current draft against `savedDraft` inside `hasPendingChanges`
- [x] run `tsc -p tsconfig.app.json --noEmit` in `frontend`

## Decisions
- pending-changes state in the quiz editor should be derived from the last draft the UI considers saved, not recomputed indirectly from `isEditing`
- the fix stays local to `quiz-editor-page.component.ts` because the existing shared guard and dialog behavior are still correct
- local TypeScript verification passed, but full Angular build verification remained blocked here by sandbox process restrictions around `ng build`

## Date
2026-04-20
