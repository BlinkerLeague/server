# 깜빡이리그 서버 개발 계획 (feat별)

기준 문서: 깜빡이리그 개발 기획서 (2026-10-26)
대상: 이 저장소(백엔드 서버). 차량 앱 작업은 서버와 맞물리는 부분만 적습니다.

> 서버 언어는 **Java 17 + Spring Boot 3.3 + Lombok**으로 확정합니다.
> 기획서의 "백엔드(Kotlin · Spring Boot)" 표기와 관계없이 서버 코드는 모두 Java로 작성합니다. (차량 앱은 기획서대로 Kotlin)

## 0. 진행 전에 정할 것

| # | 항목 | 현재 상태 | 제안 |
| --- | --- | --- | --- |
| D2 | 운전자 인증 | 기획서에 정의 없음. Spring Security가 모든 엔드포인트를 막고 있음 | **JWT(Access + Refresh, Refresh는 Redis)로 결정 → 8장.** 기존 제안: `POST /api/drivers`에서 `driverId` + 랜덤 토큰 발급, 이후 `Authorization: Bearer <token>` 헤더로 식별 |
| D3 | 운영 DB | H2 인메모리만 있음 | MVP 시연까지 H2(파일 모드 또는 인메모리), 2차에서 MySQL/PostgreSQL을 docker-compose에 추가 |
| D4 | 시간대 | — | 모든 일 단위 집계(미션 period_key, 일일 상한)는 `Asia/Seoul` 기준 |
| D5 | 지역 안전지수 정의 | 기획서에 계산식 없음 | 시즌 내 지역 운전자들의 운행 점수 평균(거리 가중). 2차 착수 전 확정 |
| D6 | 홈(SCR-01) 연승 표시 | 화면은 P0 홈에 "5일 연승"을 보여주지만 연승 계산은 P1 `feat/streak-badge` | **미정.** A) P0 `feat/daily-mission` 응답에 `streakDays` 추가 / B) P0 동안 화면에서 연승 카드를 숨김(plan 변경 없음) |
| D7 | 주간 리포트(SCR-07) "퀘스트 수락" 버튼 | plan은 맞춤 미션을 배치로 자동 생성 | **미정.** A) 수락 API와 미수락 상태 추가 / B) 화면을 "다음 주 퀘스트로 등록됨" 안내로 변경(plan 변경 없음) |
| D8 | 설정(SCR-08) "리그 순위 공개" 토글 | 개발 기획서 · plan 어디에도 없음 | **미정.** A) `driver`에 공개 여부 컬럼과 리그 집계 제외 로직 추가 / B) 화면에서 제거(plan 변경 없음) |
| D9 | "측정 누락 구간" 예외 상태 | 서버는 누락 구간을 알 수 없음 | **미정.** A) 업로드 요청에 누락 구간 필드 추가 / B) 앱에서만 표시하는 문구로 둠(plan 변경 없음) |
| D10 | 운행 한 건의 참여 조건 | 1km 미만 제외만 있음. 짧은 운행이 고득점에 유리하다는 우려 | **결정(11.3): 최소 거리 3km**(`trip.minDistanceKm`, 기준값 설정). **최소 회전 수는 두지 않음**: 회전 0회 운행도 참여로 인정하고 깜빡이 항목만 미측정(`NO_TURN`). 고득점 악용은 운행 한 건이 아니라 리그 순위 자격(D13)과 거리 가중 평균으로 막는다 |
| D11 | 리그 단위 | `region_code`(시군구 5자리)를 그대로 쓰면 마포구처럼 작은 리그가 생겨 초기 사용자가 적을 때 비어 버림 | **결정(11.3): 리그 권역 매핑표.** 저장은 시군구 코드 그대로, `시군구 → 리그 권역` 매핑표로 묶는다. 처음에는 크게 묶고(예: 서울은 생활권 5개, 마포는 은평 · 서대문과 서북권) 사용자가 늘면 나눈다. 매핑만 바꾸면 되므로 데이터 이전 없음. 참여자 10명 미만 리그는 순위를 공개하지 않는다 |
| D12 | 리그 소속 기준 | — | **결정(11.3): 등록한 거주 지역.** 위치 정보가 필요 없음(개인정보 요구사항). 시즌 중 지역을 바꾸면 다음 시즌부터 반영해 리그 이동을 막는다 |
| D13 | 리그 순위 자격 | — | **결정(11.3): 시즌 누적 50km 이상 또는 운행 10회 이상**인 운전자만 순위에 오른다(값은 기준값 설정으로 두고 조정). 짧은 고득점 운행 몇 번으로 순위를 올리지 못하게 한다 |
| D14 | 차선 변경 깜빡이 | 회전(방향 60° 이상 변화)만 판정. 차선 변경은 방향 변화가 작아 커브와 구분이 어렵고 GPS로는 차선을 알 수 없음 | **결정(11.3): P0은 회전만.** `feat/trip-upload`에서 `LANE_CHANGE` 이벤트를 받아 저장만 한다. 차량 앱 팀이 조향각 · 자이로 등으로 감지 가능성을 확인한 뒤 P1에서 깜빡이 항목에 반영 |
| D15 | 업로드 요청 형식 | plan은 "기획서 8.2 형식 그대로"인데 기획서가 저장소에 없음 | **결정(11.3): 3.4의 제안 형식으로 구현.** 기획서 8.2와 다르면 DTO만 맞춘다 |

### 화면 기획서 반영 (2026-11-01)

화면 기획서를 작성하면서 화면을 그릴 데이터가 없던 부분을 각 브랜치에 반영했다. 결정이 필요한 항목은 위 D6 ~ D9.

