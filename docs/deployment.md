# 배포 · 운영 DB 설계

작성: 2026-11-10 · 대상 브랜치: `feat/deploy-design`

서버를 실제로 띄우기 전에 정할 것(`plan.md` 9.4)을 한곳에 모은 설계 문서입니다. 정책 결정은 `plan.md` §0이 기준이고, 이 문서의 **제안**은 사용자가 결정하기 전까지 확정이 아닙니다. 결정되면 `plan.md` §0(D번호) · `docs/policies.md` · CLAUDE.md에 옮깁니다.

## 1. 지금 상태

| 항목 | 지금 | 근거 |
| --- | --- | --- |
| 운영 DB | **결정됨: PostgreSQL + Flyway**(D3). `local`은 Docker PostgreSQL + Flyway `V1` + `validate`(`feat/postgres-flyway`), 테스트는 H2 | `plan.md` D3, `application-local.yaml`, `db/migration` |
| 프로필 | `local`(기본, PostgreSQL) · `test`(H2)만 있음. 배포용 프로필 없음 | `src/main/resources` |
| Redis | 리프레시 토큰만 저장. docker-compose에 AOF 영구 저장으로 띄움 | `docker-compose.yml`, 8장 |
| HTTPS | 규칙만 있음(TLS 1.2+, 공인 인증서, 도메인). 종단 위치 미정 | CLAUDE.md Pleos 보안 가이드 |
| secret | `JWT_SECRET` 하나, 환경변수로 받고 없으면 기동 실패. Vault는 추후 | `JwtConfig` |
| 배치 | 주간 리포트(월 04:00) · 시즌 정산(매월 1일 00:10). 서버 1대 전제 | `WeeklyReportScheduler`, `LeagueSeasonScheduler` |
| 배포 대상 | **정해지지 않음** | — |

## 2. 결정이 필요한 것

| # | 항목 | 선택지 | 제안 |
| --- | --- | --- | --- |
| D36 | 배포 위치 | A) 클라우드 VM 1대 + docker compose(앱 · PostgreSQL · Redis · 리버스 프록시) / B) 매니지드(예: AWS 서울 리전 ECS + RDS + ElastiCache + ALB, 또는 NCP 같은 구성) | **A로 시작**(12.2 PT까지 비용 · 작업량이 가장 작음). 공모전용 무료 후보는 8장: **Oracle Always Free VM + Cloudflare Tunnel** 권장. 사용자가 늘거나 가용성이 필요해지면 DB부터 매니지드(B)로 옮긴다. 앱은 이미지 하나라 옮기기 쉽다 |
| D37 | 테스트 · `local` DB | A) H2 유지 + PostgreSQL 스키마 검증 테스트 하나 / B) 모든 테스트를 Testcontainers PostgreSQL로 | **결정(11.10, 사용자): `local`도 Docker PostgreSQL(9장), 테스트는 A.** 빠른 테스트는 H2로 두고, Flyway 마이그레이션 + `ddl-auto: validate`가 PostgreSQL에서 통과하는지 확인하는 테스트를 Testcontainers로 하나 둔다 |
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
- **TLS 종단은 프록시**에서 한다. 8장 추천 조합이면 Cloudflare 엣지(Tunnel)에서 끝나고 최소 TLS 1.2로 설정한다. 직접 프록시를 둔다면 D36 A는 Caddy 또는 nginx + Let's Encrypt(공인 CA, 전체 체인), B라면 ALB + ACM. 프록시 설정은 TLS 1.2 · 1.3만 허용, TLS 1.2는 ECDHE + AEAD 스위트만. 앱은 `server.forward-headers-strategy: framework`로 원래 스킴을 안다.
- **앱 포트는 외부에 열지 않는다.** PostgreSQL · Redis도 내부망만. bare IP로 API를 노출하지 않는다.
- 확인: 배포 후 `testssl.sh api.<도메인>`(또는 `nmap --script ssl-enum-ciphers -p 443`)로 SSL · TLS 1.0/1.1이 꺼져 있는지 보고 결과를 `plan.md`에 남긴다.

