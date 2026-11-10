# 배포 · 운영 DB 설계

작성: 2026-11-10 · 대상 브랜치: `feat/deploy-design`

서버를 실제로 띄우기 전에 정할 것(`plan.md` 9.4)을 한곳에 모은 설계 문서입니다. 정책 결정은 `plan.md` §0이 기준이고, 이 문서의 **제안**은 사용자가 결정하기 전까지 확정이 아닙니다. 결정되면 `plan.md` §0(D번호) · `docs/policies.md` · CLAUDE.md에 옮깁니다.

## 1. 지금 상태

| 항목 | 지금 | 근거 |
| --- | --- | --- |
| 운영 DB | **결정됨: PostgreSQL + Flyway**(D3). 코드는 아직 H2 인메모리(`MODE=MySQL`) + `ddl-auto: create`뿐 | `plan.md` D3, `application-local.yaml` |
| 프로필 | `local`(기본) · `test`만 있음. 배포용 프로필 없음 | `src/main/resources` |
| Redis | 리프레시 토큰만 저장. docker-compose에 AOF 영구 저장으로 띄움 | `docker-compose.yml`, 8장 |
| HTTPS | 규칙만 있음(TLS 1.2+, 공인 인증서, 도메인). 종단 위치 미정 | CLAUDE.md Pleos 보안 가이드 |
| secret | `JWT_SECRET` 하나, 환경변수로 받고 없으면 기동 실패. Vault는 추후 | `JwtConfig` |
| 배치 | 주간 리포트(월 04:00) · 시즌 정산(매월 1일 00:10). 서버 1대 전제 | `WeeklyReportScheduler`, `LeagueSeasonScheduler` |
| 배포 대상 | **정해지지 않음** | — |

## 2. 결정이 필요한 것

| # | 항목 | 선택지 | 제안 |
| --- | --- | --- | --- |
| D36 | 배포 위치 | A) 클라우드 VM 1대 + docker compose(앱 · PostgreSQL · Redis · 리버스 프록시) / B) 매니지드(예: AWS 서울 리전 ECS + RDS + ElastiCache + ALB, 또는 NCP 같은 구성) | **A로 시작**(12.2 PT까지 비용 · 작업량이 가장 작음). 사용자가 늘거나 가용성이 필요해지면 DB부터 매니지드(B)로 옮긴다. 앱은 이미지 하나라 옮기기 쉽다 |
| D37 | 테스트 · `local` DB | A) H2 유지 + PostgreSQL 스키마 검증 테스트 하나 / B) 모든 테스트를 Testcontainers PostgreSQL로 | **A.** 빠른 테스트는 H2로 두고, Flyway 마이그레이션 + `ddl-auto: validate`가 PostgreSQL에서 통과하는지 확인하는 테스트를 Testcontainers로 하나 둔다. H2 모드는 `MODE=PostgreSQL`로 바꾼다 |
| D38 | 운행 이벤트 보관 기간 | A) 무기한 / B) 원본 `trip_event`는 N개월 뒤 삭제(점수 · 지표는 `trip` · `trip_metric`에 남음) | **A로 시작하고 용량을 본다**(4.3 추정상 1년은 문제 없음). 단, `plays` · 배지 진행도가 `trip_event`를 읽으므로 B를 고르면 그 기능을 먼저 바꿔야 한다 |
| D39 | 기준값 운영 변경 경로 | A) 관리자 API(별도 인증) / B) 운영 절차(SQL로 `app_config` 갱신 + 버전 +1 + 재시작) | **P1 동안은 B**, 관리자 인증 방식을 정할 때 A |
| D40 | 백업 | A) 매니지드 자동 백업 + 시점 복구 / B) `pg_dump` 일 1회 + 외부 저장소 | D36이 A면 **B(보관 14일)**, B면 A. 어느 쪽이든 분기 1회 복원 연습 |

## 3. 목표 구조

```
[차량 앱] --HTTPS(TLS 1.2/1.3)--> [api.<도메인>]
                                     │
                          리버스 프록시 / 로드밸런서   ← TLS 종단, 공인 인증서(전체 체인)
                                     │ HTTP(내부망)
                              [Spring Boot 앱 1대]   ← prod 프로필, 스케줄러 포함
                               │               │
                     [PostgreSQL 16]        [Redis 7, AOF]
                     (운행 · 점수 · 포인트…)  (리프레시 토큰만)
```

