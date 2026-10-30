# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Backend for 깜빡이리그 (BlinkerLeague), a car app that scores turn-signal and other safe-driving habits. `plan.md` is the feature-by-feature roadmap (branches `feat/<name>`, P0 due 11.15). The server is written in **Java**, even though the original 기획서 says Kotlin.

Only the common base exists so far (no domain code yet), so update this file as features land:

- `common.error`: `ErrorCode` enum → `BusinessException` → `GlobalExceptionHandler` returns `{ code, message }`. Throw `BusinessException` for domain errors rather than building responses by hand.
- `common.jpa`: entities extend `BaseTimeEntity` (audited `createdAt`/`updatedAt`).
- `common.time.TimeConfig`: all day-based logic uses Asia/Seoul. Inject `Clock` instead of calling `now()` directly so tests can pin the time.
- `security.SecurityConfig`: stateless, no CSRF/form/basic login. Only `POST /api/drivers`, `GET /api/config/**`, `/h2-console/**` are public; everything else returns a 401 JSON. Use `antMatcher(...)` for request matchers, because plain strings fail once the H2 console servlet is registered. Driver token auth is planned for `feat/driver-register`.
- Domain packages go under `com.blinkerleague.server.<domain>` with `controller/service/repository/domain/dto` subpackages (see `plan.md` §2).

## Stack

- Spring Boot 3.3.13, Java 17 toolchain, Gradle wrapper 7.6.4 (Groovy DSL), Lombok.
- Dependencies already declared: Spring MVC (`starter-web`), Bean Validation, Data JPA + H2, Data Redis, Spring Session on Redis, Spring Security.
- Boot and Gradle are deliberately kept old: the dev machine runs IntelliJ IDEA 2021.3.1, whose Gradle sync breaks on newer Gradle. Do not upgrade Boot (4.x needs Gradle 8.14+) or the wrapper without confirming the IDE is updated.
- Redis-backed sessions are on the classpath, but the app starts without a running Redis (verified with `bootRun`). Redis connections are lazy, so errors appear only once sessions or Redis are actually used.
- Profiles: `local` is the default (`application-local.yaml`: in-memory H2, console at `/h2-console`, `ddl-auto: create`, SQL logging). Tests use `@ActiveProfiles("test")` (`src/test/resources/application-test.yaml`). `UserDetailsServiceAutoConfiguration` is excluded, so no generated password is logged.
- Redis runs via `docker compose up -d` (see README).

## Commands

Use the wrapper (`gradlew.bat` on Windows, `./gradlew` in Git Bash):

- Run: `./gradlew bootRun`
- Build: `./gradlew build`
- All tests: `./gradlew test`
- Single test class: `./gradlew test --tests "com.blinkerleague.server.ServerApplicationTests"`
- Single method: `./gradlew test --tests "com.blinkerleague.server.ServerApplicationTests.contextLoads"`

No lint/format plugin is configured.

## Environment gotcha

IntelliJ IDEA 2021.3.1's Gradle sync (via its bundled Android plugin, which injects `AndroidStudioToolingPlugin`) fails with `NoClassDefFoundError: org/gradle/util/VersionNumber` on newer Gradle (seen on 9.8.2). It is unrelated to the Korean characters in the user path. Hence the pinned Gradle 7.6.4 / Boot 3.3.x. The IDE sync was not re-verified after the downgrade; the CLI wrapper build works.