## 4. 운영 DB 적재 설계

### 4.1 스키마 관리

- **Flyway가 스키마의 유일한 출처**다. `src/main/resources/db/migration/V1__init.sql`에 지금 엔티티의 스키마를 한 번에 만든다(Hibernate로 PostgreSQL DDL을 뽑은 뒤 사람이 읽고 다듬어 커밋).
- 배포 프로필은 `spring.jpa.hibernate.ddl-auto: validate`. 엔티티와 스키마가 다르면 기동이 실패한다.
- 이후 스키마 변경은 `V2__...sql`처럼 새 파일로만 한다. **이미 적용된 마이그레이션 파일은 고치지 않는다**(체크섬 불일치로 기동 실패).
- 컬럼 삭제 · 이름 변경은 두 번에 나눈다(새 컬럼 추가 · 코드 전환 배포 → 다음 배포에서 옛 컬럼 삭제). 앱 1대라도 롤백할 수 있게 한다.
- `local`도 Docker PostgreSQL + Flyway + `validate`로 운영과 같게 돌린다(9장). 테스트는 H2 + `ddl-auto: create`를 두고(D37), 스키마 검증 테스트가 Flyway와 엔티티의 어긋남을 잡는다.

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
| 1 | `feat/postgres-flyway` | **완료(11.10, 9.7).** PostgreSQL 드라이버 · Flyway 의존성, `V1__init.sql`(4.2 타입 · 4.3 인덱스), `@Lob` → `text`, Testcontainers 스키마 검증 테스트, `local` Docker PostgreSQL, `DB_PASSWORD` 기동 검증 | D37 |
| 2 | `feat/prod-profile` | `application-prod.yaml`, Redis secret 기동 검증(DB는 1번에서 완료), `forward-headers-strategy`, Actuator 헬스 | — |
| 3 | `feat/deploy-infra` | Dockerfile/이미지, 운영 compose 또는 매니지드 설정, 리버스 프록시 TLS 설정, GitHub Actions | D36 |
| 4 | `feat/ops-runbook` | 백업 · 복구 절차, 기준값 변경 절차, TLS 점검 결과를 `plan.md`에 기록 | D38 · D39 · D40 |

각 feat는 lead → tester(red) → developer(green) 흐름을 따른다(CLAUDE.md).

## 8. 무료 배포 후보 (공모전용, 2026-11-10 조사)

공모전 기간에 비용 없이 서버를 띄울 곳을 조사한 결과입니다(D36). 무료 범위는 대부분 제3자 자료에서 가져왔고 2026년에 바뀐 것이 많으므로, 가입 전에 각 공식 페이지에서 다시 확인합니다.

### 8.1 이 서버가 요구하는 조건

| 조건 | 이유 |
| --- | --- |
| 항상 켜져 있음 | 서버 안 배치(주간 리포트 월 04:00, 시즌 정산 매월 1일 00:10). 잠드는 서버면 배치가 돌지 않음 |
| 메모리 1GB 이상(여유 있게 2GB+) | Spring Boot + PostgreSQL + Redis |
| HTTPS · 공인 인증서 · 도메인 | Pleos 앱 보안 가이드. bare IP 노출 금지 |
| PostgreSQL · Redis | D3, 리프레시 토큰(8장) |

### 8.2 서버(컴퓨팅) 후보