| 브랜치 | 반영 내용 | 화면 |
| --- | --- | --- |
| `feat/threshold-config` | 등급 기준, 항목별 팁 문구를 설정에 포함 | SCR-02 등급, SCR-02b 팁 |
| `feat/trip-score` | 응답에 `grade`, `summary`(득점 · 실점 · 반칙), `plays`(이벤트 목록), 그날 미션 판정 결과 | SCR-02, SCR-02a, VOC-02 |
| `feat/trip-score` | 최근 경기 목록 `GET /api/trips?limit=2` 신설 | SCR-01 최근 경기 |
| `feat/trip-upload` | `POST /api/trips` 응답을 점수 API와 같은 구조로 통일 | VOC-02 (업로드 직후 음성 문구 생성) |
| `feat/daily-mission` | 응답에 `rewardPoint`, 조건별 진행도(회전 수, 사용률) | SCR-01 진행 링, SCR-03 |
| `feat/score-full-metrics` | 측정 제외 항목도 `status`(`UNSUPPORTED`/`NOT_APPLICABLE`)와 사유로 반환, 항목별 근거 수치 `detail` | SCR-02 회색 항목, SCR-02b |
| `feat/score-full-metrics` | 회전 0회는 운행 상태가 아니라 깜빡이 항목의 제외 사유. 나머지 항목으로 평점 | SCR-02, 예외 상태 |
| `feat/point-ledger` | `GET /api/points`에 `todayEarned`, `dailyCap`, 원장 항목별 설명(거리 × 평점). 원장에 적립 시점 거리 · 점수 저장 | SCR-06 |
| `feat/streak-badge` | 획득 배지뿐 아니라 전체 배지 카탈로그(잠금 상태 + 조건) 반환 | SCR-05 진열장 |
| `feat/region-league` | 시즌 내 지역별 안전지수 목록과 시즌 종료일 `endsAt` | SCR-04 월드맵, D-12 표시 |
| `feat/weekly-report` | 항목별 전주 대비 변화와 가장 약한 항목을 응답에 명시 | SCR-07 |

**P0 시연에 직접 걸리는 것은 위쪽 다섯 줄(`threshold-config` ~ `daily-mission`)이다. 11.8 전에 반영해야 1차 시연의 경기 결과 · 홈 · 퀘스트 화면이 서버 데이터로 그려진다.**

## 1. 브랜치 · 작업 순서

브랜치 규칙은 README를 따릅니다(`develop`에서 `feat/<이름>` 분기 → PR로 `develop` 병합).

```
[P0 · 11.2 ~ 11.8]
feat/common-setup
  ├─ feat/driver-register
  │    └─ feat/threshold-config
  │         └─ feat/trip-upload
  │              └─ feat/trip-score
  │                   └─ feat/daily-mission
[P1 · 11.23 ~ 12.2]
feat/score-full-metrics   (trip-score 이후)
feat/point-ledger         (daily-mission 이후)
  └─ feat/streak-badge
feat/region-league        (point-ledger, score-full-metrics 이후)
feat/weekly-report        (score-full-metrics, daily-mission 이후)
[P2]
feat/reward-exchange      (point-ledger 이후)
```

| 순서 | 브랜치 | 관련 FR | 단계 | 목표 기간 |
| --- | --- | --- | --- | --- |
| 1 | `feat/common-setup` | — | P0 | 11.2 |
| 2 | `feat/driver-register` | — | P0 | 11.2 ~ 11.3 |
| 3 | `feat/threshold-config` | 5장 전체 | P0 | 11.3 |
| 4 | `feat/trip-upload` | FR-01, FR-04 | P0 | 11.4 ~ 11.5 |
| 5 | `feat/trip-score` | FR-03, FR-05 | P0 | 11.6 |
| 6 | `feat/daily-mission` | FR-06 | P0 | 11.7 ~ 11.8 |
| 7 | `feat/score-full-metrics` | FR-08, FR-09, FR-10 | P1 | 11.23 ~ 11.25 |
| 8 | `feat/point-ledger` | FR-12 | P1 | 11.24 ~ 11.26 |
| 9 | `feat/streak-badge` | FR-11 | P1 | 11.26 ~ 11.27 |
| 10 | `feat/region-league` | FR-13 | P1 | 11.27 ~ 11.30 |
| 11 | `feat/weekly-report` | FR-14 | P1 | 11.30 ~ 12.1 |
| 12 | `feat/reward-exchange` | FR-15 | P2 | 미정 |

FR-02(회전 감지), FR-07(음성 피드백)은 차량 앱 범위라 서버 브랜치가 없습니다. 서버는 판정이 끝난 이벤트만 받습니다.

## 2. 패키지 구조

```
com.blinkerleague.server
├── common      # 공통 응답, 예외 처리, BaseTimeEntity, 시간 유틸
├── security    # 토큰 인증 필터, SecurityConfig
├── driver
├── config      # 측정 기준값 · 가중치 설정 (threshold)
├── trip        # 운행 · 이벤트 업로드
├── score       # 점수 엔진 (순수 계산 로직)
├── mission
├── point
├── badge       # 연속 기록 · 배지
├── league
└── report
```

각 도메인 패키지는 `controller / service / repository / domain / dto` 하위 구조를 씁니다.

---

## 3. P0 (1차 제출, 11.15)

### 3.1 `feat/common-setup` — 공통 기반

작업:
- 위 패키지 구조 생성, `BaseTimeEntity`(`createdAt`, `updatedAt`) + JPA Auditing
- 공통 에러 응답(`{ code, message }`)과 `@RestControllerAdvice`
- `SecurityConfig`: CSRF 비활성화, 세션 STATELESS, `/api/drivers`(POST) · `/api/config/**` · H2 콘솔은 허용, 나머지는 인증 필요 (인증 필터 자체는 D2 확정 후 `feat/driver-register`에서 구현)
- `application.yaml`: JPA `ddl-auto`, 시간대 `Asia/Seoul`, Jackson 날짜를 ISO-8601 오프셋 형식으로
- 프로필 분리: `local`(H2 + 로컬 Redis), `test`

