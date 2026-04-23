# Task 044 - AI personalized feedback via OpenRouter

## Title
add on-demand personalized quiz feedback with external model logging and OpenRouter routing

## Goal
Let the learner request personalized feedback for an incorrect quiz answer through a dedicated button, send that prompt to an external model provider through OpenRouter, and keep a safe local fallback when the provider is unavailable.

## Why
The application already supported static post-check feedback through `explanation`, but the thesis direction requires a more intelligent tutor-like flow. At the same time, external model calls should stay explicit and visible to the learner instead of being triggered automatically after every wrong answer.

Provider behavior also required prompt-format adjustments:

- some free routed models rejected `system` / developer instructions
- some free routed models were temporarily rate-limited upstream

The implementation therefore needed a manual trigger in the UI, provider-aware request formatting, and readable backend logs for external prompt execution.

## Scope
- [x] add backend DTOs, facade, controller, and configuration for AI feedback generation
- [x] add frontend request models and service method for quiz feedback generation
- [x] show a dedicated `Generate personalized feedback` button only for checked incorrect answers
- [x] stop generating AI feedback automatically on `Check answer`
- [x] call OpenRouter-compatible `chat/completions` from the backend
- [x] send the tutor instruction as a single `user` message instead of a separate `system` message
- [x] add backend logs showing when an external prompt is sent and what status comes back
- [x] keep a local fallback `Guided feedback` response when AI is disabled or provider calls fail
- [x] make OpenRouter the default provider configuration

## Steps
- [x] inspect the existing quiz-play feedback flow and locate the correct trigger point in `QuizPlayPageComponent`
- [x] add backend `feedback` module classes under `backend/src/main/kotlin/pl/zieleeksw/quizmi/feedback`
- [x] expose `POST /courses/{courseId}/quizzes/{quizId}/questions/{questionId}/feedback`
- [x] add `app.ai.feedback.*` configuration in `backend/src/main/resources/application.yaml`
- [x] add frontend `AiFeedbackDto` and `generateFeedback(...)` API call in `attempt.service.ts`
- [x] render a dedicated action block in `quiz-play-page.component.html`
- [x] store per-question AI feedback and loading state in `quiz-play-page.component.ts`
- [x] replace Spring `RestClient` usage with JDK `HttpClient` to avoid problematic imports and keep the client lightweight
- [x] change the provider payload so only a `user` message is sent
- [x] add structured `[AI-FEEDBACK]` backend log messages for send / response / fallback cases
- [x] verify frontend typing and template compilation with `tsc --noEmit` and `ngc -p tsconfig.app.json`

## Decisions
- external personalized feedback is user-triggered, not automatic, so the learner explicitly decides when to send an answer to the external provider
- the UI shows the trigger only after a checked incorrect answer, which keeps the happy path quiet and avoids unnecessary provider calls
- OpenRouter is the default base URL because it gives a simple routing layer for free models and compatible paid providers
- the default model is `openrouter/free` so routing is more resilient than pinning the feature to one overloaded free model
- the tutor instruction is embedded into the `user` message because some routed providers rejected `system` / developer instructions with `400 INVALID_ARGUMENT`
- backend logs use a dedicated `[AI-FEEDBACK]` prefix so external prompt activity is easy to spot in application logs
- provider failures and rate limits do not break quiz flow; the backend falls back to guided local feedback instead
- the external API key is read only from environment variables and is not stored in repository files

## Verification
- [x] reviewed the backend and frontend changes statically
- [x] confirmed there is no remaining `system` message in the AI feedback payload
- [x] ran `frontend/node_modules/.bin/tsc.cmd -p tsconfig.app.json --noEmit`
- [x] ran `frontend/node_modules/.bin/ngc.cmd -p tsconfig.app.json`
- [ ] run backend compilation/tests when Gradle wrapper download/execution is allowed outside the sandbox
- [ ] verify live OpenRouter responses end-to-end with a valid active API key and available upstream model capacity

## Date
2026-04-23