| 후보 | 무료 범위 | 장점 | 주의점 | 평가 |
| --- | --- | --- | --- | --- |
| **Oracle Cloud Always Free** | ARM VM 합계 2 OCPU · 12GB(2026년 4 OCPU · 24GB에서 축소), 기간 제한 없음 | 사양 충분. 3장 구성(VM 1대 + docker compose)이 코드 변경 없이 그대로 올라감 | 지역에 따라 자리가 없어 생성이 자주 실패. 30일 이상 유휴 계정은 회수될 수 있음. 가입 시 카드 필요. 홈 리전(춘천 · 서울)에서만 무료 | **1순위** |
| **Google Compute Engine e2-micro** | VM 1대 · 1GB · 디스크 30GB, 기간 제한 없음 | 무료가 유지됨 | 미국 리전(us-west1 · us-central1 · us-east1)만 무료라 한국에서 지연. 1GB에 세 가지를 올리면 빠듯해 JVM 메모리를 줄여야 함(`-Xmx384m` 등) | 2순위(Oracle 실패 시) |
| **Google Cloud Run** | 월 요청 200만 건 · vCPU 18만 초 · 메모리 36만 GiB초(미국 리전 기준) | 서버 관리 없음, HTTPS 자동 | 요청이 없으면 꺼져 **서버 안 배치가 돌지 않음** → 배치를 호출하는 엔드포인트 + Cloud Scheduler(무료 3개)로 바꾸는 코드 작업 필요. 첫 요청 때 JVM 기동 지연 | 비권장(코드 변경) |
| **네이버 클라우드** | 신규 가입 크레딧 10만 원(3개월). 제휴 학교 재학생은 그린루키 20만 원(1년). Micro Server 1년 무료는 블로그 출처(미확인) | 서울 리전, 안정적. 공모전 기간을 덮음 | 크레딧이 끝나면 과금. 공모전 후 반드시 반납 | 대안(기간 한정) |
| **AWS 무료 플랜**(2025-07-15 이후 가입) | 크레딧 $100 + 활동(RDS 생성 등) 시 최대 $100. **6개월 또는 크레딧 소진 중 먼저 오는 때까지.** EC2 `t4g.small`(2 vCPU · 2GB) · `t3.small` 등이 무료 플랜 대상이고 사용료는 크레딧에서 차감 | 서울 리전. 크레딧이 떨어지면 과금하지 않고 계정을 멈춤. `t4g.small` 1대에 앱 · PostgreSQL · Redis를 compose로 올리면 공모전 기간은 크레딧으로 충분 | 6개월 뒤 계정이 닫히고 90일 안에 유료로 바꾸지 않으면 리소스 삭제. 공인 IPv4 주소도 시간당 과금되어 크레딧을 씀. 크레딧 만료 시점이 출처마다 다름(콘솔 Billing에서 확인) | 대안(기간 한정, 공모전에는 충분) |
| AWS Lightsail | 신규 계정 3개월 무료 체험(512MB급 요금제) | 고정 요금 VPS라 설정이 간단 | 512MB는 이 서버에 부족. 무료 범위 설명이 출처마다 다름 | 제외 |
| **Render 무료 웹 서비스** | 웹 서비스 무료, 월 750 인스턴스 시간 | `*.onrender.com`에 HTTPS 자동 | 15분 요청이 없으면 잠들고 깨는 데 약 1분 → 배치 안 돎, 앱 업로드 타임아웃 위험. 무료 PostgreSQL은 30일 뒤 만료 | 비권장 |
| **Koyeb** | 출처마다 다름(무료 인스턴스 512MB · 카드 필요 여부 상충) | — | 2026년 Mistral AI 인수, 무료 플랜 조건 불확실 | 제외 |
| **Cloudflare Workers / Containers** | Workers 무료는 JS · Wasm만. Containers는 **월 $5 유료 플랜 필요** | — | Java 서버를 무료로 돌릴 수 없음 | 서버용으로는 제외 |

### 8.3 DB · Redis 후보 (VM에 같이 띄우지 않을 때)