완료 기준: `./gradlew build` 통과, 허용 경로 외 요청은 401.

상태: 완료 (PR #5). 남은 항목: `BaseTimeEntity`가 `LocalDateTime`이라 Jackson 출력에 오프셋(`+09:00`)이 없음. `OffsetDateTime` 전환 여부는 후속 결정.

### 3.2 `feat/driver-register` — 운전자 등록

상태: 완료. 로그인이 없으므로 토큰을 잃으면 재등록한다(P0 결정, 복구 방식은 P1에서 결정 · 7장). 인증은 8장(JWT, Refresh는 Redis)을 따른다.

API: `POST /api/drivers`

```json
// 요청
{ "nickname": "깜빡왕", "regionCode": "11680" }
// 응답 201
{ "driverId": 1, "nickname": "깜빡왕", "regionCode": "11680", "accessToken": "...", "refreshToken": "..." }
```

구현:
- `driver` 테이블: `driver_id`, `nickname`(유니크, 최대 12자), `region_code`(5자리), `created_at`, `updated_at`. 토큰은 DB에 저장하지 않음(Refresh 해시는 Redis, 8장). 실명 · 연락처 컬럼은 만들지 않음
- 등록 시 Access JWT + Refresh 토큰 발급(`auth` 기능 재사용). 보호 API는 `Authorization: Bearer <access JWT>`로 인증하고, 컨트롤러는 `@CurrentDriver`로 `driverId`를 받음(8장에서 구현)
- 닉네임 중복은 409(`CONFLICT`). 동시 등록으로 생기는 중복은 DB 유니크 제약이 막고 같은 409로 응답하며, 유니크 제약이 아닌 무결성 오류는 409로 바꾸지 않음
- 검증: 닉네임 2~12자, 지역 코드 숫자 5자리(법정동 시군구 코드로 가정). 위반 시 400

테스트: 등록 성공(201 + 토큰), 닉네임 중복 409(순차 · 동시), 입력 검증 400, 토큰 없이 보호 API 호출 시 401, 등록 토큰으로 보호 API 통과 후 Refresh 재발급까지 전체 흐름.

### 3.3 `feat/threshold-config` — 기준값 · 가중치 설정

상태: 완료. 기본값은 `src/main/resources/thresholds/default-thresholds.json`이고, 서버 시작 시 저장된 값이 없을 때만 version 1로 시드한다(재시작해도 바꾼 값을 덮어쓰지 않음). `app_config` 테이블의 `thresholds` 행에 JSON으로 저장하며, `value`가 H2 예약어라 컬럼 이름은 `config_value`다. 서버 내부는 `ThresholdProvider`(메모리 캐시, 변경 시 갱신)로 읽는다. 남은 항목: 값 변경용 관리자 API(지금은 `ThresholdService.update`만 있음) · 변경 값 검증(가중치 합 1 등), 50점 미만 등급 표기(지금은 등급 없음), `TURN_SIGNAL` 외 팁 문구 확정, 서버를 여러 대로 늘릴 때 캐시 갱신 전파.

API: `GET /api/config/thresholds` (앱이 시작할 때 내려받음)

```json
{
  "version": 1,
  "trip": { "minDistanceKm": 3.0, "parkEndMinutes": 3 },
  "turn": { "minSpeedKmh": 5, "windowSec": 10, "minAngleDeg": 60, "uTurnAngleDeg": 150, "signalLeadSec": 5 },
  "harsh": { "deltaKmhPerSec": 10 },
  "safetyWarning": { "mergeWindowSec": 10 },
  "weights": { "TURN_SIGNAL": 0.4, "SMOOTH_DRIVING": 0.3, "SAFETY_WARNING": 0.2, "NIGHT_HEADLIGHT": 0.1 },
  "point": { "tripMaxKm": 50, "dailyCap": 150 },
  "grades": [
    { "grade": "S", "minScore": 95 },
    { "grade": "A", "minScore": 85 },
    { "grade": "B", "minScore": 70 },
    { "grade": "C", "minScore": 50 }
  ],
  "tips": {
    "TURN_SIGNAL": "회전 5초 전에 깜빡이를 켜 보세요.",
    "SMOOTH_DRIVING": "...",
    "SAFETY_WARNING": "...",
    "NIGHT_HEADLIGHT": "..."
  }
}
```

- `grades`: 점수 → 등급 기준(SCR-02). 높은 기준부터 비교해 처음 만족하는 등급. 50점 미만의 등급 표기는 화면 기획서와 확정
- `tips`: 항목별 개선 팁 문구(SCR-02b). 문구는 코드가 아니라 설정에 둔다(6장 규칙)

작업:
- `app_config` 테이블(`config_key`, `value`(JSON), `version`, `updated_at`) 또는 키-값 행 구조. 초기값은 `data.sql`(또는 시작 시 시드)로 넣음
- 서버 내부에서도 같은 값을 쓰도록 `ThresholdProvider` 빈 제공 (캐시 + 변경 시 갱신)
- 응답에 `version`을 넣어 앱이 바뀐 경우에만 갱신

테스트: 시드 값 조회, 값 변경 후 version 증가, 경계 점수별 등급(95 → S, 94 → A, 50 → C), 항목별 팁 문구 포함.

### 3.4 `feat/trip-upload` — 운행 · 이벤트 업로드 (FR-01, FR-04)

상태: 완료. 응답은 지금 `{ tripId, status }`만 채운다(점수 · 미션 필드는 3.5 · 3.6에서 추가). 새로 저장하면 201, 재전송은 200. 동시 업로드는 `Trip`이 `Persistable`로 INSERT를 강제해 PK 충돌로 감지하고 다시 조회한다(merge로 덮어쓰지 않음). 이벤트 시각은 시작 · 종료 시각을 포함한 구간 안이어야 한다. 남은 항목: 점수 계산 · 미션 판정 확장 지점은 연결할 대상이 생기는 3.5에서 만든다. 다른 운행의 `event_id`와 충돌하면 지금은 저장 실패(500)로 끝난다(앱이 UUID를 쓰면 사실상 발생하지 않음).

API: `POST /api/trips` (인증 필요. 요청 형식은 아래 제안 형식, D15)

```json
{
  "tripId": "5f1c...",
  "startedAt": "2026-11-03T08:00:00+09:00",
  "endedAt": "2026-11-03T08:40:00+09:00",
  "distanceKm": 12.3,
  "supported": ["TURN_SIGNAL"],
  "events": [
    { "eventId": "a1b2...", "type": "TURN", "occurredAt": "2026-11-03T08:12:30+09:00",
      "payload": { "direction": "LEFT", "signalOn": true } }
  ]
}
```

- `supported`: 차량이 측정할 수 있는 점수 항목 이름(`ScoreItem`: `TURN_SIGNAL`, `SMOOTH_DRIVING`, `SAFETY_WARNING`, `NIGHT_HEADLIGHT`)
- `payload`: 서버는 해석하지 않고 JSON 그대로 저장한다. `TURN`의 `signalOn` 해석은 `feat/trip-score`에서

작업:
- `trip` 테이블: `trip_id`(클라이언트 UUID, PK), `driver_id`, `started_at`, `ended_at`, `distance_km`, `supported_signals`, `score`(nullable), `status`
  - `status`: `UPLOADED`(업로드됨, 점수 계산 전), `SCORED`, `NO_TURN`(회전 0회), `EXCLUDED_SHORT`(최소 거리 3km 미만, D10)
  - 이 브랜치에서는 최소 거리 미만이면 `EXCLUDED_SHORT`, 나머지는 `UPLOADED`로 저장한다. `feat/trip-score`에서 점수를 계산해 `SCORED` 또는 `NO_TURN`으로 바꾼다
  - 최소 거리는 상수가 아니라 `ThresholdProvider`의 `trip.minDistanceKm`에서 읽는다
- `trip_event` 테이블: `event_id`(클라이언트 UUID, PK), `trip_id`, `type`, `occurred_at`, `payload`(JSON 문자열)
  - `type`: `TURN`, `LANE_CHANGE`, `HARSH_ACCEL`, `HARSH_BRAKE`, `SAFETY_WARNING`, `NIGHT_SEGMENT` (P0은 `TURN`만 점수에 쓰고, 나머지는 저장만. `LANE_CHANGE`는 D14)
- **멱등 처리**: 같은 `trip_id`가 이미 있으면 저장하지 않고 기존 결과를 200으로 반환. 동시 요청은 PK 충돌(`DataIntegrityViolationException`)을 잡아 재조회
- 다른 운전자의 `trip_id`로 재전송하면 409
- 검증: `endedAt > startedAt`, `distanceKm >= 0`, 이벤트 시각이 운행 구간 안
- 원시 위치 좌표 필드는 받지 않음(DTO에 없음) — 개인정보 요구사항
- 업로드 처리 끝에 점수 계산 · 미션 판정을 호출할 확장 지점만 만들어 둠 (다음 브랜치에서 연결)
- **응답은 3.5 점수 응답과 같은 구조**(`TripResult`)로 통일한다. 앱이 업로드 직후 이 응답만으로 음성 문구(VOC-02)를 만들 수 있어야 한다. 이 브랜치에서는 `tripId` · `status` 등 업로드 결과만 채우고, 점수 관련 필드는 `feat/trip-score`, 미션 필드는 `feat/daily-mission`에서 채운다

테스트: 정상 업로드(`UPLOADED`), 같은 `trip_id` 2회 업로드 시 1건만 저장, 최소 거리(3km) 미만은 `EXCLUDED_SHORT`, 다른 운전자의 `trip_id`는 409, 검증 실패는 400.

### 3.5 `feat/trip-score` — 점수 엔진 (FR-03, FR-05)

API: `GET /api/trips/{tripId}/score`. `POST /api/trips` 응답도 이와 같은 구조(`TripResult`)

```json
{
  "tripId": "5f1c...",
  "status": "SCORED",
  "score": 75,
  "grade": "B",
  "metrics": [
    { "metric": "TURN_SIGNAL", "value": 0.75, "score": 75, "weight": 1.0 }
  ],
  "turns": { "total": 4, "success": 3 },
  "summary": { "goals": 3, "conceded": 1, "fouls": 0 },
  "plays": [
    { "occurredAt": "2026-11-03T08:12:30+09:00", "type": "TURN", "result": "GOAL" },
    { "occurredAt": "2026-11-03T08:15:02+09:00", "type": "TURN", "result": "CONCEDED" }
  ],
  "mission": { "missionId": 1, "status": "IN_PROGRESS" }
}
```

- `grade`: 3.3 `grades` 기준으로 점수에서 계산(SCR-02). 점수가 없으면 `null`
- `summary`: 경기 비유 집계(SCR-02a). 제안: 득점 = 깜빡이를 켠 회전, 실점 = 켜지 않은 회전, 반칙 = 급가감속 · 안전 경고(P0은 0). 정의는 화면 기획서와 확정
- `plays`: 운행 중 이벤트를 시각순으로(SCR-02a). `result`는 `summary`와 같은 정의
- `mission`: 이 운행을 반영한 그날 미션 판정 결과(SCR-02, VOC-02). `feat/daily-mission` 전까지는 `null`

작업:
- `ScoreCalculator`: 입력(운행 + 이벤트 + 지원 신호 + 설정) → 출력(항목별 점수, 적용 가중치, 총점). DB · 스프링에 의존하지 않는 순수 클래스로 만들어 단위 테스트
- 깜빡이 항목: `TURN` 이벤트의 `payload.signalOn`이 true인 비율 × 100 (판정 자체는 앱이 수행)
- **가중치 재분배**: 측정 불가 항목(미지원 신호, 회전 0회 등)을 빼고 남은 가중치를 합이 1이 되도록 비율대로 조정. P0에서는 결과적으로 깜빡이 항목만 가중치 1.0
- `trip_metric` 테이블: `trip_id`, `metric`, `value`, `score`, `weight`
- 회전 0회 → `status=NO_TURN`, `score=null` (앱은 "측정할 회전이 없었어요" 표시). P0은 측정 항목이 깜빡이뿐이라 이렇게 두고, 4.1에서 깜빡이 항목의 제외 사유로 바꾼다
- 남의 운행 조회 시 404
- **최근 경기 목록** `GET /api/trips?limit=2`(SCR-01): 내 운행을 종료 시각 최신순으로. 항목은 `tripId`, `endedAt`, `distanceKm`, `status`, `score`, `grade`. `limit` 기본 · 최댓값은 착수 시 확정

테스트: 회전 4회 중 3회 성공 → 75점, 회전 0회 → `NO_TURN`, 재분배 계산(가중치 합 1) 단위 테스트, 점수별 `grade`, `summary` · `plays` 집계, 최근 경기 목록 정렬 · `limit` · 남의 운행 제외. 응답 시간 3초 목표는 통합 테스트에서 대략 확인.

### 3.6 `feat/daily-mission` — 오늘의 미션 (FR-06)

API: `GET /api/missions/today`

```json
{
  "missionId": 1,
  "title": "회전 깜빡이 완벽",
  "periodKey": "2026-11-03",
  "status": "IN_PROGRESS",
  "rewardPoint": 20,
  "progress": { "turns": 2, "successTurns": 2, "requiredTurns": 3 },
  "conditions": [
    { "type": "TURN_COUNT", "current": 2, "target": 3 },
    { "type": "SIGNAL_RATE", "current": 100, "target": 100 }
  ]
}
```

- `rewardPoint`: 달성 시 지급 포인트(SCR-03). 실제 지급은 `feat/point-ledger`
- `conditions`: 조건별 진행도(SCR-01 진행 링, SCR-03). 회전 수와 깜빡이 사용률(%)
- 홈 연승 표시(`streakDays`)를 여기에 넣을지는 D6 결정에 따름

작업:
- `mission` 테이블: `mission_id`, `type`, `period`(`DAILY`/`WEEKLY`), `condition`(JSON), `reward_point`, `active_from`, `active_to`. "회전 깜빡이 완벽"(회전 3회 이상, 사용률 100%, 20P) 시드
- `driver_mission` 테이블: `driver_id`, `mission_id`, `period_key`, `status`(`IN_PROGRESS`/`ACHIEVED`/`FAILED`), `achieved_at`. (`driver_id`, `mission_id`, `period_key`) 유니크
- 운행 업로드 후 그날(Asia/Seoul) 점수 대상 운행들의 회전을 합산해 판정. 한 번이라도 실패 회전이 있으면 그날은 `FAILED`
- `EXCLUDED_SHORT` 운행은 집계에서 제외
- 포인트 지급은 P1(`feat/point-ledger`)에서 연결. 지금은 달성 상태만 기록

테스트: 회전 3회 모두 성공 → `ACHIEVED`, 2회만 → `IN_PROGRESS`, 실패 1회 포함 → `FAILED`, 자정 경계 운행의 period_key, 조건별 진행도(회전 수 · 사용률)와 `rewardPoint` 응답.

### P0 마무리 (11.9 ~ 11.15)
- 차량 앱과 실제 연동 확인(에뮬레이터 시나리오: 좌회전 성공 → 우회전 실패 → 운행 종료 점수 · 미션)
- README에 API 목록 · 실행 방법 갱신
- `develop` → `main` PR, 제출 버전 태그

---

## 4. P1 (2차 PT, 12.2)

### 4.1 `feat/score-full-metrics` — 급가감속 · 야간 전조등 · 안전 경고 (FR-08 ~ FR-10)

- `ScoreCalculator`에 항목 추가 (기획서 6.1)
  - 부드러운 가감속: `max(0, 100 − 10 × 10km당 (HARSH_ACCEL + HARSH_BRAKE))`
  - 안전 경고: `max(0, 100 − 20 × 100km당 SAFETY_WARNING)`
  - 야간 전조등: `(1 − 꺼짐 비율) × 100`. `NIGHT_SEGMENT.payload`에 `{ durationSec, headlightOffSec, autoLight }`를 받음. 오토라이트면 100점, 야간 구간 없으면 측정 불가
- `supported`에 없는 신호의 항목은 제외 후 재분배
- 이벤트 payload 스키마를 앱 팀과 확정(9장 체크리스트 5~7 결과 반영)
- **측정 제외 항목도 `metrics`에 담아 반환**(SCR-02 회색 항목): `status`는 `SCORED` / `UNSUPPORTED`(차량이 신호를 지원하지 않음) / `NOT_APPLICABLE`(측정할 상황이 없음), 제외면 `reason`과 함께 `score` · `weight`는 `null`
- **항목별 근거 수치 `detail`**(SCR-02b): 예) 급가감속 `{ "count": 1, "per10Km": 1.4 }`, 깜빡이 `{ "turns": 4, "signalOn": 3 }`. 화면 문구("급가속 1회", "10km당 1.4회")는 앱이 만든다
- **회전 0회는 운행 상태가 아니라 깜빡이 항목의 제외 사유**: `TURN_SIGNAL`을 `NOT_APPLICABLE`(`reason: NO_TURN`)로 두고 나머지 항목으로 재분배해 평점(화면: "나머지 항목으로 평점"). 운행 `status=NO_TURN`은 이 브랜치에서 없앤다. 모든 항목이 제외된 경우의 상태 이름은 착수 시 확정

