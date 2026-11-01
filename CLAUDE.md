# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Freshly generated Spring Boot skeleton (Spring Initializr). The only source is `ServerApplication` and a context-load test in `com.blinkerleague.server`; `application.yaml` only sets `spring.application.name`. There is no domain code yet, so update this file as architecture emerges.

## Stack

- Spring Boot 3.3.13, Java 17 toolchain, Gradle wrapper 7.6.4 (Groovy DSL), Lombok.
- Dependencies already declared: Spring MVC (`starter-web`), Data JPA + H2, Data Redis, Spring Session on Redis, Spring Security.
- Boot and Gradle are deliberately kept old: the dev machine runs IntelliJ IDEA 2021.3.1, whose Gradle sync breaks on newer Gradle. Do not upgrade Boot (4.x needs Gradle 8.14+) or the wrapper without confirming the IDE is updated.
- Redis-backed sessions are on the classpath, but the app starts without a running Redis (verified with `bootRun`). Redis connections are lazy, so errors appear only once sessions or Redis are actually used.
- Security locks all endpoints by default and logs a generated dev password at startup. H2 is in-memory; the console is off unless `spring.h2.console.enabled=true`.

## Commands

Use the wrapper (`gradlew.bat` on Windows, `./gradlew` in Git Bash):

- Run: `./gradlew bootRun`
- Build: `./gradlew build`
- All tests: `./gradlew test`
- Single test class: `./gradlew test --tests "com.blinkerleague.server.ServerApplicationTests"`
- Single method: `./gradlew test --tests "com.blinkerleague.server.ServerApplicationTests.contextLoads"`

No lint/format plugin is configured.

## Development workflow: red-green TDD

`feat/common-setup` (the first feat) is done. Build every feat after it test-first, in red → green → refactor cycles:

1. **Red**: Pick one behavior, usually one of the "테스트:" cases listed for the feat in `plan.md`. Write a failing test for it, then run that test alone and confirm it fails for the expected reason (assertion failure, missing endpoint, wrong status). A compile error or a mistyped URL is not a valid red.
2. **Green**: Write the minimum production code that makes the test pass. Run that test again, then `./gradlew test` to make sure nothing else broke.
3. **Refactor**: Clean up the code and the test while keeping them green. Do not add behavior that has no test yet.
4. Repeat for the next behavior. Do not write production code before a failing test for it exists.

Conventions:

- Test names are Korean sentences that describe the behavior, e.g. `인증없이_보호된_API를_호출하면_401_JSON을_반환한다`.
- Pure domain logic (e.g. `ScoreCalculator`) gets plain JUnit unit tests without Spring. API behavior gets `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` tests that assert status and `$.code` (see `SecurityConfigTest`, `GlobalExceptionHandlerTest`).
- Pin time by injecting a fixed `Clock` in tests and never depend on the real date.
- When reporting progress, show the red run (failing output) and the green run for each cycle. A feat PR is ready only when every `plan.md` test case for it exists and `./gradlew build` passes.

## Environment gotcha

IntelliJ IDEA 2021.3.1's Gradle sync (via its bundled Android plugin, which injects `AndroidStudioToolingPlugin`) fails with `NoClassDefFoundError: org/gradle/util/VersionNumber` on newer Gradle (seen on 9.8.2). It is unrelated to the Korean characters in the user path. Hence the pinned Gradle 7.6.4 / Boot 3.3.x. The IDE sync was not re-verified after the downgrade; the CLI wrapper build works.