| 후보 | 무료 범위 | 주의점 |
| --- | --- | --- |
| VM 안 docker compose(PostgreSQL · Redis) | VM 무료 범위 안 | 백업을 직접 해야 함(D40 B) |
| Google Cloud SQL · Memorystore | **무료 없음** | — |
| AWS RDS(`db.t4g.micro`) · Aurora PostgreSQL 서버리스 | AWS 무료 플랜 크레딧 안에서 사용(Aurora PostgreSQL은 2026-03 무료 플랜 대상에 추가) | 크레딧 차감 · 6개월 제한은 무료 플랜과 같음. ElastiCache(Redis)는 크레딧을 빨리 쓰므로 Redis는 EC2 안 compose 권장 |
| Neon (PostgreSQL) | 프로젝트당 저장 0.5GB · 월 100 CU-시간, 5분 유휴 시 꺼짐 | 첫 쿼리 지연. 4.4 추정상 0.5GB는 몇 주 분량 |
| Upstash (Redis) | 256MB · 월 명령 50만 건 · DB 1개 | TLS 접속(`rediss://`) 설정 필요. 토큰 재발급만 쓰므로 명령 수는 여유 |
| Render 무료 PostgreSQL | 1GB | 30일 뒤 만료 |

### 8.4 HTTPS · 도메인 (앞단)

| 후보 | 무료 범위 | 주의점 |
| --- | --- | --- |
| **Cloudflare Tunnel + 무료 DNS** | 터널 · DNS · 인증서 무료. 최소 TLS 버전 1.2 지정 가능 | **도메인 필요, DNS를 Cloudflare로 옮겨야 함**(DuckDNS 등 무료 서브도메인 불가, .com 1년 1~2만 원). 서버 포트를 열지 않아도 됨 |
| Cloudflare Quick Tunnel(`trycloudflare.com`) | 무료, 도메인 불필요 | 재시작마다 주소가 바뀌어 앱에 넣을 수 없음(운영 부적합) |
| Caddy · nginx + Let's Encrypt + DuckDNS | 모두 무료 | 443 포트를 열고 TLS 설정을 직접 관리. 무료 서브도메인이 앱 심사에 문제없는지 확인 필요 |

### 8.5 추천 조합

| 순위 | 조합 | 비용 | 코드 변경 |
| --- | --- | --- | --- |
| 1 | Oracle Always Free VM(앱 · PostgreSQL · Redis compose) + Cloudflare Tunnel | 도메인값만 | 없음 |
| 2 | GCE e2-micro + Cloudflare Tunnel(메모리 부족 시 DB · Redis를 Neon · Upstash로) | 도메인값만 | 없음(JVM 옵션만) |
| 3 | 네이버 클라우드 크레딧 VM 또는 AWS 무료 플랜 EC2 `t4g.small` + Cloudflare Tunnel | 기간 내 0원(최대 3 ~ 6개월) | 없음 |
| — | Cloud Run + Neon + Upstash | 0원 | 배치 호출 방식으로 변경 필요 |

1 · 2 · 3 모두 TLS는 Cloudflare 엣지에서 끝나므로(3장 "TLS 종단"), 결정되면 3장과 `plan.md` 9.4에 종단 위치를 기록합니다.

### 8.6 출처