- **앱은 1대**로 시작한다. 두 배치가 서버 1대를 전제하므로, 2대 이상이 되면 ShedLock(PostgreSQL 잠금 테이블)을 먼저 넣는다.
- **TLS 종단은 프록시**에서 한다. D36 A라면 Caddy 또는 nginx + Let's Encrypt(공인 CA, 전체 체인), B라면 ALB + ACM. 프록시 설정은 TLS 1.2 · 1.3만 허용, TLS 1.2는 ECDHE + AEAD 스위트만. 앱은 `server.forward-headers-strategy: framework`로 원래 스킴을 안다.
- **앱 포트는 외부에 열지 않는다.** PostgreSQL · Redis도 내부망만. bare IP로 API를 노출하지 않는다.
- 확인: 배포 후 `testssl.sh api.<도메인>`(또는 `nmap --script ssl-enum-ciphers -p 443`)로 SSL · TLS 1.0/1.1이 꺼져 있는지 보고 결과를 `plan.md`에 남긴다.

## 4. 운영 DB 적재 설계

### 4.1 스키마 관리

- **Flyway가 스키마의 유일한 출처**다. `src/main/resources/db/migration/V1__init.sql`에 지금 엔티티의 스키마를 한 번에 만든다(Hibernate로 PostgreSQL DDL을 뽑은 뒤 사람이 읽고 다듬어 커밋).
- 배포 프로필은 `spring.jpa.hibernate.ddl-auto: validate`. 엔티티와 스키마가 다르면 기동이 실패한다.
- 이후 스키마 변경은 `V2__...sql`처럼 새 파일로만 한다. **이미 적용된 마이그레이션 파일은 고치지 않는다**(체크섬 불일치로 기동 실패).
- 컬럼 삭제 · 이름 변경은 두 번에 나눈다(새 컬럼 추가 · 코드 전환 배포 → 다음 배포에서 옛 컬럼 삭제). 앱 1대라도 롤백할 수 있게 한다.
- `local`은 H2 + `ddl-auto: create`를 그대로 두되(D37 A), 스키마 검증 테스트가 Flyway와 엔티티의 어긋남을 잡는다.

### 4.2 타입 매핑 (H2 → PostgreSQL에서 바뀌는 것)

| 대상 | 지금 | PostgreSQL에서 | 할 일 |
| --- | --- | --- | --- |
| `Instant`(`trip.started_at` · `ended_at`, `point_ledger.earned_at` 등) | timestamp | `timestamptz` | 그대로. 일 · 주 · 시즌 경계는 이미 코드가 Asia/Seoul로 계산해 `Instant` 범위로 질의한다 |
| `BaseTimeEntity`의 `LocalDateTime` | timestamp | `timestamp`(오프셋 없음) | `hibernate.jdbc.time_zone: Asia/Seoul` 유지. 응답에 내보낼 때 `Instant`로 바꾸는 것은 9.5 그대로 |
| `@Lob String`(`trip_event.payload`, `app_config.config_value`, `weekly_report`) | CLOB | Hibernate 6 기본 매핑은 대용량 객체(`oid`)라 `text`가 아니다 | **`@Lob` 대신 `@JdbcTypeCode(SqlTypes.LONG32VARCHAR)`(→ `text`)로 바꾼다.** 검증 테스트로 확인. `jsonb`는 서버가 JSON 안을 SQL로 질의할 때 다시 본다 |
| 클라이언트 UUID(`trip_id`, `event_id`) | `varchar(36)` | `varchar(36)` | 그대로(앱이 문자열로 보냄). `uuid` 타입 전환은 이득이 작아 하지 않는다 |
| `driver.league_visible` 기본값 | `@ColumnDefault` | `boolean default true` | 마이그레이션에 기본값 명시 |
| 예약어 | `metric_value` 등 이미 회피 | — | 새 컬럼도 `value` · `user` 등 예약어를 피한다 |

### 4.3 인덱스 (조회 경로 기준)

