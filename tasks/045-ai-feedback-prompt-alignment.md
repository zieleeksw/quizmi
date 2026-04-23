# Task 045 - AI feedback prompt alignment

## Title
improve ai personalized feedback prompt alignment

## Goal
Refine the external AI feedback prompt so it better matches the theoretical assumptions, especially around formative feedback, structured output, scaffolding, and safe prompt design.

## Why
The first version of the personalized feedback prompt was technically correct and concise, but it did not fully reflect the didactic framing described in chapter two of the thesis.

The thesis places strong emphasis on:

- structured output
- immediate formative feedback
- diagnosis of the learner's misunderstanding
- scaffolding instead of giving away full solutions
- conservative behavior that reduces unsupported model claims

The prompt should therefore encode these expectations directly.

## Scope
- [x] keep the prompt concise enough for fast feedback generation
- [x] explicitly frame the response as formative feedback for an incorrect answer
- [x] require diagnosis of the misunderstanding based on selected and missed answers
- [x] forbid unsupported claims beyond question context
- [x] add a short, structured three-part response format
- [x] prefer guidance and hinting over full ready-made solutions
- [x] keep language adaptation tied to the learner question language

## Steps
- [x] inspect the existing `buildUserMessage(...)` prompt in `AiFeedbackGenerator`
- [x] identify which parts already matched chapter two and which parts were missing
- [x] expand the prompt with explicit formative-feedback framing
- [x] add output structure constraints for easier scanning in the UI
- [x] add a conservative instruction to reduce hallucination risk
- [x] reinforce scaffolding by preferring a hint over a full solution

## Decisions
- the response is constrained to exactly three short parts so the output is easier to scan and better aligned with the prompt-structuring ideas described in chapter two
- the prompt now explicitly diagnoses misunderstanding instead of only explaining correctness, which better matches the formative feedback model
- the instruction avoids full solution dumping and pushes the model toward hint-based support, which is closer to the scaffolding approach from the thesis
- conservative grounding was added so the model stays tied to the provided question, answers, and explanation instead of drifting into unsupported claims
- the prompt remains in a single `user` message because some routed providers rejected `system` / developer instruction messages

## Verification
- [x] reviewed the updated prompt text statically
- [x] confirmed the payload format still uses a single `user` message
- [ ] verify on a live provider that the returned feedback now follows the three-part structure consistently

## Date
2026-04-23