```json
"metrics": [
  { "metric": "TURN_SIGNAL", "status": "NOT_APPLICABLE", "reason": "NO_TURN", "score": null, "weight": null },
  { "metric": "SMOOTH_DRIVING", "status": "SCORED", "score": 86, "weight": 0.6, "detail": { "count": 1, "per10Km": 1.4 } },
  { "metric": "NIGHT_HEADLIGHT", "status": "UNSUPPORTED", "reason": "SIGNAL_NOT_SUPPORTED", "score": null, "weight": null }
]
```

테스트: 항목별 경계값, 지원 신호 조합별 재분배, 제외 항목의 `status` · `reason`, `detail` 수치, 회전 0회일 때 깜빡이 제외 후 나머지로 평점.

### 4.2 `feat/point-ledger` — 포인트 원장 (FR-12)

API: `GET /api/points` (잔액 + 최근 원장 20건)

```json
{
  "balance": 1240,
  "todayEarned": 62,
  "dailyCap": 150,
  "entries": [
    { "amount": 42, "reason": "TRIP", "description": { "distanceKm": 12.3, "score": 86 }, "createdAt": "2026-11-03T08:40:00+09:00" },
    { "amount": 20, "reason": "DAILY_MISSION", "description": null, "createdAt": "2026-11-03T08:40:00+09:00" }
  ]
}
```

