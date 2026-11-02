# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Backend for 깜빡이리그 (BlinkerLeague), a car app that scores turn-signal and other safe-driving habits. `plan.md` is the feature-by-feature roadmap (branches `feat/<name>`, P0 due 11.15). The server is written in **Java**, even though the original 기획서 says Kotlin.

Implemented so far: the common base, driver authentication (`auth`), and driver registration (`driver`). Update this file as features land:

- `common.error`: `ErrorCode` enum → `BusinessException` → `GlobalExceptionHandler` returns `{ code, message }`. Throw `BusinessException` for domain errors rather than building responses by hand.
- `common.jpa`: entities extend `BaseTimeEntity` (audited `createdAt`/`updatedAt`).
- `common.time.TimeConfig`: all day-based logic uses Asia/Seoul. Inject `Clock` instead of calling `now()` directly so tests can pin the time.
- `security.SecurityConfig`: stateless, no CSRF/form/basic login. Drivers authenticate with `Authorization: Bearer <access JWT>`, verified by Spring's OAuth2 resource server. Only `POST /api/drivers`, `POST /api/auth/refresh`, `GET /api/config/**`, `/h2-console/**` are public. A missing token returns 401 `UNAUTHORIZED`, and an invalid, forged, or expired token returns 401 `INVALID_TOKEN`, both as `{ code, message }` JSON. Use `antMatcher(...)` for request matchers, because plain strings fail once the H2 console servlet is registered.
- `security.jwt`: `JwtProperties` (`jwt.secret`, `jwt.access-ttl` 1h, `jwt.refresh-ttl` 30d) and `JwtConfig` (HS256 `JwtEncoder`/`JwtDecoder` beans, expiry checked against the injected `Clock` with zero skew). Startup fails if the secret is missing, an unresolved `${...}` placeholder, or shorter than 32 bytes.
- `driver`: registration, in the hexagonal layout. `domain.Driver` is also the JPA entity (`driver` table, unique `nickname` up to 12 chars, 5-digit `region_code`, extends `BaseTimeEntity`). `application.service.RegisterDriverService` rejects a taken nickname with `CONFLICT`, saves, then issues tokens through `port.out.IssueDriverTokensPort`, implemented by `adapter.out.auth.AuthDriverTokensAdapter` on top of `auth`. `adapter.out.persistence.DriverPersistenceAdapter` uses `saveAndFlush` and turns only a unique-constraint violation into `CONFLICT` (a concurrent duplicate). `adapter.in.web.DriverController` serves `POST /api/drivers` (nickname 2–12 chars, region code `\d{5}`). There is no login: a driver who loses the tokens registers again (P0 decision, recovery is decided in P1).
- `auth`: driver tokens, in the hexagonal layout. `application.port.out` has `AccessTokenIssuer` and `RefreshTokenStore`. `application.service.RefreshTokenService` issues and rotates refresh tokens (stores SHA-256 hashes only, a reused token revokes the driver's current token). `application.service.TokenReissueService` returns a new access + refresh pair. Adapters: `adapter.out.jwt.JwtAccessTokenIssuer` (access JWT, `sub` = driverId), `adapter.out.redis.RedisRefreshTokenStore` (keys `auth:refresh:{hash}`, `auth:refresh:driver:{id}`, `auth:refresh:used:{hash}`, all with TTL, `GETDEL` on consume), `adapter.in.web.AuthController` (`POST /api/auth/refresh`), and `@CurrentDriver Long driverId` for controllers. `auth.config.AuthBeanConfig` wires ports to adapters. Issuing tokens at driver registration is left for `feat/driver-register`.
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

Testing follows the ports. Unit-test services with in-memory fakes of their out-ports, as `support.InMemoryRefreshTokenStore` does for `RefreshTokenStore`. Test adapters against the real technology: MockMvc for web, H2 for JPA, Testcontainers for Redis.

`HexagonalArchitectureTest` (ArchUnit) enforces the dependency rules in the build: `application` must not depend on `adapter`, `domain` must not depend on `application` or `adapter`, and `application` must not use Spring Web, Redis, OAuth2/JWT, servlet, or `com.blinkerleague.server.security` classes directly. Pass framework-free values (for example a `Duration` instead of `JwtProperties`) into services.

## Stack

- Spring Boot 3.3.13, Java 17 toolchain, Gradle wrapper 7.6.4 (Groovy DSL), Lombok.
- Dependencies already declared: Spring MVC (`starter-web`), Bean Validation, Data JPA + H2, Data Redis, Spring Security + OAuth2 resource server (JWT). Tests use ArchUnit and Testcontainers.
- Boot and Gradle are deliberately kept old: the dev machine runs IntelliJ IDEA 2021.3.1, whose Gradle sync breaks on newer Gradle. Do not upgrade Boot (4.x needs Gradle 8.14+) or the wrapper without confirming the IDE is updated.
- Redis-backed sessions are on the classpath, but the app starts without a running Redis (verified with `bootRun`). Redis connections are lazy, so errors appear only once sessions or Redis are actually used.
- Profiles: `local` is the default (`application-local.yaml`: in-memory H2, console at `/h2-console`, `ddl-auto: create`, SQL logging). Tests use `@ActiveProfiles("test")` (`src/test/resources/application-test.yaml`). `UserDetailsServiceAutoConfiguration` is excluded, so no generated password is logged.
- Redis runs via `docker compose up -d` (see README).

## Security rules: secrets and tokens

Build defensively. Treat every secret as if the repository were public.

- **No real secret in the repository, ever.** Real values come from environment variables now and from Vault later (Spring Cloud Vault via `spring.config.import` in the deployed profile). The only secret-like values committed are the test-profile values in `src/test/resources/application-test.yaml`, which protect nothing, and empty placeholders in `.env.example`.
- **No default values for secrets in YAML.** Never write `${JWT_SECRET:some-default}`. A missing secret must stop startup, not fall back to a value everyone can read in git.
- **Local secrets live in `.env`** (gitignored, `chmod 600`), loaded only by the `local` profile through `spring.config.import: optional:file:.env[.properties]`. Copy `.env.example` and generate values with `openssl rand -base64 48`.
- **Validate at startup.** Every new secret setting gets fail-fast validation (missing, unresolved `${...}`, too short) with a message naming the environment variable, plus a red-green test for it, as `JwtConfigTest` does.
- **Tokens:** store refresh tokens only as SHA-256 hashes, keep JWT claims to `sub`/`iat`/`exp` (no personal data), and never log tokens, secrets, or `Authorization` headers.
- **Before committing,** check `git diff --cached` for secrets. If a real secret is ever committed or pushed, treat it as leaked: rotate it, do not just delete the line.

## Commands

Use the wrapper (`gradlew.bat` on Windows, `./gradlew` in Git Bash):

- Run: `./gradlew bootRun`
- Build: `./gradlew build`
- All tests: `./gradlew test`
- Single test class: `./gradlew test --tests "com.blinkerleague.server.ServerApplicationTests"`
- Single method: `./gradlew test --tests "com.blinkerleague.server.ServerApplicationTests.contextLoads"`

No lint/format plugin is configured.

Local setup: copy `.env.example` to `.env` and fill `JWT_SECRET`, then start Redis with `docker compose up -d`. Tests that use Testcontainers (`RedisRefreshTokenStoreTest`, `AuthControllerTest`) need a running Docker engine but not the compose Redis.

## Development workflow: red-green TDD

`feat/common-setup` (the first feat) is done. Build every feat after it test-first, in red → green → refactor cycles:

1. **Red**: Pick one behavior, usually one of the "테스트:" cases listed for the feat in `plan.md`. Write a failing test for it, then run that test alone and confirm it fails for the expected reason (assertion failure, missing endpoint, wrong status). A compile error or a mistyped URL is not a valid red.
2. **Green**: Write the minimum production code that makes the test pass. Run that test again, then `./gradlew test` to make sure nothing else broke.
3. **Refactor**: Clean up the code and the test while keeping them green. Do not add behavior that has no test yet.
4. Repeat for the next behavior. Do not write production code before a failing test for it exists.

Keep the cycles small and separate:

- **One failing test per cycle.** Do not write a batch of red tests and then implement them all at once. Write one test, make it pass with the minimum code, refactor, then write the next one. Each line of production code should be traceable to the test that required it.
- **No new tests in the refactor step.** Refactoring changes structure only, and the existing tests are the safety net. New tests, including architecture rules such as ArchUnit, go into their own commit or the next red step.
- **Commit per step.** Commit red (`test: ...`) and green (`feat: ...`/`fix: ...`) separately, and refactor (`refactor: ...`) when there is one, so the history shows the TDD order. Red commits stay local. Push only once the branch is green again.
- A red step may add the minimum skeleton (empty classes, method stubs that throw `UnsupportedOperationException`, a new `ErrorCode`, a dependency) so that the test compiles and fails for the right reason.

Conventions:

- Test names are Korean sentences that describe the behavior, e.g. `인증없이_보호된_API를_호출하면_401_JSON을_반환한다`.
- Pure domain logic (e.g. `ScoreCalculator`) gets plain JUnit unit tests without Spring. API behavior gets `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` tests that assert status and `$.code` (see `SecurityConfigTest`, `GlobalExceptionHandlerTest`).
- Pin time by injecting a fixed `Clock` in tests and never depend on the real date.
- When reporting progress, show the red run (failing output) and the green run for each cycle. A feat PR is ready only when every `plan.md` test case for it exists and `./gradlew build` passes.

## Environment gotcha

IntelliJ IDEA 2021.3.1's Gradle sync (via its bundled Android plugin, which injects `AndroidStudioToolingPlugin`) fails with `NoClassDefFoundError: org/gradle/util/VersionNumber` on newer Gradle (seen on 9.8.2). It is unrelated to the Korean characters in the user path. Hence the pinned Gradle 7.6.4 / Boot 3.3.x. The IDE sync was not re-verified after the downgrade; the CLI wrapper build works.

Docker on this Mac is colima (`brew install colima docker docker-compose`, `colima start`), not Docker Desktop. `~/.testcontainers.properties` points `docker.host` at `~/.colima/default/docker.sock`. Boot 3.3 pins Testcontainers 1.19.x, which defaults to Docker API 1.32 and is rejected by Docker Engine 29+, so `build.gradle` sets the test system property `api.version=1.44` and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` for Ryuk. Both are safe for Docker Desktop and Linux.