유니크 제약은 엔티티에 이미 있다(`point_ledger(reason, ref_id)`, `driver_mission(driver, mission, period_key)`, `league_member(season, driver)` 등). 그 외에 JDBC 집계(`TripDailyRoundsAdapter` · `WeekDrivingAdapter` · `LeagueRecordsAdapter` · `BadgeProgressAdapter`)와 목록 조회가 쓰는 인덱스를 `V1`에 넣는다.

| 인덱스 | 쓰는 곳 |
| --- | --- |
| `trip(driver_id, ended_at)` | 오늘의 미션 라운드, 주간 리포트, 리그 기록, 최근 경기 목록 |
| `trip(ended_at)` | 주간 리포트 배치의 대상 운전자(`driversWithTrips`) |
| `trip_event(trip_id, type)` | 미션 · 배지의 `TURN` 집계, 점수 조회의 `plays` |
| `trip_metric(trip_id)` | 유니크 `(trip_id, metric)`이 이미 앞쪽 컬럼으로 덮음 → 추가 없음 |
| `point_ledger(driver_id, earned_at)` | 잔액 · 오늘 적립 · 시즌 포인트 합 · 최근 20건 |
| `driver_mission(driver_id, status)` | 연속 기록 · 배지(달성한 날 목록) |
| `league_champion(driver_id)` | 배지 진행도 · `me.championships` |

`league_member`의 유니크 제약은 `(season_id, driver_id)` 순서로 두어 시즌별 조회에도 쓰이게 한다.

### 4.4 데이터 양 추정

가정(앱 팀 · 사용자 수가 정해지면 다시 계산): 운전자 1,000명, 하루 2회 운행, 운행당 이벤트 30개, 이벤트 payload 평균 300B.

| 테이블 | 하루 | 1년 |
| --- | --- | --- |
| `trip` | 2,000행 | 약 73만 행, 수백 MB 미만 |
| `trip_event` | 6만 행 · 약 20MB(인덱스 포함) | 약 2,200만 행 · 약 7GB |
| `trip_metric` | 최대 16,000행(항목 8개) | 약 580만 행 |
| `point_ledger` | 수천 행 | 수백만 행 |

`trip_event`가 대부분을 차지한다. 이 규모는 PostgreSQL 1대(2 vCPU · 4GB · 디스크 50GB)로 충분하다. 사용자가 10배가 되면 9.5의 "집계 비용"(조회마다 전체 기록 집계)이 먼저 문제가 되므로, 그때 누적 카운터 · 캐시와 `trip_event` 보관 기간(D38)을 함께 본다.

### 4.5 쓰기 경로와 일관성

- 운행 업로드는 지금처럼 단계별 트랜잭션(저장 → 적립 → 미션 → 배지 → 리그)이고, 중간 실패는 같은 `tripId` 재전송으로 복구한다(9.5). PostgreSQL에서도 그대로 동작한다: 중복은 유니크 · PK 위반으로 감지하고, 적립은 `driver` 행 `SELECT ... FOR UPDATE`로 직렬화한다.
- PostgreSQL에서는 **실패한 문장 뒤 트랜잭션 전체가 중단**된다. 유니크 위반을 잡아 같은 트랜잭션 안에서 계속 쓰는 코드가 없는지(지금 어댑터는 `saveAndFlush` 실패를 예외로 바꿔 트랜잭션 밖에서 다시 읽는다) 스키마 검증 테스트와 함께 동시성 테스트(`EarnPointConcurrencyTest` 등)를 PostgreSQL로 한 번 돌려 확인한다.
- 커넥션 풀(Hikari)은 기본 10. 앱 1대 · DB 1대 기준으로 충분하고, DB `max_connections`(기본 100) 안에 둔다.

### 4.6 초기 데이터 적재

- **카탈로그 데이터는 지금처럼 앱 시드가 넣는다**(Flyway 데이터 마이그레이션으로 옮기지 않는다): 기준값(`seedIfAbsent`, 저장된 값은 덮어쓰지 않음), 미션 · 배지(비어 있을 때만). 첫 배포에서 빈 DB에 자동으로 들어간다.
- 그래서 **시드 JSON을 고쳐도 운영 DB에는 반영되지 않는다.** 기준값 변경은 D39 경로로, 미션 · 배지 추가는 새 Flyway 마이그레이션(`INSERT`)으로 한다.
- 개발 · 테스트 데이터(H2)를 운영으로 옮기지 않는다. 운영 DB는 빈 상태로 시작한다(P0 소급 적립은 뒤로 미룸).
- 지역 리그 매핑(`leagues/league-regions.json`)과 주간 미션 템플릿은 DB가 아니라 리소스 파일이라 배포로 바뀐다.