- `todayEarned` · `dailyCap`: 오늘(Asia/Seoul) 적립 합계와 일일 상한(SCR-06)
- 원장 항목별 설명(거리 × 평점, SCR-06): 적립 시점 값을 보여줘야 하므로 원장에 남긴다(운행을 다시 조회해 계산하지 않음)
- `point_ledger`: `ledger_id`, `driver_id`, `amount`, `reason`(`TRIP`/`DAILY_MISSION`/`STREAK_7`/`SEASON_LEAGUE`), `ref_id`, `distance_km`(nullable), `score`(nullable), `created_at`. (`reason`, `ref_id`) 유니크. `distance_km` · `score`는 `TRIP` 적립 시점 값
- 잔액 = `SUM(amount)` (잔액 컬럼 따로 두지 않음)
- 운행 적립: `floor(min(distanceKm, 50) × score ÷ 100)`, `ref_id = tripId`
- 일간 미션 달성 시 20P, `ref_id = driver_mission` ID
- **일일 상한 150P**: 그날 합계를 구해 남은 만큼만 적립. 같은 운전자의 동시 적립은 운전자 단위 비관적 락(또는 `driver` 행 `SELECT ... FOR UPDATE`)으로 직렬화
- P0에서 이미 달성된 미션에 대한 소급 적립 여부 결정

