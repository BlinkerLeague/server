# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Backend for 깜빡이리그 (BlinkerLeague), a car app that scores turn-signal and other safe-driving habits. `plan.md` is the feature-by-feature roadmap (branches `feat/<name>`, P0 due 11.15). The server is written in **Java**, even though the original 기획서 says Kotlin.

Only the common base exists so far (no domain code yet), so update this file as features land:

- `common.error`: `ErrorCode` enum → `BusinessException` → `GlobalExceptionHandler` returns `{ code, message }`. Throw `BusinessException` for domain errors rather than building responses by hand.
- `common.jpa`: entities extend `BaseTimeEntity` (audited `createdAt`/`updatedAt`).
- `common.time.TimeConfig`: all day-based logic uses Asia/Seoul. Inject `Clock` instead of calling `now()` directly so tests can pin the time.
- `security.SecurityConfig`: stateless, no CSRF/form/basic login. Only `POST /api/drivers`, `GET /api/config/**`, `/h2-console/**` are public; everything else returns a 401 JSON. Use `antMatcher(...)` for request matchers, because plain strings fail once the H2 console servlet is registered. Driver token auth is planned for `feat/driver-register`.
- Domain packages go under `com.blinkerleague.server.<domain>` and follow the hexagonal layout below. This replaces the `controller/service/repository/domain/dto` layout in `plan.md` §2.

## Architecture: hexagonal (ports and adapters) first

Build every new feature in the hexagonal layout. The domain and use cases sit in the middle, and outside technology (web, JPA, Redis, JWT) connects only through ports. Dependencies always point inward: `adapter → application → domain`.

```
com.blinkerleague.server.<domain>
├── domain/                  # domain model and rules
├── application/
│   ├── port/in/             # use case interfaces (optional, see below)
│   ├── port/out/            # interfaces for outside technology: SaveXxxPort, LoadXxxPort, XxxStore
│   └── service/             # use case implementations, depend only on domain and port/out
└── adapter/
    ├── in/web/              # controllers, request/response DTOs
    └── out/<tech>/          # port/out implementations: persistence (JPA), redis, jwt, ...
```

We use a pragmatic version to keep pace with the P0 deadline:

- **Out-ports are required.** A service never touches a JPA repository, `RedisTemplate`, or `JwtEncoder` directly. It calls a `port/out` interface, and an adapter in `adapter/out/<tech>` implements it.
- **In-ports are optional.** A controller may call a service class directly. Add a `port/in` interface when a use case has more than one caller or needs to be swapped.
- **The domain may carry JPA annotations.** A domain class may be the JPA entity (and extend `BaseTimeEntity`), so no separate entity and mapper are needed. It must not depend on web, Redis, or security classes. Pure logic such as score calculation stays free of Spring entirely.
- Shared, cross-domain code (`common.*`, `security.SecurityConfig`, framework config such as JWT encoder/decoder beans) stays outside the domain packages.

Testing follows the ports. Unit-test services with in-memory fakes of their out-ports, as `InMemoryRefreshTokenStore` does for `RefreshTokenStore`. Test adapters against the real technology: MockMvc for web, H2 for JPA, Testcontainers for Redis.

Code written before this rule, such as `security.jwt` on `feat/jwt-auth`, gets moved into this layout (for example `auth/application/...` and `auth/adapter/out/redis`) before or while its feature is finished.

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