### 4.7 백업 · 복구

- PostgreSQL: D40. 복구 목표는 하루치 손실 이내(RPO 24h), 복구 1시간 이내(RTO 1h)로 시작.
- **Redis도 사실상 필수 데이터다.** 로그인이 없어서(토큰 분실 복구 미정) 리프레시 토큰을 잃으면 Access 만료(1시간) 뒤 운전자가 재등록해야 하고 기록이 끊긴다. AOF(`appendfsync everysec`) + 볼륨 유지, 매니지드라면 영속성 옵션을 켠다. Redis 백업 파일도 PostgreSQL 백업과 함께 보관한다.

## 5. 배포 프로필 (`prod`)

`application-prod.yaml`(제안). 값은 모두 환경변수로 받고 기본값을 두지 않는다(CLAUDE.md 보안 규칙).

```yaml
spring:
  datasource:
    url: ${DB_URL}            # jdbc:postgresql://db:5432/blinkerleague
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  data:
    redis:
      host: ${REDIS_HOST}
      password: ${REDIS_PASSWORD}
server:
  forward-headers-strategy: framework
# springdoc은 application.yaml 기본값(false) 그대로. show-sql 없음.
```

- 새 secret(`DB_PASSWORD`, `REDIS_PASSWORD`)은 `JwtConfigTest`처럼 기동 시 검증(없음 · `${...}` 미해결)과 red-green 테스트를 둔다.
- 앱 DB 계정은 소유자 계정과 나눈다: 마이그레이션은 스키마 소유 계정, 앱은 DML 권한만 있는 계정(Flyway를 앱 기동 시 돌리면 같은 계정을 쓰게 되므로, 처음에는 하나로 시작하고 매니지드로 옮길 때 나눈다).
- Vault 전환은 `spring.config.import: vault://`로 나중에 한다.

## 6. 빌드 · 배포 흐름

1. `develop → main` 릴리스 PR 머지.
2. GitHub Actions: `./gradlew build`(Testcontainers 포함) → `bootBuildImage` 또는 Dockerfile로 이미지 → 레지스트리(GHCR 등)에 커밋 SHA 태그로 푸시.
3. 서버에서 이미지 교체(D36 A: `docker compose pull && docker compose up -d app`). 기동 시 Flyway가 마이그레이션을 먼저 적용한다.
4. 헬스 체크(Actuator `health`만 내부 포트로 노출, 추가 필요) 통과 확인. 실패하면 이전 태그로 되돌린다. 되돌릴 때 이미 적용된 마이그레이션은 남으므로 4.1의 "두 번에 나눠 바꾸기"를 지킨다.

## 7. 작업 순서 (feat 분할 제안)

| 순서 | 브랜치 | 내용 | 결정 의존 |
| --- | --- | --- | --- |
| 1 | `feat/postgres-flyway` | PostgreSQL 드라이버 · Flyway 의존성, `V1__init.sql`(4.2 타입 · 4.3 인덱스), `@Lob` → `text`, H2 `MODE=PostgreSQL`, Testcontainers 스키마 검증 테스트 | D37 |
| 2 | `feat/prod-profile` | `application-prod.yaml`, DB · Redis secret 기동 검증, `forward-headers-strategy`, Actuator 헬스 | — |
| 3 | `feat/deploy-infra` | Dockerfile/이미지, 운영 compose 또는 매니지드 설정, 리버스 프록시 TLS 설정, GitHub Actions | D36 |
| 4 | `feat/ops-runbook` | 백업 · 복구 절차, 기준값 변경 절차, TLS 점검 결과를 `plan.md`에 기록 | D38 · D39 · D40 |

각 feat는 lead → tester(red) → developer(green) 흐름을 따른다(CLAUDE.md).