테스트: 같은 운행 재업로드 시 적립 1회, 상한 초과 시 잘림, 동시 업로드 2건에서 상한 유지, `todayEarned` 자정 경계, 원장 설명에 적립 시점 거리 · 점수.

### 4.3 `feat/streak-badge` — 연속 기록 · 배지 (FR-11)

API: `GET /api/streak`

- 연속 기록 = 오늘(또는 어제)부터 거꾸로 `driver_mission.status = ACHIEVED`인 연속 일수. 조회 시 계산하거나 `driver_streak` 캐시 테이블 유지
- 7일마다 50P (`reason=STREAK_7`, `ref_id = driverId:달성일`)
- `badge_award`: `driver_id`, `badge_code`, `awarded_at`, (`driver_id`, `badge_code`) 유니크. 배지 목록 · 조건(예: 첫 운행, 연속 7일, 깜빡이 100회)은 착수 시 확정
- **전체 배지 카탈로그 반환**(SCR-05 진열장): `GET /api/badges`에서 획득한 배지만이 아니라 전체 배지를 잠금 상태 · 조건과 함께. 예) `{ "code": "STREAK_7", "name": "...", "condition": "7일 연속 미션 달성", "earned": false, "awardedAt": null }`. 배지 정의(이름 · 조건 문구)는 `badge` 카탈로그(시드)에 둔다

### 4.4 `feat/region-league` — 지역 리그 (FR-13)

API: `GET /api/leagues/current`

- 시즌 4주. `league_season`: `season_id`, `region_code`, `starts_at`, `ends_at`, `safety_index`
- 운행 점수 확정 시 Redis ZSET `league:{season}:{region}`에 운전자 시즌 점수 반영(`ZINCRBY` 또는 평균 계산용 합계 · 횟수 별도 키)
- 순위 조회는 Redis에서(캐시 응답 요구사항). 응답에는 닉네임만, 내 순위 + 상위 N명
- 시즌 종료 배치(`@Scheduled`): 순위 · 안전지수 RDB 확정, 상위 지역 참여자 100P, 새 시즌 생성
- 지역 안전지수 계산식(D5), "같은 측정 항목 집단끼리 비교" 여부 착수 전 결정
- **리그 권역(D11)**: `league_region_mapping`(`region_code` 시군구 → `league_region`) 매핑표로 묶는다. 위의 `region`은 시군구가 아니라 리그 권역이다. 초기 매핑(서울 생활권 5개 등)은 착수 시 확정
- **소속(D12)**: 운전자의 등록 지역 기준. 시즌 중 지역 변경은 다음 시즌부터 반영(시즌 시작 시 소속 권역을 고정해 기록)
- **순위 자격(D13)**: 시즌 누적 50km 이상 또는 운행 10회 이상인 운전자만 순위에 포함. 리그 참여자가 10명 미만이면 순위 비공개(D11). 두 값 모두 기준값 설정에 둔다
- **응답에 시즌 내 지역별 안전지수 목록과 시즌 종료일**(SCR-04 월드맵, "D-12" 표시): `season: { seasonId, startsAt, endsAt }`, `regions: [{ regionCode, safetyIndex, rank }]`. 시즌 중에는 현재까지의 값(Redis 집계), 종료 후에는 확정값. D-day는 앱이 `endsAt`으로 계산
- Redis가 이미 docker-compose에 있으므로 추가 인프라 없음

