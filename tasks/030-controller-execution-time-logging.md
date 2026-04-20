# Task 030 - Controller execution time logging

## Title
add backend timing logs for REST controller execution

## Goal
Log how long each REST controller method takes so slow endpoints are easier to spot and improve later.

## Why
Without request timing in logs it is hard to see which backend endpoints are getting slower over time. Adding a shared timing mechanism at the controller layer gives quick visibility without touching each controller manually.

## Scope
- [x] add Spring AOP dependencies required for controller timing
- [x] add one shared aspect for classes annotated with `@RestController`
- [x] log HTTP method, request path, controller method, and execution time
- [x] log both successful requests and requests ending with an exception
- [x] add focused test coverage for the aspect behavior

## Steps
- [x] inspect the backend stack and confirm Spring Boot with Kotlin controllers
- [x] add `spring-aop` and `aspectjweaver` dependencies in `backend/build.gradle.kts`
- [x] create `ControllerExecutionTimeLoggingAspect` under `backend/src/main/kotlin/pl/zieleeksw/quizmi/logging`
- [x] measure duration with `System.nanoTime()` and emit logs in milliseconds
- [x] add test coverage in `backend/src/test/kotlin/pl/zieleeksw/quizmi/logging`

## Decisions
- AOP was chosen because it keeps timing logic out of individual controllers and applies consistently across the whole REST layer
- the logging pointcut targets `@RestController` classes, which keeps the scope focused on API entrypoints
- the aspect is disabled by default and can be enabled with `QUIZMI_CONTROLLER_EXECUTION_TIME_LOGGING_ENABLED=true`
- success cases are logged at `INFO`, while failed executions are logged at `WARN`
- log entries include request method and URI so they are useful during later performance analysis
- automated Gradle execution could not be completed in this environment because the wrapper needed network access to download Gradle

## Date
2026-04-20