- Oracle: [InfoQ](https://infoq.com/news/2026/07/oracle-cloud-free-tier-limits/), [braindetox](https://braindetox.kr/en/posts/oracle_always_free_tier_reduced_2026.html), [space-node](https://space-node.net/blog/oracle-cloud-always-free-limits-2026)
- Google: [Cloud Run 가격](https://goo.gle/3Inc1KA), [Cloud Scheduler 가격](https://cloud.google.com/scheduler/pricing), [GCP 무료 가이드(agentdeals)](https://agentdeals.dev/gcp-free-tier-2026), [cloudchipr](https://cloudchipr.com/blog/cloud-run-pricing)
- 네이버 클라우드: [크레딧 이벤트](https://www.ncloud.com/main/creditEvent?tab=1), [학생 크레딧](https://www.ncloud.com/main/creditEvent?tab=2)
- AWS: [무료 컴퓨팅(공식)](https://aws.amazon.com/free/compute), [Aurora PostgreSQL 무료 플랜(공식)](https://aws.amazon.com/about-aws/whats-new/2026/03/amazon-aurora-postgresql-aws-free-tier), [classmethod](https://dev.classmethod.jp/en/articles/try-new-aws-free-tier-2025/), [Builder Center](https://builder.aws.com/content/3Kkt4kNiDSUnO3n6db2weSNubzE/the-dollar0-aws-account-in-2026-free-plan-credits-guardrails), [Help Net Security](https://helpnetsecurity.com/2026/09/17/aws-spend-limit-agent-set-permissions)
- Render: [공식 문서](https://render.com/docs/free), [temps.sh](https://temps.sh/blog/render-pricing-2026), [swyftstack](https://swyftstack.com/blog/free-postgresql-hosting)
- Koyeb: [agentdeals](https://agentdeals.dev/vendor/koyeb), [snapdeploy](https://snapdeploy.dev/state-of-free-hosting)
- Cloudflare: [Containers 가격(공식)](https://developers.cloudflare.com/containers/pricing/), [Northflank](https://northflank.com/blog/top-cloudflare-containers-alternatives), [Tunnel 가이드](https://flaviocopes.com/cloudflare-tunnel.md), [DEV](https://dev.to/recca0120/cloudflare-tunnel-in-2026-expose-localhost-without-opening-ports-or-buying-an-ip-32l5)
- Neon · Upstash: [Upstash 가격](https://upstash.com/pricing/redis), [DB 무료 비교(agentdeals)](https://agentdeals.dev/database-free-tier-comparison-2026)

## 9. 로컬 Docker 개발 환경 (PostgreSQL + Redis)

배포 전에 로컬에서 운영과 같은 DB로 개발하기 위한 설계입니다(사용자 요청 11.10). `feat/postgres-flyway`로 구현했고 결과는 9.7입니다. 사용법은 [`docs/server-run-guide.md`](server-run-guide.md)가 기준입니다. 전에는 DB가 H2 인메모리라 재시작하면 데이터가 사라지고, PostgreSQL에서만 나는 문제(4.2 타입, 4.5 트랜잭션 중단)를 로컬에서 볼 수 없었습니다.

### 9.1 바뀌는 것 한눈에

| 대상 | 지금 | 바뀐 뒤 |
| --- | --- | --- |
| `docker-compose.yml` | Redis 하나 | **PostgreSQL 16 + Redis 7**. 둘 다 `127.0.0.1`에만 포트를 연다 |
| `local` 프로필 DB | H2 인메모리(`MODE=MySQL`), `ddl-auto: create` | Docker PostgreSQL, **Flyway 마이그레이션 + `ddl-auto: validate`** |
| 데이터 수명 | 서버 재시작 시 사라짐 | Docker 볼륨에 남음(초기화는 9.5) |
| H2 콘솔 | `/h2-console` | 쓰지 않음(IntelliJ Database 도구나 `psql`) |
| 테스트(`test` 프로필) | H2 | H2 유지(`MODE=MySQL` 그대로, Flyway 끔) + Testcontainers PostgreSQL 스키마 검증 테스트 1개 |
| `.env` | `JWT_SECRET` | `JWT_SECRET` + `DB_NAME` · `DB_USERNAME` · `DB_PASSWORD` |
| 의존성 | H2(`runtimeOnly`) | `org.postgresql:postgresql`(runtime), `flyway-core` + `flyway-database-postgresql`(Boot 3.3이 Flyway 10 버전 관리), H2는 `testRuntimeOnly`로, Testcontainers `postgresql` 모듈 추가 |

### 9.2 `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: blinkerleague-postgres
    environment:
      # 값은 프로젝트 루트 .env에서 읽는다(docker compose가 자동으로 읽음). 없으면 compose가 바로 멈춘다.
      POSTGRES_DB: ${DB_NAME:?DB_NAME을 .env에 설정하세요}
      POSTGRES_USER: ${DB_USERNAME:?DB_USERNAME을 .env에 설정하세요}
      POSTGRES_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD를 .env에 설정하세요}
      TZ: Asia/Seoul
    ports:
      - "127.0.0.1:5432:5432"   # 내 PC에서만 접속. 같은 네트워크의 다른 기기에 열지 않는다
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 5s
      timeout: 3s
      retries: 10

  redis:
    image: redis:7-alpine
    container_name: blinkerleague-redis
    command: ["redis-server", "--appendonly", "yes"]
    ports:
      - "127.0.0.1:6379:6379"   # 지금은 0.0.0.0에 열려 있음 → 내 PC로 좁힌다
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

volumes:
  postgres-data:
  redis-data:
```

- **`.env` 하나를 Spring과 docker compose가 같이 읽는다.** compose는 프로젝트 루트의 `.env`를 자동으로 읽고, Spring `local`은 이미 `optional:file:.env[.properties]`로 읽는다. 비밀번호를 두 군데에 적지 않는다.
- 비밀값에 기본값을 두지 않는다(`${DB_PASSWORD:?...}`): 없으면 compose가 이유를 출력하고 멈춘다(CLAUDE.md 보안 규칙).
- 이미지 버전은 운영(3장)과 같게 PostgreSQL 16 · Redis 7로 맞추고, Testcontainers도 같은 이미지를 쓴다.

### 9.3 `.env.example`

```properties
# 생성: openssl rand -base64 48
JWT_SECRET=
# 로컬 PostgreSQL(docker compose와 Spring local 프로필이 같이 읽음)
DB_NAME=blinkerleague
DB_USERNAME=blinkerleague
# 생성: openssl rand -hex 24
DB_PASSWORD=
```

`DB_NAME` · `DB_USERNAME`은 비밀이 아니라 예시값을 적고, `DB_PASSWORD`는 빈 칸으로 둔다. 이름은 운영 `prod` 프로필(5장)과 같게 맞춘다(운영은 `DB_URL`을 따로 받는다).

### 9.4 `application-local.yaml`

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
  datasource:
    url: jdbc:postgresql://localhost:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate      # 스키마는 Flyway만 만든다(4.1)
    properties:
      hibernate:
        format_sql: true
    show-sql: true
  flyway:
    enabled: true             # 기동할 때 db/migration을 적용
```

- H2 콘솔 설정(`spring.h2.console`)은 지우고 H2는 `testRuntimeOnly`로 옮긴다. `SecurityConfig`의 `/h2-console/**` 허용 제거는 후속 작업(9.7 남은 것)이다.
- **DB가 꺼져 있으면 서버가 기동하지 않는다.** Redis는 연결이 지연돼 없이도 뜨지만, DataSource와 Flyway는 시작할 때 연결한다. 그래서 순서는 항상 `docker compose up -d` → `./gradlew bootRun`.
- `DB_PASSWORD`가 없거나 `${...}`로 남으면 Spring의 연결 오류 대신 환경변수 이름을 말하는 메시지로 멈추게 한다(`JwtConfigTest`처럼 기동 검증 + red-green 테스트, CLAUDE.md "Validate at startup").

### 9.5 개발자 사용법 (README에 옮길 내용)

| 할 일 | 명령 |
| --- | --- |
| 처음 한 번 | `cp .env.example .env && chmod 600 .env` 후 `JWT_SECRET` · `DB_PASSWORD` 채우기 |
| DB · Redis 시작 | `docker compose up -d` → `docker compose ps`에서 둘 다 `healthy` 확인 |
| 서버 실행 | `./gradlew bootRun` (Flyway가 스키마를 만들고, 시드가 기준값 · 미션 · 배지를 넣음) |
| DB 접속 | `docker exec -it blinkerleague-postgres psql -U blinkerleague blinkerleague` 또는 IntelliJ Database(`localhost:5432`) |
| 중지(데이터 유지) | `docker compose stop` |
| **DB만 초기화** | `docker compose rm -sf postgres && docker volume rm server_postgres-data && docker compose up -d postgres` |
| 전부 초기화 | `docker compose down -v` (**Redis의 리프레시 토큰도 지워져 앱에서 재등록 필요**) |
| 5432 포트 충돌 | PC에 PostgreSQL이 이미 깔려 있으면(`brew services list`) 끄거나, compose 포트를 `127.0.0.1:15432:5432`로 바꾸고 URL도 맞춤 |

볼륨 이름은 compose 프로젝트 이름(폴더 이름 `server`)이 앞에 붙는다. `docker volume ls`로 확인한다.

### 9.6 데이터가 남게 되면서 생기는 것

- **시드는 이미 반복 실행에 안전하다**: 기준값은 `seedIfAbsent`, 미션 · 배지는 비어 있을 때만 넣는다. 그래서 **시드 JSON을 고쳐도 기존 로컬 DB에는 반영되지 않는다.** 로컬에서는 DB만 초기화(9.5)하면 되고, 운영은 4.6 경로를 따른다.
- 엔티티를 바꾸면 `validate`가 기동을 막는다. 새 `V{n}__*.sql`을 먼저 쓴다. 이미 적용된 마이그레이션을 고쳐 체크섬 오류가 나면 로컬은 DB만 초기화한다(운영에서는 절대 고치지 않음, 4.1).
- 시간 경계 테스트용으로 오래된 운행을 쌓아 두면 D17(과거 7일) 때문에 같은 요청을 다시 올릴 수 없다. 재현 데이터가 필요하면 `http/*.http` 시나리오를 다시 실행한다.

### 9.7 구현 결과 (`feat/postgres-flyway`, 2026-11-10)

| # | 동작(테스트) | 종류 | 실제 red |
| --- | --- | --- | --- |
| 1 | PostgreSQL에서 Flyway 마이그레이션 뒤 엔티티 스키마 검증을 통과한다(`PostgresSchemaTest`) | Testcontainers PostgreSQL + `@DataJpaTest`(`ddl-auto: validate`) | `Schema-validation: missing table [app_config]` |
| 2 | PostgreSQL 비밀번호가 없으면 `DB_PASSWORD`를 안내하며 기동을 멈춘다(`DatabaseSecretValidatorTest`) | 단위 테스트(`EnvironmentPostProcessor`) | 예외가 나지 않음 |
| 3 | 치환되지 않은 `${DB_PASSWORD}`도 미설정으로 본다 | 같은 클래스 | Spring의 `IllegalArgumentException: Could not resolve placeholder` |

- `@Lob String`이 PostgreSQL에서 `oid`로 만들어지는 것을 Hibernate DDL로 확인했고(4.2), `@JdbcTypeCode(SqlTypes.LONG32VARCHAR)`(→ `text`)로 바꿨다. 사이클 1의 green에 포함된다.
- enum 컬럼에는 DB check 제약을 두지 않았다(`V1__init.sql` 머리말). 허용 값은 앱이 검증하므로(D21) 값을 늘릴 때 마이그레이션이 필요 없다.
- H2 테스트 모드는 `MODE=MySQL` 그대로 두었다. 스키마 어긋남은 `PostgresSchemaTest`가 잡는다.
- 수동 확인: 로컬 PostgreSQL로 `bootRun` → 등록 · 업로드(201 `SCORED`) · 재전송(200) · 점수 · 포인트 · 미션 · 배지 · 리그 조회가 정상, 재시작 뒤 데이터 유지와 `Schema "public" is up to date`, `DB_PASSWORD`가 비면 안내 메시지로 기동 실패.
- **남은 것**: `EarnPointConcurrencyTest`(동시 적립 · `SELECT ... FOR UPDATE`)를 PostgreSQL에서 돌리는 확인(4.5), `SecurityConfig`의 `/h2-console/**` 허용 제거(H2가 런타임에서 빠져 콘솔은 더 이상 없음).