테스트: Testcontainers 또는 임베디드 Redis로 순위 계산, 시즌 확정 배치.

### 4.5 `feat/weekly-report` — 주간 리포트 (FR-14)

API: `GET /api/reports/weekly?week=2026-W45` (기본: 지난주)

- 항목별 주간 평균(깜빡이 사용률, 10km당 급가감속, 100km당 경고, 전조등)과 전주 대비 변화
- 가장 약한 항목을 골라 다음 주 "맞춤 개선 미션"(주간 미션) 생성
- 매주 월요일 새벽 배치로 생성 · 저장(`weekly_report`), 조회는 저장본 반환
- 주간 미션 "안전 경고 없는 주"(50km 이상, 경고 0회)도 여기서 함께 판정
- **응답 형식**(SCR-07): 항목별 이번 주 · 지난주 값과 변화, 가장 약한 항목, 생성된 맞춤 미션. "퀘스트 수락" 버튼 유지 여부는 D7 결정에 따름

```json
{
  "week": "2026-W45",
  "metrics": [
    { "metric": "TURN_SIGNAL", "thisWeek": 0.82, "lastWeek": 0.75, "change": 0.07 },
    { "metric": "SAFETY_WARNING", "thisWeek": 1.9, "lastWeek": 1.2, "change": 0.7 }
  ],
  "weakestMetric": "SAFETY_WARNING",
  "nextMission": { "missionId": 12, "title": "안전 경고 없는 주" }
}
```

---

## 5. P2

### 5.1 `feat/reward-exchange` — 보상 교환 (FR-15)
- 제휴 쿠폰 카탈로그, 교환 시 원장에 음수 적립(`reason=EXCHANGE`), 잔액 부족 검증
- 9장 체크리스트 11번(인앱 구매 API) 결과에 따라 범위 확정

---

## 6. 공통 규칙

- 각 feat 브랜치는 해당 기능의 단위 테스트 + 컨트롤러 통합 테스트(`@SpringBootTest` + MockMvc)를 포함하고 `./gradlew build` 통과 후 PR
- 엔티티에 원시 위치 좌표 필드를 두지 않음
- 모든 업로드 · 적립은 클라이언트 ID 또는 (reason, ref_id)로 멱등
- 기준값 · 가중치 · 포인트 규칙은 코드에 상수로 두지 않고 `ThresholdProvider`에서 읽음
- API 변경 시 README의 API 표를 같은 PR에서 갱신

## 7. 차량 앱과 맞춰야 할 항목

| 항목 | 필요 시점 |
| --- | --- |
| 인증 방식(D2), 토큰 보관 | `feat/driver-register` 전 (결정: Access 1시간 + Refresh 30일, 앱은 Keystore 등 안전한 저장소에 보관) |
| 토큰 분실 시 복구(앱 삭제 · 기기 변경) | P1 착수 전. P0는 재등록, 후보는 기기 인증 정보 · 소셜 로그인 |
| `TURN` payload 필드(`direction`, `signalOn`, 필요 시 `uTurn`) | `feat/trip-upload` 전. 서버는 payload를 그대로 저장하므로 업로드는 먼저 진행, 확정은 `feat/trip-score` 전 |
| 기준값 JSON 형식 | `feat/threshold-config` 전 |
| `HARSH_*`, `SAFETY_WARNING`, `NIGHT_SEGMENT` payload | `feat/score-full-metrics` 전 (9장 5~7 결과) |
| `supported` 신호 이름 목록 | 제안: `ScoreItem` 이름(`TURN_SIGNAL` 등) 사용(D15). 앱 팀 확인 필요 |
| 차선 변경 감지 가능성(조향각 · 자이로 신호 제공 여부, 커브 오탐) | P1 착수 전 (D14) |

## 8. 운전자 인증 JWT 전환 (D2)

상태: `feat/jwt-auth`에서 완료. Access JWT 검증, `INVALID_TOKEN` 구분, `@CurrentDriver`, Redis Refresh 저장소, `POST /api/auth/refresh`, secret fail-fast 검증. 남은 항목: 등록 시 토큰 발급은 3.2 `feat/driver-register`, 운영 secret의 Vault 이전은 배포 준비 단계.

common-setup 이후 첫 작업. `feat/jwt-auth` 브랜치에서 인증 기반을 먼저 만들고, `feat/driver-register`에서 등록 API에 연결한다.

### 8.1 결정: Access + Refresh 방식

아이디 · 비밀번호 로그인 없이 운전자 등록 때 한 번 토큰을 받는 구조이므로, JWT가 만료되면 다시 받을 수단이 필요하다.

| 방식 | 내용 | 비고 |
| --- | --- | --- |
| **A. Access + Refresh (채택)** | Access JWT는 짧게(1시간), Refresh는 랜덤 값을 Redis에 해시로만 저장(30일). `POST /api/auth/refresh`로 재발급 | 도난 시 악용 시간이 최대 1시간, Refresh 삭제로 강제 로그아웃 가능 |
| B. 장기 JWT 1개 | 1년짜리 JWT 하나 | 단순하지만 도난 시 1년간 막을 수 없고 만료 후 재발급 수단이 없음 |

흐름:

```
1. 등록      POST /api/drivers        → { accessToken, refreshToken }
2. API 호출  Authorization: Bearer <accessToken>
3. 만료      API 호출                  → 401 INVALID_TOKEN
4. 재발급    POST /api/auth/refresh { refreshToken }
             → 새 accessToken + 새 refreshToken (이전 refresh는 무효)
```

