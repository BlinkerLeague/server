# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Backend for 깜빡이리그 (BlinkerLeague), a car app that scores turn-signal and other safe-driving habits. `plan.md` is the feature-by-feature roadmap (branches `feat/<name>`, P0 due 11.15). The server is written in **Java**, even though the original 기획서 says Kotlin.

Implemented so far: the common base, driver authentication (`auth`), driver registration (`driver`), thresholds (`threshold`), trip upload (`trip`), trip scoring (`score` + `trip`), the daily mission (`mission`), and the point ledger core (`point`). Update this file as features land:

- `common.error`: `ErrorCode` enum → `BusinessException` → `GlobalExceptionHandler` returns `{ code, message }`. Throw `BusinessException` for domain errors rather than building responses by hand.
- `common.jpa`: entities extend `BaseTimeEntity` (audited `createdAt`/`updatedAt`).
- `common.time.TimeConfig`: all day-based logic uses Asia/Seoul. Inject `Clock` instead of calling `now()` directly so tests can pin the time.
- `security.SecurityConfig`: stateless, no CSRF/form/basic login. Drivers authenticate with `Authorization: Bearer <access JWT>`, verified by Spring's OAuth2 resource server. Only `POST /api/drivers`, `POST /api/auth/refresh`, `GET /api/config/**`, `/h2-console/**`, and the API docs (`GET /v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`) are public. A missing token returns 401 `UNAUTHORIZED`, and an invalid, forged, or expired token returns 401 `INVALID_TOKEN`, both as `{ code, message }` JSON. Use `antMatcher(...)` for request matchers, because plain strings fail once the H2 console servlet is registered.
- `security.jwt`: `JwtProperties` (`jwt.secret`, `jwt.access-ttl` 1h, `jwt.refresh-ttl` 30d) and `JwtConfig` (HS256 `JwtEncoder`/`JwtDecoder` beans, expiry checked against the injected `Clock` with zero skew). Startup fails if the secret is missing, an unresolved `${...}` placeholder, or shorter than 32 bytes.
- `driver`: registration, in the hexagonal layout. `domain.Driver` is also the JPA entity (`driver` table, unique `nickname` up to 12 chars, 5-digit `region_code`, extends `BaseTimeEntity`). `application.service.RegisterDriverService` rejects a taken nickname with `CONFLICT`, saves, then issues tokens through `port.out.IssueDriverTokensPort`, implemented by `adapter.out.auth.AuthDriverTokensAdapter` on top of `auth`. `adapter.out.persistence.DriverPersistenceAdapter` uses `saveAndFlush` and turns only a unique-constraint violation into `CONFLICT` (a concurrent duplicate). `adapter.in.web.DriverController` serves `POST /api/drivers` (nickname 2–12 chars, region code `\d{5}`). There is no login: a driver who loses the tokens registers again (P0 decision, recovery is decided in P1).
- `auth`: driver tokens, in the hexagonal layout. `application.port.out` has `AccessTokenIssuer` and `RefreshTokenStore`. `application.service.RefreshTokenService` issues and rotates refresh tokens (stores SHA-256 hashes only, a reused token revokes the driver's current token). `application.service.TokenReissueService` returns a new access + refresh pair. Adapters: `adapter.out.jwt.JwtAccessTokenIssuer` (access JWT, `sub` = driverId), `adapter.out.redis.RedisRefreshTokenStore` (keys `auth:refresh:{hash}`, `auth:refresh:driver:{id}`, `auth:refresh:used:{hash}`, all with TTL, `GETDEL` on consume), `adapter.in.web.AuthController` (`POST /api/auth/refresh`), and `@CurrentDriver Long driverId` for controllers. `auth.config.AuthBeanConfig` wires ports to adapters. `driver` issues the first tokens at registration through `AuthDriverTokensAdapter`.
- `threshold`: judging thresholds, score weights, point rules, grades, and tips, in the hexagonal layout. `domain.Thresholds` is a pure record matching the `GET /api/config/thresholds` JSON (`gradeOf(score)` compares the highest `minScore` first and is empty below the lowest). `application.port.in.ThresholdProvider` is how other domains read the current `VersionedThresholds`; never hard-code these values (`plan.md` §6). `application.service.ThresholdService` implements it with an in-memory cache, `seedIfAbsent` (version 1, never overwrites a stored value), and `update` (version + 1, no admin API yet). `adapter.out.persistence.ThresholdPersistenceAdapter` stores the JSON in `app_config` (`config_key` = `thresholds`, `config_value`, `version`). `adapter.in.startup.DefaultThresholdsSeeder` seeds `src/main/resources/thresholds/default-thresholds.json` before the web server takes requests. `adapter.in.web.ThresholdController` serves the API.
- `trip`: trip and event upload, in the hexagonal layout. `domain.Trip` is the JPA entity (`trip` table, client UUID `trip_id`, `supported_signals` via `ScoreItemSetConverter`, `status`) and owns `TripEvent`s (`trip_event`, client UUID `event_id`, raw JSON `payload`). `Trip.upload(...)` validates `endedAt > startedAt`, that events fall inside the trip (inclusive), and that `eventId`s are unique within the trip (a duplicate would otherwise surface as a PK violation and a 500), and sets `EXCLUDED_SHORT` below `minDistanceKm` or `UPLOADED`. `Trip` implements `Persistable` so saving a client-assigned id INSERTs instead of merging; never switch it back to merge, or a concurrent duplicate silently overwrites the first trip. `application.service.UploadTripService` reads the minimum distance from `ThresholdProvider`, returns the stored result (`created=false`) for a retried `tripId` by the same driver, rejects another driver's `tripId` with `CONFLICT`, and on `TripAlreadyExistsException` (concurrent upload) reloads and applies the same rule. `adapter.out.persistence.TripPersistenceAdapter` maps only unique/PK violations to `TripAlreadyExistsException`. `adapter.in.web.TripController` serves `POST /api/trips` (201 new, 200 retry, Bean Validation on the request, including no `null` inside `supported`). Event-count/request-size limits (D16) and an allowed trip time window (D17) are still undecided in `plan.md` §0, so none are enforced yet.
- `score`: the pure scoring engine (`domain` only, no Spring or JPA). `ScoreCalculator` takes the supported items, `DrivingEvent`s, and weights and returns `ScoreResult(score, metrics)`. P0 measures only `TURN_SIGNAL` (share of `TURN`s with the signal on), and only if the car supports it. `WeightRedistribution` drops unmeasured items and rescales the rest to sum to 1. `MatchReport` builds `summary` and `plays` (D22). Nothing measured means `score = null`.
- `trip` scoring (`feat/trip-score`): `UploadTripService` scores every trip that is not `EXCLUDED_SHORT` during upload and calls `Trip.applyScore(score, metrics)` (`SCORED`, or `NO_TURN` when the score is null). Item scores are `TripMetric` rows in `trip_metric` (column `metric_value`, because `value` is reserved in H2). `TurnPayload` reads `signalOn` from the raw payload (only a boolean `true` counts, D23). `TripScoreView` is the shared `TripResult` for the upload response and for `GetTripScoreService` (`GET /api/trips/{tripId}/score`; a missing trip or another driver's trip is `NOT_FOUND`). `summary`, `plays`, and `turns` are computed from events on read, and the grade from the current thresholds. `ListRecentTripsService` serves `GET /api/trips` through `LoadRecentTripsPort` (D24). Because `open-in-view` is false, `TripPersistenceAdapter.findById` initializes `metrics` and `events` inside its transaction; keep that if the response reads another lazy collection.
- `mission`: the daily mission (`plan.md` 3.6), in the hexagonal layout. `domain.Mission` is the JPA entity (`mission` table, condition as JSON via `TurnSignalConditionConverter`), seeded from `missions/default-missions.json` by `adapter.in.startup.DefaultMissionSeeder` only when no mission exists. Judging is per round (one trip = one round): `TurnSignalCondition.judgeDay(rounds)` returns `ACHIEVED` once any round meets the condition, otherwise `IN_PROGRESS`. Trips count on the Asia/Seoul day of their `endedAt` (`DailyPeriod`), and `adapter.out.trip.TripDailyRoundsAdapter` reads `trip`/`trip_event` (excluding `EXCLUDED_SHORT`; `signalOn` counts only as a boolean `true`, same as D23). `GetTodayMissionService` + `MissionController` serve `GET /api/missions/today` (404 `NOT_FOUND` when no mission is active). `domain.DriverMission` is the `driver_mission` row (unique driver/mission/period_key; once `ACHIEVED` it never changes, and `achievedAt` keeps the first time). `application.port.in.RecordMissionAfterTripUseCase` is called by `trip` through `trip.application.port.out.TripMissionPort` (adapter `trip.adapter.out.mission.MissionTripAdapter`): `recordAfterTrip` judges and saves after `POST /api/trips` (new or retried), retrying once on `DriverMissionAlreadyExistsException`; `judgeDay` only computes, for `GET /api/trips/{tripId}/score`. Both fill `TripResult.mission` (`{ missionId, status }`, `null` when no mission is active). A past day that was not achieved is `FAILED` (D25): `recordAfterTrip`/`judgeDay` settle it, and `GetTodayMissionService` shows the latest earlier day (`DriverMissionStore.findLatestBefore`) when there is no round today. A stored row of a past day may still read `IN_PROGRESS` until it is judged again; treat it as `FAILED`.
- `point`: the point ledger (`plan.md` 4.2), in the hexagonal layout. `domain.PointEntry` is the JPA entity (`point_ledger` table, unique (`reason`, `ref_id`), `earned_at` as an `Instant`; the balance is `SUM(amount)`, never a stored column). `PointRules` holds the pure formulas (trip points `floor(min(distanceKm, point.tripMaxKm) × score / 100)`, daily cap), and `PointDay` is the Asia/Seoul day used for `todayEarned`. `application.service.EarnPointService.earnForTrip` returns the points actually earned: 0 for an already-earned (`reason`, `ref_id`), cut to what is left under `point.dailyCap` (read from `ThresholdProvider`). It runs inside `port.out.DriverPointLockPort`, implemented by `adapter.out.persistence.DriverPointLockAdapter` (transaction + `SELECT ... FOR UPDATE` on the `driver` row), so one driver's earnings are serialized; `EarnPointConcurrencyTest` checks the cap with 8 threads. `PointLedgerPersistenceAdapter` maps only unique violations to `PointAlreadyEarnedException`. `GetPointsService` + `adapter.in.web.PointController` serve `GET /api/points` (balance, todayEarned, dailyCap, latest 20 entries, `createdAt` at `+09:00`). Nothing calls `earnForTrip` yet: the `TRIP` hook waits for `feat/trip-score`, and `DAILY_MISSION` (20P) waits for `driver_mission` in #33.
- `common.openapi`: API docs with springdoc-openapi 2.6.0 (the 2.6.x line targets Boot 3.3; 2.7+ needs Boot 3.4). `OpenApiConfig` sets the title/description and makes Bearer JWT (`bearerAuth`) the default security. Docs are off by default (`springdoc.api-docs.enabled`/`swagger-ui.enabled: false` in `application.yaml`) and on only in `local`, so deployed environments never expose them (Pleos guide); `ApiDocsDisabledByDefaultTest` guards this. `override-with-generic-response: false`, so each operation lists only the responses its controller declares. `@CurrentDriver` parameters are hidden (`AuthWebConfig`). Every new controller must carry `@Tag` (Korean feature name), `@Operation(summary, description)`, and an `@ApiResponse` per status code, with error responses pointing at `ErrorResponse` as `application/json`; public endpoints add an empty `@SecurityRequirements`. `ApiDocsTest` checks these per endpoint, so add the new endpoint's rows there. After an API change, regenerate `docs/api/openapi.json` with `jq -S 'del(.servers)'` (README; `-S` keeps the key order stable between runs) and commit it with the change.
- Domain packages go under `com.blinkerleague.server.<domain>` and follow the hexagonal layout below. It replaced the original `controller/service/repository/domain/dto` layout, and `plan.md` §2 now records the same rule.

## Policy decisions (plan.md §0, D10–D15 decided 2026-11-03, D22–D24 decided 2026-11-05, D25 decided 2026-11-08)

These are product rules the code must follow. Do not change them without the user's decision, and keep `plan.md` §0 in sync.

- **Trip participation (D10):** a trip shorter than 3km is `EXCLUDED_SHORT`. Read the limit from `ThresholdProvider` (`trip.minDistanceKm`, default 3.0), never hard-code it. There is no minimum turn count: a trip with 0 turns still participates, only the turn-signal item is unmeasured (`NO_TURN`).
- **Trip status:** `feat/trip-upload` stores `EXCLUDED_SHORT` or `UPLOADED`; `feat/trip-score` turns `UPLOADED` into `SCORED` or `NO_TURN`.
- **League unit (D11):** keep storing the 5-digit 시군구 `region_code`, and group it into leagues through a `시군구 → 리그 권역` mapping table (start coarse, e.g. Seoul's 5 생활권; split as users grow). Never key leagues directly by `region_code`. A league with fewer than 10 participants does not publish rankings.
- **League membership (D12):** the driver's registered home region, fixed per season. A region change applies from the next season. No location data is used.
- **League ranking eligibility (D13):** at least 50km or 10 trips in the season. Anti-gaming lives here (plus distance-weighted averages), not in per-trip limits. Keep the values in the threshold config.
- **Lane changes (D14):** P0 scores turns only. `POST /api/trips` accepts and stores `LANE_CHANGE` events without scoring them; scoring waits for the car-app team to confirm detection is feasible (P1).
- **Upload request format (D15):** the proposed format in `plan.md` §3.4 (`tripId`, `startedAt`, `endedAt`, `distanceKm`, `supported`, `events[{eventId, type, occurredAt, payload}]`). The server stores `payload` as raw JSON. Adjust only the DTO if the 기획서 8.2 format turns out different.
- **Match summary and plays (D22, decided 2026-11-05):** a goal is a turn with the signal on, a conceded goal is a turn without it, and a foul is a harsh accel/brake or safety warning (always 0 in P0). `plays` lists every stored event in time order; only `TURN` has a `result` (`GOAL`/`CONCEDED`), the others carry `result: null`.
- **Unreadable `signalOn` (D23):** a `TURN` whose payload lacks a boolean `signalOn` counts as a turn without the signal. Upload does not reject it.
- **Recent trips `limit` (D24):** default 10, max 50; larger values are clamped to 50.
- **Unachieved mission days (D25, decided 2026-11-08):** a day that ended without reaching the mission is `FAILED`, decided lazily when judged (upload or read), with no batch (`MissionStatus.settle(dayEnded)`). Before the driver's first trip today, `GET /api/missions/today` keeps showing the final state (`ACHIEVED` or `FAILED`) of the latest earlier mission day; today's first trip starts today's mission. A late upload of a trip that ended on a `FAILED` day and meets the condition turns it into `ACHIEVED`. `ACHIEVED` never changes back.

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
- Redis stores only refresh tokens (`auth`). Spring Session was removed in `feat/jwt-auth`, so there are no server sessions. Redis connections are lazy, so the app can start without Redis, but registration (`POST /api/drivers`) and `POST /api/auth/refresh` fail until it is running.
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

## Pleos Connect app security guide (required by default)

The car app is distributed through the Pleos App Market and must pass its security review ([앱 보안 가이드](https://document.pleos.ai/docs/pleos-only/app-registration-guide/app-security-guide)). Most items target the Android app, but this server is the app's backend and must never be the reason the app fails review. Follow these rules in every feature without being asked; if a requirement conflicts with one, stop and ask the user.

- **Transport:** every deployed environment serves HTTPS only, with TLS 1.2 minimum and 1.3 preferred. SSL and TLS 1.0/1.1 are disabled, and TLS 1.2 uses vetted cipher suites only (ECDHE + AEAD; see the [OWASP TLS cheat sheet](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Security_Cheat_Sheet.html)). The certificate comes from a public CA with the full chain and a matching hostname, because the app must verify chain, expiry, and hostname and may not ignore certificate errors. Expose the API through a domain name, never a bare IP. Plain HTTP is allowed only in the `local` profile. When the deployed profile is built, record in `plan.md` where TLS terminates (load balancer or `server.ssl.*` with `enabled-protocols`) and add a check for the protocol versions.
- **Credentials in requests:** accept tokens only in the `Authorization` header, or in the JSON body for refresh as `POST /api/auth/refresh` does. Never design an endpoint that takes a token, session ID, password, or key in the URL path or query string; URLs end up in access logs and proxies.
- **Cryptography:** standard algorithms only. Use SHA-256 or stronger for hashing and integrity (no MD5 or SHA-1). If encryption is ever needed, use AES-GCM with a fresh nonce per message under the same key (never ECB), and RSA with OAEP-SHA-256. Generate tokens, keys, and nonces with `SecureRandom` (as `RefreshTokenService` does), never `Random`, `Math.random()`, or timestamps. Keys come from the environment or Vault, never from code, resources, or committed config.
- **Minimal sensitive data:** store and request only what a feature needs. Personal data today is the nickname and region code. Raw location coordinates are never accepted (already enforced by the trip DTO), and vehicle identifiers such as a VIN are not collected unless a feature needs one and the user decides so. Treat vehicle data that the app reads through the Pleos Vehicle SDK as sensitive.
- **Vehicle API scope:** the app may use only Pleos-approved vehicle APIs, within the scope of the feature, and its permissions must be pre-approved by the Pleos App Market. When designing a request field that needs vehicle data, ask for the smallest set the scoring needs and list the field in `plan.md` §7 so the car-app team can check the permission.
- **Logs:** never log tokens, secrets, `Authorization` headers, or request bodies containing personal or vehicle data. Error responses carry only `{ code, message }`, never stack traces or internal details. SQL logging (`show-sql`) stays in the `local` profile.
- **Server-to-app commands:** the server only returns data. It never sends commands that make the app control the vehicle. If such a feature is ever proposed, decide it with the user first (the guide requires the app to verify the origin and integrity of remote commands and to limit their scope).
- **CORS:** the API serves a native app, so no CORS is configured and browsers are refused cross-origin calls. If a web client is added, allow explicit origins only, never `*` with credentials.
- **Dependencies:** when adding or upgrading a dependency, check it for known vulnerabilities and handle high-severity ones first. Stay on the latest patch release of the pinned Spring Boot 3.3.x line. If a critical vulnerability has no fix within the pinned versions (see Stack), raise it to the user instead of upgrading silently.

## API specification: managed with Swagger

The API specification is managed with Swagger (OpenAPI, springdoc). The controller annotations are the source of truth, and there is no hand-written spec document. The mechanics are under `common.openapi` above and in the README section "API 명세 관리 규칙".

- **Spec lives in code.** Document request and response shapes, validation, status codes, and error codes with `@Tag`, `@Operation`, `@ApiResponse`, and Bean Validation on the controller and DTOs. Do not write or update a separate spec in Markdown, a doc, or a wiki.
- **A PR that adds or changes an API includes its spec.**
  - Update the controller annotations.
  - Add the endpoint's rows to `ApiDocsTest`. Write that test first, in the red step.
  - Regenerate `docs/api/openapi.json` and commit it in the same PR.
  - A PR that changes an API without these is not ready.
- **Share the spec through Swagger.**
  - Someone who can run the server checks out the repo, runs it with the `local` profile, and opens `/swagger-ui.html`.
  - Someone who cannot run it opens `docs/api/openapi.json` in Swagger Editor or Postman.
  - Point people there instead of writing a new spec document. Ask the user before creating one.
- **Other documents have other roles.**
  - `plan.md` records design and decisions.
  - The README API table is a summary.
  - `http/*.http` files are for manual local testing.
  - When a shape differs between them, Swagger is the reference. Fix the others to match, or ask the user if the code itself looks wrong.
- **Docs stay off outside `local`** (Pleos guide). Never enable `springdoc.*` in a deployed profile without the user's decision.

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

## Branches and pull requests

`main` holds released work and `develop` is the integration branch. GitHub's default branch is `main`, so `gh pr create` targets `main` unless told otherwise; always pass the base explicitly.

- **Work branches start from `develop`.** Name them `feat/<name>` (as in `plan.md`), `fix/`, `docs/`, `refactor/`, or `test/`. Pull `develop` first so the branch starts from its latest commit.
- **Work PRs target `develop`:** `gh pr create --base develop`. Never open or merge a work branch into `main`. Before merging any PR, check its base branch (`gh pr view <n> --json baseRefName`).
- **Only `develop → main` reaches `main`,** as a release PR that lists the work PRs it brings in. Open it only when the user asks.
- **Merge with a merge commit** (`gh pr merge --merge`), as every PR so far has, so the TDD commit order stays visible. No squash or rebase merges.
- **After merging,** delete the work branch on the remote and locally once it is contained in `develop`. Never delete `main` or `develop`.
- **If a PR was merged into the wrong base,** do not merge the same commits again after a revert: git treats them as already merged, and the revert wins. Revert only that PR's changes on the wrong branch, then cherry-pick the commits onto `develop` in a new branch and open a new PR (this is how #23–#26 fixed #21 and #22 going straight to `main`).
- PR bodies follow the earlier ones: changes, TDD cycles, how it was checked (`./gradlew build` and test count), and points for review. Claim a red failure reason only if it was actually observed.

## Environment gotcha

IntelliJ IDEA 2021.3.1's Gradle sync (via its bundled Android plugin, which injects `AndroidStudioToolingPlugin`) fails with `NoClassDefFoundError: org/gradle/util/VersionNumber` on newer Gradle (seen on 9.8.2). It is unrelated to the Korean characters in the user path. Hence the pinned Gradle 7.6.4 / Boot 3.3.x. The IDE sync was not re-verified after the downgrade; the CLI wrapper build works.

Docker on this Mac is Docker Desktop (Docker Engine 29.x, context `desktop-linux`). colima is installed but stopped and not used; do not start it alongside Docker Desktop. The compose Redis (`blinkerleague-redis`) runs on Docker Desktop. `~/.testcontainers.properties` points `docker.host` at Docker Desktop's user socket `~/.docker/run/docker.sock`, because `/var/run/docker.sock` does not exist on this Mac. Boot 3.3 pins Testcontainers 1.19.x, which defaults to Docker API 1.32 and is rejected by Docker Engine 29+, so `build.gradle` sets the test system property `api.version=1.44` and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` for Ryuk (the socket path inside the Docker VM, not on the Mac). Both settings also work with colima and Linux.