### 8.2 결정: 구현 라이브러리는 resource server

OAuth 2.0의 역할 구분에서 차량 앱은 Client, 토큰을 발급하는 우리 서버는 Authorization server, 토큰을 검증하고 데이터를 내주는 우리 서버는 **Resource server**다. `spring-boot-starter-oauth2-resource-server`는 이 중 Resource server 역할(Bearer JWT 꺼내기 → 서명 · 만료 검증 → `SecurityContext` 설정, 실패 시 401)을 Spring이 대신 해 주는 모듈이다. 이름과 달리 OAuth 로그인(구글 · 카카오)과는 관계없고 토큰은 우리 서버가 직접 발급한다.

| | resource server (채택) | jjwt (0.12.x) |
| --- | --- | --- |
| 검증 필터 | Spring 제공(`BearerTokenAuthenticationFilter`) | `OncePerRequestFilter` 직접 작성 |
| 발급 | `JwtEncoder.encode(...)` | `Jwts.builder()...signWith(key).compact()` |
| 실패 처리 | entry point만 지정 | 필터에서 예외 직접 처리 |
| 주의 | `oauth2ResourceServer`에도 `authenticationEntryPoint`를 지정해야 401이 `{ code, message }` JSON으로 나감 | 0.12에서 API 변경(`parserBuilder()`, `parseClaimsJws()` deprecated) |

### 8.3 Refresh 토큰은 Redis로 관리

DB에 컬럼을 두지 않고 Redis에 저장한다. TTL로 자동 만료되고, 이미 docker-compose에 Redis가 있다.

키 설계 (원문은 저장하지 않고 SHA-256 해시만 저장):

```
auth:refresh:{hash}              → driverId      TTL 30일    # 토큰으로 운전자 찾기
auth:refresh:driver:{driverId}   → hash          TTL 30일    # 운전자의 현재 토큰 (강제 로그아웃용)
auth:refresh:used:{hash}         → driverId      TTL 남은 기간 # 이미 쓴 토큰 (탈취 감지용)
```

재발급(`POST /api/auth/refresh`):

1. 받은 토큰을 해시해 `GETDEL auth:refresh:{hash}`로 꺼내면서 지운다. 같은 토큰으로 동시에 요청해도 한 번만 성공한다.
2. 값이 있으면 그 driverId로 새 Access + 새 Refresh를 발급하고, 이전 해시는 `used`로 기록한다.
3. 값이 없고 `used`에 있으면 탈취로 보고 그 운전자의 현재 Refresh를 지운 뒤 401.
4. 둘 다 없으면 401 (만료 또는 위조).

강제 로그아웃: `driver:{id}`로 현재 해시를 찾아 관련 키를 지운다. 남은 Access는 최대 1시간 안에 만료된다.

주의: 비밀번호 로그인이 없으므로 Redis 데이터가 사라지면 운전자는 재등록해야 한다.

- `docker-compose.yml`의 Redis를 `redis-server --appendonly yes`로 실행해 AOF 영구 저장을 켠다.
- 기기 ID 기반 재발급 같은 복구 경로는 차량 앱과 논의한다(7장).

### 8.4 작업

- 의존성: `spring-boot-starter-oauth2-resource-server` 추가, 쓰지 않는 `spring-session-data-redis` 제거
- 설정: `jwt.secret`(256bit 이상, 환경변수 `JWT_SECRET`, 저장소에 커밋하지 않음), `jwt.access-ttl: 1h`, `jwt.refresh-ttl: 30d`. 테스트 설정에만 고정 secret
- `security/jwt/`
  - `JwtProperties`: 위 설정 레코드(`@ConfigurationProperties`)
  - `JwtConfig`: HS256 `JwtEncoder` · `JwtDecoder` Bean. 디코더 만료 검사에 기존 `Clock` 연결
  - `JwtTokenProvider`: `issueAccessToken(driverId)`, `sub` = driverId, `iat`/`exp`는 `Clock` 기준
  - `RefreshTokenStore`: 저장소 인터페이스. 운영은 `RedisRefreshTokenStore`, 단위 테스트는 메모리 구현
  - `RefreshTokenService`: 발급 · 재발급(rotation) · 탈취 감지 · 강제 로그아웃
- `SecurityConfig`: `.oauth2ResourceServer(o -> o.jwt(...).authenticationEntryPoint(...))`, `POST /api/auth/refresh` 허용
- `@CurrentDriver`: `JwtAuthenticationToken`의 `sub`를 `Long driverId`로 넘기는 인자 리졸버
- `ErrorCode.INVALID_TOKEN`(401) 추가
- 3.2 등록 응답의 `token`을 `accessToken` · `refreshToken`으로 분리하고 `driver.token_hash` 컬럼은 만들지 않음. README API 표와 7장 갱신

### 8.5 테스트 (red-green 순서)

단위 (스프링 없이):

1. `JwtTokenProviderTest`: 발급 토큰의 `sub`가 driverId, 만료 시각 = 발급 시각 + TTL, 만료 후 디코딩 실패, 다른 secret으로 서명한 토큰 거부
2. `RefreshTokenServiceTest`: 해시로만 저장, 재발급 시 새 토큰 발급, 사용한 토큰 재사용 시 `INVALID_TOKEN` + 운전자 토큰 무효화, 없는 토큰 거부, 강제 로그아웃 후 재발급 불가

통합:

3. `SecurityConfigTest`: 유효 토큰은 인증 통과(컨트롤러가 없으면 404), 토큰 없음 · 위조 · 만료는 401 + `INVALID_TOKEN`
4. `RedisRefreshTokenStore`: Testcontainers로 실제 Redis에서 저장 · `GETDEL` · TTL 확인
