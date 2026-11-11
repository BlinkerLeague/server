# 깜빡이리그 서버 개발 계획 (feat별)

기준 문서: 깜빡이리그 개발 기획서 (2026-10-26)
대상: 이 저장소(백엔드 서버). 차량 앱 작업은 서버와 맞물리는 부분만 적습니다.

> 서버 언어는 **Java 17 + Spring Boot 3.3 + Lombok**으로 확정합니다.
> 기획서의 "백엔드(Kotlin · Spring Boot)" 표기와 관계없이 서버 코드는 모두 Java로 작성합니다. (차량 앱은 기획서대로 Kotlin)

> **지금 상태와 다음에 할 일은 [9장](#9-다음에-할-일-2026-11-09-기준)에 정리했습니다.**

## 0. 진행 전에 정할 것

| # | 항목 | 현재 상태 | 제안 |
| --- | --- | --- | --- |
| D2 | 운전자 인증 | 기획서에 정의 없음. Spring Security가 모든 엔드포인트를 막고 있음 | **JWT(Access + Refresh, Refresh는 Redis)로 결정 → 8장.** 기존 제안: `POST /api/drivers`에서 `driverId` + 랜덤 토큰 발급, 이후 `Authorization: Bearer <token>` 헤더로 식별 |
| D3 | 운영 DB | H2 인메모리만 있음 | **결정(11.9): PostgreSQL.** docker-compose에 추가하고 스키마는 Flyway로 관리(`ddl-auto: validate`). **`local`도 Docker PostgreSQL, 테스트는 H2 + PostgreSQL 스키마 검증 테스트(11.10 결정, `docs/deployment.md` D37) → `feat/postgres-flyway`에서 완료** |
| D4 | 시간대 | — | 모든 일 단위 집계(미션 period_key, 일일 상한)는 `Asia/Seoul` 기준 |
| D5 | 지역 안전지수 정의 | 기획서에 계산식 없음 | **결정(11.9): 시즌 안에 그 권역 운전자들이 낸 운행 리그 점수(D19)의 거리 가중 평균**(0~100). 리그 점수가 없는 운행 · 순위 공개를 끈 운전자(D8)는 빠진다 |
| D6 | 홈(SCR-01) 연승 표시 | 화면은 P0 홈에 "5일 연승"을 보여주지만 연승 계산은 P1 `feat/streak-badge` | **결정(11.8): B) P0 동안 화면에서 연승 카드를 숨김.** `GET /api/missions/today`에 `streakDays`를 넣지 않는다. 연승은 P1 `feat/streak-badge`에서 계산하고 그때 카드를 다시 보여 준다 |
| D7 | 주간 리포트(SCR-07) "퀘스트 수락" 버튼 | plan은 맞춤 미션을 배치로 자동 생성 | A) 수락 API와 미수락 상태 추가 / B) 화면을 "다음 주 퀘스트로 등록됨" 안내로 변경(plan 변경 없음). **결정(11.8): B** — 맞춤 미션은 리포트를 만들 때 다음 주로 자동 등록(`ASSIGNED`), 수락 API 없음 |
| D8 | 설정(SCR-08) "리그 순위 공개" 토글 | 개발 기획서 · plan 어디에도 없음 | **결정(11.9): A) 토글 구현, 기본 공개.** `driver`에 공개 여부 컬럼을 두고 운전자가 바꾸는 API를 추가한다. **순위 공개를 끈 운전자는 순위 목록 · 지역 안전지수 · 시즌 보상에서 빠지고, 참여자 수(10명 기준)에는 남는다(11.9 변경).** 나머지 운전자끼리 순위를 매기므로 한 명이 끈다고 다른 운전자의 순위가 비공개되지 않는다. 끈 운전자는 자기 순위도 볼 수 없다(점수는 본인에게 보임) |
| D9 | "측정 누락 구간" 예외 상태 | 서버는 누락 구간을 알 수 없음 | **뒤로 미룸(11.9).** A) 업로드 요청에 누락 구간 필드 추가 / B) 앱에서만 표시하는 문구로 둠(plan 변경 없음) |
| D10 | 운행 한 건의 참여 조건 | 1km 미만 제외만 있음. 짧은 운행이 고득점에 유리하다는 우려 | **결정(11.3): 최소 거리 3km → 변경(11.9, 앱 요청 10번): 1km**(`trip.minDistanceKm`, 기준값 설정). 3km면 집 근처 짧은 운행이 대부분 빠지므로 앱 MVP 기준에 맞춘다. **최소 회전 수는 두지 않음**: 회전 0회 운행도 참여로 인정하고 깜빡이 항목만 미측정(`NO_TURN`). 고득점 악용은 운행 한 건이 아니라 리그 순위 자격(D13)과 거리 가중 평균으로 막는다 |
| D11 | 리그 단위 | `region_code`(시군구 5자리)를 그대로 쓰면 마포구처럼 작은 리그가 생겨 초기 사용자가 적을 때 비어 버림 | **결정(11.3): 리그 권역 매핑표.** 저장은 시군구 코드 그대로, `시군구 → 리그 권역` 매핑표로 묶는다. 처음에는 크게 묶고(예: 서울은 생활권 5개, 마포는 은평 · 서대문과 서북권) 사용자가 늘면 나눈다. 매핑만 바꾸면 되므로 데이터 이전 없음. 참여자 10명 미만 리그는 순위를 공개하지 않는다 |
| D12 | 리그 소속 기준 | — | **결정(11.3): 등록한 거주 지역.** 위치 정보가 필요 없음(개인정보 요구사항). 시즌 중 지역을 바꾸면 다음 시즌부터 반영해 리그 이동을 막는다 |
| D13 | 리그 순위 자격 | — | **결정(11.3): 시즌 누적 50km 이상 또는 운행 10회 이상**인 운전자만 순위에 오른다(값은 기준값 설정으로 두고 조정). 짧은 고득점 운행 몇 번으로 순위를 올리지 못하게 한다 |
| D14 | 차선 변경 깜빡이 | 회전(방향 60° 이상 변화)만 판정. 차선 변경은 방향 변화가 작아 커브와 구분이 어렵고 GPS로는 차선을 알 수 없음 | **결정(11.3): P0은 회전만.** `feat/trip-upload`에서 `LANE_CHANGE` 이벤트를 받아 저장만 한다. 차량 앱 팀이 조향각 · 자이로 등으로 감지 가능성을 확인한 뒤 P1에서 깜빡이 항목에 반영 |
| D15 | 업로드 요청 형식 | plan은 "기획서 8.2 형식 그대로"인데 기획서가 저장소에 없음 | **결정(11.3): 3.4의 제안 형식으로 구현.** 기획서 8.2와 다르면 DTO만 맞춘다 |
| D16 | 운행 업로드 이벤트 개수 · 요청 크기 상한 | `POST /api/trips`의 `events` 개수와 본문 크기에 제한이 없음. 한 요청으로 수만 건을 보내 DB · 메모리를 소모시킬 수 있음 | **결정(11.9): C) 둘 다.** `events` 최대 2,000개(초과는 400 `INVALID_INPUT`, 값은 기준값 설정)와 요청 본문 1MB 상한. 장거리 운행의 실제 이벤트 수를 앱 팀과 확인해 조정 |
| D17 | 운행 시각 허용 범위 | `startedAt` · `endedAt`이 먼 미래나 아주 오래된 시각이어도 받음. 미래 시각 운행은 일일 미션 · 일일 상한 · 시즌 집계를 왜곡할 수 있음 | **결정(11.9): A) 미래 5분 · 과거 7일.** 서버 시각(`Clock`) 기준 `endedAt`이 현재 + 5분보다 뒤이거나 `startedAt`이 현재 − 7일보다 앞이면 400 `INVALID_INPUT`. 두 값은 기준값 설정에 두고, 과거 상한은 앱의 오프라인 보관 기간에 맞춰 조정 |
| D18 | 점수 항목 확장 | 항목 4개(P0은 깜빡이 1개)라 회전 2~3번으로 점수가 정해지고 만점이 쉬움. 차량 앱 팀이 Pleos SDK 실행 결과로 쓸 수 있는 데이터를 확인함(android-app `docs/sdk-test-log.md`) | **채택(11.8, `feat/score-full-metrics`): 🟢 · 🟡 8개 항목과 앱 문서의 초안 가중치(TURN_SIGNAL 0.25 · SIGNAL_CANCEL 0.05 · TURN_SPEED 0.05 · SMOOTH_DRIVING 0.15 · SEAT_BELT 0.05 · SAFETY_WARNING 0.05 · NIGHT_HEADLIGHT 0.05 · RAIN_DRIVING 0.05, 측정한 항목끼리 재분배).** 원 제안: 11개 항목으로 확장(`POLICY_NOTION.md` 4-1장, android-app `docs/scoring-criteria.md`). 깜빡이 4단계(30m 기준) · 깜빡이 끄기 · 내비 안내 회전 · 제한속도 · 단속 구간 · 보호 구간 감속 · 회전 속도 · 급가감속 · 안전벨트 · 경고등 · 야간 전조등 · 우천 운전. 법규 기준, 거리 정규화, 최소 표본. 내비 데이터 항목(제한속도 · 단속 · 보호 구간 · 내비 회전)은 에뮬레이터에서 값이 안 와 실차 확인 필요 → 4.6으로 분리. 채택 범위 · 가중치는 `feat/score-full-metrics` 착수 전 확정 |
| D19 | 리그 비교 점수 | 차량마다 측정 가능한 항목이 달라, 항목이 적은 차가 리그에서 유리할 수 있음(4.4 "같은 측정 항목 집단끼리 비교") | **결정(11.9): 리그 순위 · 안전지수는 공통 항목만으로 재분배한 리그 점수로**, 운행 점수 · 포인트는 전체 항목 점수로. 공통 항목은 기준값 `league.commonItems`(`TURN_SIGNAL` · `SMOOTH_DRIVING` · `TURN_SPEED`). 공통 항목이 하나도 측정되지 않은 운행은 리그 집계에서 빠진다 |
| D20 | 오토라이트 · 비상등 규칙 | Vehicle SDK 2.3.2에 오토라이트 상태 · 비상등 API가 없음(에뮬레이터 확인) | **채택(11.8): 4.1의 "오토라이트면 100점"과 `NIGHT_SEGMENT.autoLight` 삭제**, 하향등 상태로만 판정. 앱 회전 제외 규칙의 비상등 조건도 적용 불가. 실차에서 경고등으로 비상등이 오면 재검토 |
| D21 | 새 이벤트 종류 · 항목 이름 수용 | `TripEventType` · `ScoreItem`이 enum이라 모르는 값이 오면 `POST /api/trips`가 400. 앱은 4xx를 재전송하지 않고 `REJECTED`로 멈추므로 운행 전체가 버려짐 | **미정.** A) 서버 P1 반영(enum 추가)이 배포된 뒤에만 앱이 새 값을 보냄(기준값 `version`으로 앱이 판단) / B) 모르는 이벤트 종류는 저장만 하고 무시, 모르는 `supported` 이름은 버림. **채택(11.8): A**(서버는 계속 모르는 값을 400으로 거부) |
| D22 | 경기 요약(`summary`) · 경기 기록(`plays`) 정의 | 3.5에 제안만 있고 화면 기획서와 확정 전 | **결정(11.5):** 득점 = 깜빡이를 켠 회전, 실점 = 켜지 않은 회전, 반칙 = 급가감속 · 안전 경고(P0은 점수에 쓰지 않으므로 항상 0). `plays`는 저장된 **모든 이벤트**를 시각순으로 담고, `TURN`만 `result`(`GOAL`/`CONCEDED`)가 있으며 나머지(`LANE_CHANGE` 등)는 `result: null` |
| D23 | `TURN` payload의 `signalOn` 해석 불가 | 업로드는 payload를 해석하지 않고 저장하므로 `signalOn`이 없거나 boolean이 아닐 수 있음 | **결정(11.5): 깜빡이를 켜지 않은 회전으로 판정.** `signalOn`이 `true`일 때만 성공. 업로드(400)로 막지 않는다 |
| D24 | 최근 경기 목록 `limit` | 3.5에서 "착수 시 확정" | **결정(11.5): 기본 10, 최대 50.** 50을 넘으면 50으로 자른다 |
| D25 | 달성하지 못한 날의 미션 상태 · 홈 표시 | 3.6은 "그날이 끝날 때까지 달성하지 못하면 FAILED"만 있고 시점 · 화면 표시가 없음 | **결정(11.8): 끝난 날의 미달성은 `FAILED`.** 배치 없이 판정할 때(업로드 · 조회) 정한다. **오늘 첫 운행 전에는 `GET /api/missions/today`가 마지막 미션 날의 최종 상태(`ACHIEVED` · `FAILED`)를 계속 보여 주고**, 오늘 운행을 올리면 오늘 미션으로 새로 시작한다. 그날 끝난 운행을 늦게 올려(오프라인 후) 조건을 채우면 `FAILED`에서 `ACHIEVED`로 바꾼다. `ACHIEVED`는 되돌리지 않는다 |
| D26 | 연속 기록 · 배지 세부 | 4.3은 "배지 목록 · 조건은 착수 시 확정" | **결정(11.8): 기본 배지 3종** `FIRST_TRIP`(점수가 난 첫 운행), `STREAK_7`(미션 7일 연속 달성, 최장 연속 기준), `SIGNAL_100`(깜빡이를 켠 회전 누적 100회). 카탈로그는 `badges/default-badges.json`(조건 종류 `rule` + 목표 `target`)으로 두어 코드 수정 없이 늘린다. 연속 보상은 기준값 `point.streakRewardDays`(7)일마다 `point.streakRewardPoint`(50P) |
| D27 | 항목별 최소 표본 기본값 | 제안은 회전 3번 미만이면 깜빡이 항목 제외. MVP 앱은 깜빡이 1항목만 보내 회전 1~2번 운행은 점수 · 포인트가 사라짐 | **결정(11.8): 기준값 `minSamples`(항목별)로 두고 기본 1**(P0 동작 그대로). 2차에 앱이 여러 항목을 보내기 시작하면 설정만 3으로 올린다 |
| D28 | 측정할 항목이 하나도 없는 운행의 상태 | 4.1은 "`NO_TURN`을 없애고 이름은 착수 시 확정". 앱은 `SCORED` · `NO_TURN` · `EXCLUDED_SHORT`를 가정 | **결정(11.8): `NO_TURN` 이름 유지**, 뜻을 "측정할 수 있는 항목이 하나도 없음"으로 넓힌다. 회전이 0회여도 다른 항목을 측정하면 `SCORED` |
| D29 | 리그 시즌 주기 | 4.4는 "시즌 4주" | **결정(11.9): 달력 월 단위.** 매월 1일 00:00(Asia/Seoul)에 시작해 다음 달 1일 00:00에 끝난다(시즌 길이 28~31일). 시즌 ID는 `2026-11` 형식 |
| D30 | 시즌 리그 보상 대상 | 4.4는 "상위 지역 참여자 100P" | **결정(11.9): 안전지수 1위 권역의 순위 자격자(D13) 전원.** 순위가 공개된(참여자 10명 이상) 권역만 1위 후보. **보상은 포인트가 아니라 시즌 우승 칭호 + 우승 배지(11.9 변경, D34)** |
| D31 | 초기 리그 권역 매핑(D11) | 매핑표의 첫 묶음 | **결정(11.9): 서울은 생활권 5개(도심권 · 동북권 · 서북권 · 서남권 · 동남권), 나머지는 시 · 도 단위.** 사용자가 늘면 매핑만 바꿔 나눈다 |
| D32 | 주간 맞춤 미션 보상 | 원장 사유가 없어 적립 안 함(4.5) | **결정(11.9): 적립하지 않음(지금 그대로)** |
| D33 | 보상 교환(FR-15)과 포인트의 쓰임 | 5.1은 제휴 쿠폰 교환을 가정 | **결정(11.9): 보상 교환은 보류.** 제휴처가 없어 개발하지 않는다. 게임의 목표는 리그 순위를 올리는 것이고, 포인트는 지금은 적립만 하는 단위다. 제휴가 생기면 5.1을 다시 연다. 포인트의 쓰임은 D34 |
| D34 | 포인트의 쓰임 · 시즌 보상 | 교환할 리워드 상품이 없어 적립한 포인트에 쓸 곳이 없음 | **결정(11.9): 포인트는 리그 순위 점수에 더한다.** 순위 점수 = 리그 점수 거리 가중 평균(0 ~ 100) + 그 시즌 안에 적립한 포인트 합. 지역 안전지수(D5)에는 포인트를 넣지 않는다. 적립 방법(운행 · 오늘의 미션 · 연속 기록)과 일일 상한은 그대로. 시즌 1위 권역 보상(D30)은 포인트 대신 시즌 우승 칭호(`league_champion`) + `SEASON_CHAMPION` 배지 |
| D35 | 내비 데이터 점수 항목(4.6) 착수 시점 | 에뮬레이터에서는 NaviHelper 값이 오지 않음(제한속도 0, 카메라 · 주의 지점 이벤트 없음). 공개 문서로도 에뮬레이터의 경로 안내 시뮬레이션 지원을 확인하지 못함 | **결정(11.10): 실차 확인 후 착수(지금처럼).** 제안 payload로 서버를 먼저 만들지 않는다. 4.6은 추후 개선안으로 남긴다. 앱 팀이 에뮬레이터 모의 주행 · 경로 재생으로 이벤트를 다시 확인하거나 Pleos(42dot)에 테스트 방법을 문의하면 그 결과로 다시 본다 |

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
  └─ feat/score-navi-metrics  (차량 앱 실차 확인 이후, D18)
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
| 7-1 | `feat/score-navi-metrics` | D18 (제안) | P1 | 실차 확인 후. 12.2 전에 확인되지 않으면 이후로 |

FR-02(회전 감지), FR-07(음성 피드백)은 차량 앱 범위라 서버 브랜치가 없습니다. 서버는 판정이 끝난 이벤트만 받습니다.

## 2. 패키지 구조

```
com.blinkerleague.server
├── common      # 공통 응답, 예외 처리, BaseTimeEntity, 시간 유틸
├── security    # SecurityConfig, JWT 인코더 · 디코더 설정
├── auth        # Access · Refresh 토큰 발급 · 재발급 (8장)
├── driver
├── threshold   # 측정 기준값 · 가중치 설정
├── trip        # 운행 · 이벤트 업로드
├── score       # 점수 엔진 (순수 계산 로직)
├── mission
├── point
├── badge       # 연속 기록 · 배지
├── league
└── report
```

**각 도메인 패키지는 헥사고날(포트 · 어댑터) 구조를 씁니다.** 처음 계획한 `controller / service / repository / domain / dto` 구조를 대신합니다. 의존은 항상 안쪽으로(`adapter → application → domain`) 향하고, `HexagonalArchitectureTest`(ArchUnit)가 빌드에서 검사합니다. 자세한 규칙은 CLAUDE.md의 "Architecture" 절을 따릅니다.

```
<domain>
├── domain/                 # 도메인 모델 · 규칙 (JPA 엔티티를 겸할 수 있음)
├── application/
│   ├── port/in/            # 유스케이스 인터페이스 (선택)
│   ├── port/out/           # 외부 기술 인터페이스 (필수)
│   └── service/            # 유스케이스 구현
└── adapter/
    ├── in/web/             # 컨트롤러, 요청 · 응답 DTO
    └── out/<기술>/         # port/out 구현 (persistence, redis, jwt …)
```

---

## 3. P0 (1차 제출, 11.15)

### 3.1 `feat/common-setup` — 공통 기반

작업:
- 위 패키지 구조 생성, `BaseTimeEntity`(`createdAt`, `updatedAt`) + JPA Auditing
- 공통 에러 응답(`{ code, message }`)과 `@RestControllerAdvice`
- `SecurityConfig`: CSRF 비활성화, 세션 STATELESS, `/api/drivers`(POST) · `/api/config/**` · H2 콘솔은 허용, 나머지는 인증 필요 (인증 필터 자체는 D2 확정 후 `feat/driver-register`에서 구현)
- `application.yaml`: JPA `ddl-auto`, 시간대 `Asia/Seoul`, Jackson 날짜를 ISO-8601 오프셋 형식으로
- 프로필 분리: `local`(H2 + 로컬 Redis → 11.10부터 Docker PostgreSQL + Redis), `test`(H2)

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

앱 요청 1 · 7번 반영(11.9): 차량 앱은 회전을 핸들 각도로 감지한다(실차에서 FusedLocation 미지원). `turn`에 `steeringEnterDeg`(90, 이 각도 이상 꺾이면 회전) · `steeringExitDeg`(20, 이 각도를 넘은 때부터 다시 내려올 때까지가 한 번의 조향) · `mergeGapSec`(3, 이어진 같은 방향 조향을 하나로) · `steeringRightPositive`(false, 음수가 오른쪽. 실차 부호가 반대면 이 값만 바꿈)를 두었다. 서버 점수 계산에는 쓰지 않고 앱이 내려받아 쓴다. 진행 방향 감지용이던 `windowSec` · `minAngleDeg` · `uTurnAngleDeg`는 앱이 쓰지 않지만 호환을 위해 남겼다.

상태: 완료. 기본값은 `src/main/resources/thresholds/default-thresholds.json`이고, 서버 시작 시 저장된 값이 없을 때만 version 1로 시드한다(재시작해도 바꾼 값을 덮어쓰지 않음). `app_config` 테이블의 `thresholds` 행에 JSON으로 저장하며, `value`가 H2 예약어라 컬럼 이름은 `config_value`다. 서버 내부는 `ThresholdProvider`(메모리 캐시, 변경 시 갱신)로 읽는다. 변경 값 검증은 완료(11.11, `feat/threshold-validation`): `ThresholdsValidator`가 빠진 그룹, 가중치(음수 금지 · 합 > 0, **합 1은 요구하지 않음** — 측정한 항목끼리 재분배하므로. 11.11 결정), 등급(0~100 · 이름과 기준 점수 중복 금지), 최소 표본 ≥ 1, 비율 0~1, 거리 · 포인트 · 감점 ≥ 0, 개수 · 기간 ≥ 1, 리그 공통 항목 1개 이상을 확인한다. `update`는 위반이면 저장하지 않고 `INVALID_INPUT`, `seedIfAbsent`는 기본값이 위반이면 시작을 멈춘다. 남은 항목: 값 변경용 관리자 API(지금은 `ThresholdService.update`만 있음, D39), 50점 미만 등급 표기(지금은 등급 없음), `TURN_SIGNAL` 외 팁 문구 확정, 서버를 여러 대로 늘릴 때 캐시 갱신 전파.

API: `GET /api/config/thresholds` (앱이 시작할 때 내려받음)

```json
{
  "version": 1,
  "trip": { "minDistanceKm": 1.0, "parkEndMinutes": 3 },
  "turn": { "minSpeedKmh": 5, "windowSec": 10, "minAngleDeg": 60, "uTurnAngleDeg": 150, "signalLeadSec": 5,
            "steeringEnterDeg": 90, "steeringExitDeg": 20, "mergeGapSec": 3, "steeringRightPositive": false },
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

상태: 완료. 응답은 지금 `{ tripId, status }`만 채운다(점수 · 미션 필드는 3.5 · 3.6에서 추가). 새로 저장하면 201, 재전송은 200. 동시 업로드는 `Trip`이 `Persistable`로 INSERT를 강제해 PK 충돌로 감지하고 다시 조회한다(merge로 덮어쓰지 않음). 이벤트 시각은 시작 · 종료 시각을 포함한 구간 안이어야 한다. 한 요청 안에서 `eventId`가 중복되거나 `supported`에 `null`이 있으면 400. 남은 항목: 점수 계산 · 미션 판정 확장 지점은 연결할 대상이 생기는 3.5에서 만든다. 다른 운행의 `event_id`와 충돌하면 지금은 저장 실패(500)로 끝난다(앱이 UUID를 쓰면 사실상 발생하지 않음). **업로드 제한(11.9, `feat/upload-limits`)**: 새 운행만 대상으로 이벤트 `trip.maxEvents`(2,000)개 초과, 종료가 서버 시각 + `trip.maxFutureMinutes`(5분)보다 뒤, 시작이 서버 시각 − `trip.maxPastDays`(7일)보다 앞이면 400 `INVALID_INPUT`(D16 · D17). 이미 저장된 `tripId`의 재전송은 기간이 지나도 200으로 처음 결과를 돌려준다. 요청 본문은 `app.request.max-body-size`(1MB)를 넘으면 413 `PAYLOAD_TOO_LARGE`(`common.web.RequestBodyLimitFilter`, 인증보다 먼저, `Content-Length`가 없는 chunked 본문은 읽으면서 센다).

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
  - `status`: `UPLOADED`(업로드됨, 점수 계산 전), `SCORED`, `NO_TURN`(회전 0회), `EXCLUDED_SHORT`(최소 거리 1km 미만, D10. 11.9에 3km에서 바꿈)
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

테스트: 정상 업로드(`UPLOADED`), 같은 `trip_id` 2회 업로드 시 1건만 저장, 최소 거리(1km, 11.9 이전 3km) 미만은 `EXCLUDED_SHORT`, 다른 운전자의 `trip_id`는 409, 검증 실패는 400.

### 3.5 `feat/trip-score` — 점수 엔진 (FR-03, FR-05)

상태: 완료. 순수 점수 엔진은 `score.domain`(`ScoreCalculator`, `WeightRedistribution`, `MatchReport`)에 있고, `POST /api/trips`가 업로드하면서 점수를 매긴다(별도 배치 없음). 항목별 점수는 `trip_metric`에 저장한다(`value`가 H2 예약어라 컬럼 이름은 `metric_value`). `summary` · `plays` · `turns`는 저장하지 않고 조회할 때 이벤트에서 계산한다. 결정: 경기 요약 · 기록 D22, `signalOn` 해석 불가 D23, 목록 `limit` D24. `limit`이 1 미만이면 400(500 방지). 응답 시간은 회전 300회 운행 업로드가 3초 안에 끝나는지 통합 테스트로 확인(로컬 0.2초). 남은 항목: `mission`은 3.6에서 채운다. 기준값(가중치)이 바뀌어도 이미 저장된 점수는 다시 계산하지 않는다(등급만 조회 시점 기준으로 계산).

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
    { "occurredAt": "2026-11-03T08:12:30+09:00", "type": "TURN", "direction": "LEFT", "result": "GOAL" },
    { "occurredAt": "2026-11-03T08:15:02+09:00", "type": "TURN", "direction": "RIGHT", "result": "CONCEDED" }
  ],
  "mission": { "missionId": 1, "status": "IN_PROGRESS" }
}
```

- `grade`: 3.3 `grades` 기준으로 점수에서 계산(SCR-02). 점수가 없으면 `null`
- `summary`: 경기 비유 집계(SCR-02a). 득점 = 깜빡이를 켠 회전, 실점 = 켜지 않은 회전, 반칙 = 급가감속 · 안전 경고(P0은 0). D22로 결정
- `plays`: 운행 중 모든 이벤트를 시각순으로(SCR-02a). `TURN`의 `result`는 `summary`와 같은 정의, 그 외 이벤트는 `result: null`(D22). `signalOn`을 해석할 수 없는 회전은 실점(D23). `TURN`은 앱이 보낸 회전 방향 `direction`(`LEFT` · `RIGHT`)을 함께 담고, 다른 이벤트는 `direction: null`(앱 요청 8번, 11.9)
- `mission`: 이 운행을 반영한 그날 미션 판정 결과(SCR-02, VOC-02). `feat/daily-mission` 전까지는 `null`

작업:
- `ScoreCalculator`: 입력(운행 + 이벤트 + 지원 신호 + 설정) → 출력(항목별 점수, 적용 가중치, 총점). DB · 스프링에 의존하지 않는 순수 클래스로 만들어 단위 테스트
- 깜빡이 항목: `TURN` 이벤트의 `payload.signalOn`이 true인 비율 × 100 (판정 자체는 앱이 수행)
- **가중치 재분배**: 측정 불가 항목(미지원 신호, 회전 0회 등)을 빼고 남은 가중치를 합이 1이 되도록 비율대로 조정. P0에서는 결과적으로 깜빡이 항목만 가중치 1.0
- `trip_metric` 테이블: `trip_id`, `metric`, `value`, `score`, `weight`
- 회전 0회 → `status=NO_TURN`, `score=null` (앱은 "측정할 회전이 없었어요" 표시). P0은 측정 항목이 깜빡이뿐이라 이렇게 두고, 4.1에서 깜빡이 항목의 제외 사유로 바꾼다
- 남의 운행 조회 시 404
- **최근 경기 목록** `GET /api/trips?limit=2`(SCR-01): 내 운행을 종료 시각 최신순으로. 항목은 `tripId`, `endedAt`, `distanceKm`, `status`, `score`, `grade`. `limit` 기본 10, 최대 50(넘으면 50으로 자름, D24)

테스트: 회전 4회 중 3회 성공 → 75점, 회전 0회 → `NO_TURN`, 재분배 계산(가중치 합 1) 단위 테스트, 점수별 `grade`, `summary` · `plays` 집계, 최근 경기 목록 정렬 · `limit` · 남의 운행 제외. 응답 시간 3초 목표는 통합 테스트에서 대략 확인.

### 3.6 `feat/daily-mission` — 오늘의 미션 (FR-06)

API: `GET /api/missions/today`

상태: 완료(11.8, PR #33). 판정은 라운드 단위(운행 1건 = 1라운드, 그날 하나라도 채우면 `ACHIEVED`)이고, 운행은 종료 시각이 속한 날(Asia/Seoul)에 집계한다. `POST /api/trips`가 저장한 뒤 그날 판정을 `driver_mission`에 기록하고(한 번 달성한 날은 되돌리지 않음, 같은 날 동시 업로드는 유니크 충돌 후 다시 읽어 반영) 응답 `mission`에 `{ missionId, status }`를 담는다. 같은 운행 재전송도 `mission`을 담는다. `GET /api/trips/{tripId}/score`는 기록하지 않고 그날 판정만 계산해 담는다. 회전 집계는 점수와 같은 기준(D23, `signalOn`이 boolean `true`일 때만 켠 회전). 끝난 날의 미달성은 `FAILED`(D25): 배치 없이 판정할 때 정하고, 오늘 첫 운행 전에는 `GET /api/missions/today`가 마지막 미션 날(`driver_mission`의 가장 최근 이전 기록)의 최종 상태를 보여 준다. 늦게 올린 운행으로 그날 조건을 채우면 `FAILED` → `ACHIEVED`. 저장된 `driver_mission.status`는 그날 이후 다시 판정될 때 `FAILED`로 바뀌므로, 지난 날 행이 `IN_PROGRESS`로 남아 있으면 `FAILED`로 읽는다. 달성하면 미션 보상 20P를 `DAILY_MISSION`으로 적립한다(4.2).

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
- 홈 연승 표시(`streakDays`)는 넣지 않음(D6 결정 B, P1 `feat/streak-badge`에서)

작업:
- `mission` 테이블: `mission_id`, `type`, `period`(`DAILY`/`WEEKLY`), `condition`(JSON), `reward_point`, `active_from`, `active_to`. "회전 깜빡이 완벽"(회전 3회 이상, 사용률 100%, 20P) 시드
- `driver_mission` 테이블: `driver_id`, `mission_id`, `period_key`, `status`(`IN_PROGRESS`/`ACHIEVED`/`FAILED`), `achieved_at`. (`driver_id`, `mission_id`, `period_key`) 유니크
- **라운드 단위 판정(11.8 결정):** 시동을 켜고 끄기까지의 운행 1건이 1라운드다. 미션 조건(회전 3회 이상, 사용률 100%)은 라운드 1건 안에서 판정한다. 회전을 하루 동안 합산하지 않는다
- 그날(Asia/Seoul, 운행 **종료 시각** 기준) 라운드 중 **하나라도** 조건을 채우면 그날 미션은 `ACHIEVED`로 확정한다. 이후 실패한 라운드가 있어도 바뀌지 않는다
- 실패한 라운드(깜빡이를 안 켠 회전 포함)가 있어도 그날은 `IN_PROGRESS`로 남아 다음 라운드에서 다시 도전한다. 그날이 끝날 때까지 달성하지 못하면 `FAILED`(`driver_mission` 기록)
- 진행도(`progress`, `conditions`): 달성했으면 달성한 라운드, 아니면 가장 최근 라운드. 라운드가 없으면 0
- `EXCLUDED_SHORT` 운행은 집계에서 제외
- 포인트 지급은 P1(`feat/point-ledger`)에서 연결. 지금은 달성 상태만 기록

테스트: 한 라운드에서 회전 3회 모두 성공 → `ACHIEVED`, 2회만 → `IN_PROGRESS`, 실패 라운드 뒤 달성 라운드 → `ACHIEVED`, 달성 뒤 실패 라운드 → `ACHIEVED` 유지, 달성 라운드 없음 → `IN_PROGRESS`(최근 라운드 진행도), 자정 경계 운행의 period_key, 조건별 진행도(회전 수 · 사용률)와 `rewardPoint` 응답.

### P0 마무리 (11.9 ~ 11.15)
- 차량 앱과 실제 연동 확인(에뮬레이터 시나리오: 좌회전 성공 → 우회전 실패 → 운행 종료 점수 · 미션)
- README에 API 목록 · 실행 방법 갱신 (완료: README API 표 · Swagger 접속 방법, #36 ~ #43)
- ~~`develop` → `main` PR, 제출 버전 태그~~ → **릴리스는 하지 않음(11.8 결정).** 제출은 `develop` 기준

---

## 4. P1 (2차 PT, 12.2)

### 4.1 `feat/score-full-metrics` — 급가감속 · 야간 전조등 · 안전 경고 (FR-08 ~ FR-10)

상태: 완료(11.8, D18 채택 · D20 · D21 A · D27 · D28). 내비 데이터 항목(4.6)은 제외.
- 항목 8개: `TURN_SIGNAL`(4단계: 제때 1 · 늦게 `turn.lateSignalScore` 0.5 · 반대 · 안 켬 0, `signalTiming`이 없으면 `signalOn` 2단계), `SIGNAL_CANCEL`(`100 − signalCancel.penalty` 25 × `SIGNAL_LEFT_ON` 횟수), `TURN_SPEED`(속도를 아는 회전 중 `turn.turnSpeedKmh` 30 이하 비율), `SMOOTH_DRIVING`(`100 − harsh.penaltyPer10Km` 10 × 10km당 급가감속), `SEAT_BELT`(출발 시 미착용 −50 · 주행 중 1분마다 −10, 하한 0), `SAFETY_WARNING`(`mergeWindowSec` 안 중복 1회, `100 − safetyWarning.penaltyPer100Km` 20 × 100km당), `NIGHT_HEADLIGHT`(켜 둔 비율, 오토라이트 규칙 없음), `RAIN_DRIVING`(점등 비율과 감속 준수 비율의 평균, 제한속도를 모르면 점등만)
- `metrics`에 모든 항목을 담는다: `status` `SCORED` / `UNSUPPORTED`(`SIGNAL_NOT_SUPPORTED`) / `NOT_APPLICABLE`(`NO_TURN` · `TOO_FEW_SAMPLES` · `NO_SPEED` · `NO_NIGHT` · `NO_RAIN` · `NO_DATA`), 제외 항목은 `value` · `score` · `weight`가 `null`. `detail`은 근거 수치(예: 깜빡이 `{ turns, onTime, late, missed }`, 급가감속 `{ count, per10Km }`). `trip_metric`에 `status` · `reason` · `detail`(JSON)을 저장
- 회전 0회는 깜빡이 항목의 제외 사유이고 나머지 항목으로 평점. 측정할 항목이 하나도 없으면 운행 `status=NO_TURN`(D28)
- 기준값 추가: `turn.lateSignalScore` · `turnSpeedKmh` · `signalLeadDistanceM`, `harsh.penaltyPer10Km`, `safetyWarning.penaltyPer100Km`, `signalCancel`, `seatBelt`, `rain`, `minSamples`(기본 `TURN_SIGNAL` 1, D27), D18 가중치, 새 항목 `tips`. 이미 저장된 기준값은 시드가 덮어쓰지 않으므로, 운영 DB가 생긴 뒤에는 `ThresholdService.update`로 바꿔야 한다
- 남은 항목: `safetyWarning.telltales`(점수에 셀 경고등 종류)는 실차 확인 후. 미션 · 배지의 회전 판정은 지금도 `signalOn`을 쓰므로 앱이 `signalTiming`과 함께 `signalOn`을 계속 보내야 한다(P0 호환)

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

**점수 항목 확장(D18, 제안) 반영 시 이 브랜치에 추가되는 것** — 에뮬레이터에서 데이터를 확인한 🟢 · 🟡 항목만. 내비 데이터 항목은 4.6.
- 대상 항목: `TURN_SIGNAL` 4단계, `SIGNAL_CANCEL`, `TURN_SPEED`, `SMOOTH_DRIVING`, `SEAT_BELT`, `SAFETY_WARNING`, `NIGHT_HEADLIGHT`, `RAIN_DRIVING`
- 범위가 커지므로 일정(11.23 ~ 11.25)은 착수 전 다시 잡는다. 기존 4개 항목을 먼저 끝내고 새 항목을 하나씩 red-green으로 추가
- `ScoreItem` enum에 `SIGNAL_CANCEL`, `TURN_SPEED`, `SEAT_BELT`, `RAIN_DRIVING` 추가. `supported`는 계속 `ScoreItem` 이름이고, 앱은 항목에 필요한 신호가 모두 있을 때만 그 항목을 넣는다(예: `TURN_SPEED` = 조향각 + 속도)
- `TripEventType`에 `SIGNAL_LEFT_ON`, `SEAT_BELT`, `RAIN_SEGMENT` 추가. 배포 순서는 D21
- `TURN` payload 확장: `{ direction, signalOn, signalTiming, leadDistanceM, maxSpeedKmh }`. `signalTiming`은 `ON_TIME` / `LATE` / `WRONG_DIRECTION` / `NONE`. `signalOn`은 P0 호환용으로 유지하고, `signalTiming`이 없으면 `signalOn`으로 2단계 판정
- 새 payload: `SIGNAL_LEFT_ON { durationSec }`, `SEAT_BELT { buckledAtDeparture, unbuckledMovingSec }`(운행당 1건), `RAIN_SEGMENT { durationSec, headlightOffSec, knownLimitSec, overRainLimitSec }`
- `NIGHT_SEGMENT`: `autoLight` 제거(D20), 오토라이트 100점 규칙 삭제
- 기준값(3.3) 추가: `turn.signalLeadDistanceM`(30), `turn.lateSignalScore`(0.5), `turn.turnSpeedKmh`(30), `signalCancel.forgetSec`(20) · `minSpeedKmh`(30) · `penalty`(25), `seatBelt.departurePenalty`(50) · `perMinutePenalty`(10), `rain.minWiperSec`(30) · `speedRatio`(0.8), `safetyWarning.telltales`(점수에 셀 경고등 종류 목록), 항목별 최소 표본 `minSamples`. `weights`는 D18 확정값으로 교체하고 `version`을 올린다. `Thresholds` record와 `default-thresholds.json`을 함께 바꿈(이미 저장된 값은 시드가 덮어쓰지 않으므로 `ThresholdService.update`로 갱신)
- `tips`에 새 항목 문구 추가
- 최소 표본 미달은 `NOT_APPLICABLE`(`reason: TOO_FEW_SAMPLES`)

테스트(추가): 깜빡이 4단계 점수(제때 1.0 · 늦게 0.5), `signalTiming` 없는 P0 payload 호환, 깜빡이 끄기 횟수별 감점, 회전 속도 비율, 안전벨트 감점 하한 0, 우천 구간에서 제한속도를 모를 때 점등만으로 계산, 최소 표본 미달 `NOT_APPLICABLE`, 새 가중치 합 1과 재분배.

### 4.2 `feat/point-ledger` — 포인트 원장 (FR-12)

API: `GET /api/points` (잔액 + 최근 원장 20건)

상태: 원장과 적립 연결 완료(11.8, #37 · `feat/point-earning`). `point_ledger` 테이블, 운행 적립 계산, 일일 상한(`point.dailyCap`, 기준값 설정), (`reason`, `ref_id`) 한 번만 적립(동시 충돌은 유니크 제약으로 감지), 운전자 행 `SELECT ... FOR UPDATE`로 같은 운전자의 적립 직렬화, `GET /api/points`. 적립 시각은 `earned_at`(Instant)에 두고 응답의 `createdAt`은 `+09:00`. **적립 연결:** `POST /api/trips`가 점수가 난 운행(`SCORED`)을 저장하면 `TRIP` 적립(재전송에도 다시 요청해 첫 적립 실패를 복구, 한 번만 적립), 그날 미션이 `ACHIEVED`가 되면 미션의 `rewardPoint`(20P)를 `DAILY_MISSION`으로 적립(`ref_id = 운전자ID:미션ID:period_key`). 두 적립 모두 일일 상한 안에서다. trip · mission은 각자 out-port → 어댑터 → point의 in-port `EarnPointUseCase`로 부른다. **남은 항목:** P0 기간 달성분 소급 적립 여부(운영 데이터가 생기기 전이라 지금은 해당 없음). 아래 예시의 운행 적립은 공식대로 12.3km × 86점 → 10P로 고쳤다(이전 예시 42P는 공식과 맞지 않았음).

```json
{
  "balance": 1240,
  "todayEarned": 62,
  "dailyCap": 150,
  "entries": [
    { "amount": 10, "reason": "TRIP", "description": { "distanceKm": 12.3, "score": 86 }, "createdAt": "2026-11-03T08:40:00+09:00" },
    { "amount": 20, "reason": "DAILY_MISSION", "description": null, "createdAt": "2026-11-03T08:40:00+09:00" }
  ]
}
```

- `todayEarned` · `dailyCap`: 오늘(Asia/Seoul) 적립 합계와 일일 상한(SCR-06)
- 원장 항목별 설명(거리 × 평점, SCR-06): 적립 시점 값을 보여줘야 하므로 원장에 남긴다(운행을 다시 조회해 계산하지 않음)
- `point_ledger`: `ledger_id`, `driver_id`, `amount`, `reason`(`TRIP`/`DAILY_MISSION`/`STREAK_7`; `SEASON_LEAGUE`는 D34로 없앰), `ref_id`, `distance_km`(nullable), `score`(nullable), `created_at`. (`reason`, `ref_id`) 유니크. `distance_km` · `score`는 `TRIP` 적립 시점 값
- 잔액 = `SUM(amount)` (잔액 컬럼 따로 두지 않음)
- 운행 적립: `floor(min(distanceKm, 50) × score ÷ 100)`, `ref_id = tripId`
- 일간 미션 달성 시 20P(미션의 `rewardPoint`), `ref_id = 운전자ID:미션ID:period_key`(driver_mission 행 ID 대신, 저장 전에도 정해지고 운전자 · 미션 · 날짜마다 하나)
- **일일 상한 150P**: 그날 합계를 구해 남은 만큼만 적립. 같은 운전자의 동시 적립은 운전자 단위 비관적 락(또는 `driver` 행 `SELECT ... FOR UPDATE`)으로 직렬화
- P0에서 이미 달성된 미션에 대한 소급 적립 여부: 뒤로 미룸(11.9)

테스트: 같은 운행 재업로드 시 적립 1회, 상한 초과 시 잘림, 동시 업로드 2건에서 상한 유지, `todayEarned` 자정 경계, 원장 설명에 적립 시점 거리 · 점수.

### 4.3 `feat/streak-badge` — 연속 기록 · 배지 (FR-11)

API: `GET /api/streak`

상태: 완료(11.8, `feat/streak-badge`). D26대로 구현했다.
- `GET /api/streak` → `{ streakDays, todayAchieved }`. 오늘 달성했으면 오늘부터, 아니면 어제부터 거꾸로 센 `driver_mission.status = ACHIEVED` 연속 일수(조회 시 계산, `driver_streak` 캐시 테이블은 두지 않음)
- 연속 보상: 그날 미션이 `ACHIEVED`가 될 때 그날로 끝나는 연속이 `point.streakRewardDays`의 배수면 `STREAK_7`로 `point.streakRewardPoint` 적립(`ref_id = 운전자ID:달성일`, 일일 상한 안)
- `GET /api/badges` → 전체 카탈로그(진열 순서) `[{ code, name, condition, earned, awardedAt }]`. 배지는 운행 업로드 때(적립 · 미션 판정 다음) 누적 기록(점수가 난 운행 수, 최장 연속 달성 일수, 깜빡이 켠 회전 수, D23 기준)으로 판정해 `badge_award`(운전자 · 배지 유니크)에 남긴다
- 남은 항목: 확장 항목 배지(D18)는 `feat/score-full-metrics` 이후 카탈로그에 추가

- 연속 기록 = 오늘(또는 어제)부터 거꾸로 `driver_mission.status = ACHIEVED`인 연속 일수. 조회 시 계산하거나 `driver_streak` 캐시 테이블 유지
- 7일마다 50P (`reason=STREAK_7`, `ref_id = driverId:달성일`)
- `badge_award`: `driver_id`, `badge_code`, `awarded_at`, (`driver_id`, `badge_code`) 유니크. 배지 목록 · 조건(예: 첫 운행, 연속 7일, 깜빡이 100회)은 착수 시 확정
- **전체 배지 카탈로그 반환**(SCR-05 진열장): `GET /api/badges`에서 획득한 배지만이 아니라 전체 배지를 잠금 상태 · 조건과 함께. 예) `{ "code": "STREAK_7", "name": "...", "condition": "7일 연속 미션 달성", "earned": false, "awardedAt": null }`. 배지 정의(이름 · 조건 문구)는 `badge` 카탈로그(시드)에 둔다
- **확장 항목 배지(D18, 제안)**: 항목별 누적 기록 배지를 카탈로그에 추가. 예) 제때 깜빡이 100회(`TURN` `signalTiming=ON_TIME` 누적), 벨트 매고 출발 30회, 단속 구간 50곳 무위반(4.6 이후). 누적 집계는 `trip_metric` · `trip_event`에서 읽음

### 4.4 `feat/region-league` — 지역 리그 (FR-13)

API: `GET /api/leagues/current`, `PUT /api/drivers/me/league-visibility`

상태: 완료(11.9, `feat/region-league`, D5 · D8 · D11 · D12 · D13 · D19 · D29 ~ D31). 포인트 합산 · 우승 칭호는 `feat/league-point-ranking`(D34).
- 리그 점수(D19): 업로드 때 공통 항목(`league.commonItems`)만으로 재분배한 점수를 `trip.league_score`에 저장(`score.domain.LeagueScore`). 공통 항목을 하나도 측정하지 않으면 `null`이고 리그 집계에서 빠진다
- 권역(D11 · D31): `leagues/league-regions.json` 매핑표(서울 생활권 5개 + 시 · 도 16개, 옛 · 새 특별자치도 코드). 시군구 목록이 먼저, 없으면 앞 2자리. 매핑표에 없는 코드는 리그 없음
- 시즌(D29): 한국 시간 달력 월(`LeagueSeason`, id `2026-11`). 소속(D12): 시즌 첫 참여 운행(최소 거리 이상) 업로드 때 그때 등록 지역으로 `league_member`(운전자 · 시즌 유니크)에 고정
- 순위 · 안전지수(`LeagueBoard`, 순수 계산): 순위 점수 = 리그 점수 거리 가중 평균(`averageScore`, 소수 첫째 자리) + 시즌 포인트(`points`, 그 시즌 안에 `earned_at`이 있는 원장 합, D34), 같은 점수는 같은 순위(1, 2, 2, 4). 순위 자격 D13, 리그 점수가 있는 참여자(순위 공개를 끈 운전자 포함) 10명 미만이면 비공개(D11), 순위 공개를 끈 운전자는 순위 · 안전지수 · 시즌 보상에서 제외(D8). 안전지수(D5) = 권역 리그 점수 거리 가중 평균(순위 자격과 무관, 포인트 미포함)
- **Redis ZSET 대신 조회 때 RDB 집계(11.9):** 순위 공개 · 자격 · 비공개 제외 규칙을 한 곳(`LeagueBoard`)에서 계산하려고 `GET /api/leagues/current`가 시즌 기록을 JDBC로 한 번에 읽어 계산한다. 사용자가 늘어 느려지면 결과를 Redis에 캐시한다(9.5)
- 시즌 정산(D30): 매월 1일 00:10(Asia/Seoul) `LeagueSeasonScheduler`가 지난 시즌의 순위 공개 권역 중 안전지수 1위 권역(같으면 모두)의 순위 자격자에게 우승 칭호를 남기고(`league_champion`: 시즌 · 운전자 · 권역, 시즌 · 운전자 유니크) 배지를 판정한다(`SEASON_CHAMPION`, 규칙 `SEASON_CHAMPIONS` 1회, D34). 우승 칭호는 `GET /api/leagues/current`의 `me.championships`. 권역 순위 등 나머지 시즌 결과는 저장하지 않는다(지난 시즌 조회 API가 생길 때 추가)
- 순위 공개 설정(D8): `driver.league_visible`(기본 true), `PUT /api/drivers/me/league-visibility`
- 기준값 `league`: `commonItems`, `minParticipants` 10, `minSeasonDistanceKm` 50, `minSeasonTrips` 10, `topN` 10(`seasonRewardPoint`는 D34로 없앰)

처음 계획에서 바뀐 점(11.9 결정, 기록용):

- 시즌 4주 → 달력 월(D29). 시즌을 행으로 만들지 않고 `LeagueSeason`이 날짜로 계산한다(`league_season` 테이블 없음)
- Redis ZSET 순위 → 조회 때 RDB 집계(위 설명). Redis는 여전히 Refresh 토큰만 저장한다
- 시즌 종료 보상 "상위 지역 참여자 100P" → 안전지수 1위 권역 순위 자격자에게 우승 칭호 + `SEASON_CHAMPION` 배지(D30, D34). 원장 사유 `SEASON_LEAGUE`는 없앰
- 리그 비교 점수(D19), 안전지수(D5), 권역 매핑(D11 · D31), 소속(D12), 순위 자격(D13), 순위 공개 설정(D8)은 위 "상태"대로 결정 · 구현됐다. 매핑은 `league_region_mapping` 테이블이 아니라 `leagues/league-regions.json`이다
- 응답의 지역 목록은 `regions: [{ leagueCode, name, safetyIndex, rank }]`(시군구 `regionCode`가 아니라 리그 권역)

테스트: 순수 계산은 `LeagueBoardTest` · `LeagueSeasonTest` · `LeagueRegionsTest`, 소속 고정은 `JoinSeasonServiceTest`, 시즌 정산은 `SettleSeasonServiceTest` · `LeagueSeasonSchedulerTest`, 저장 · 집계는 H2 어댑터 테스트(`LeagueRecordsAdapterTest` 등), API는 `LeagueControllerTest` · `LeagueVisibilityControllerTest`.

### 4.5 `feat/weekly-report` — 주간 리포트 (FR-14)

API: `GET /api/reports/weekly?week=2026-W45` (기본: 지난주)

상태: 완료(11.8, `feat/weekly-report`, D7 B).
- 한 주는 ISO 주(월요일 시작, Asia/Seoul), `week`는 `2026-W45` 형식. 형식이 틀리거나 끝나지 않은 주는 400 `INVALID_INPUT`
- `metrics`: 항목별 **거리 가중 평균 점수(0~100)**의 이번 주 · 지난주 · 변화(`thisWeek` · `lastWeek` · `change`, 그 주에 측정하지 않았으면 `null`). 아래 예시의 원래 단위(사용률 · 10km당 횟수 등)는 항목마다 달라 "가장 약한 항목"을 고를 수 없으므로 점수로 통일했다. 응답에 `trips` · `distanceKm`(최소 거리 미만 제외)도 담는다
- `weakestMetric`: 이번 주 평균 점수가 가장 낮은 항목(동점이면 항목 순서가 앞선 것)
- 맞춤 미션(`weekly_mission`): `reports/weekly-missions.json`의 항목별 틀(제목 · 목표 점수 · 최소 주간 거리)로 다음 주에 자동 등록(D7 B). 그 주가 끝나 다음 리포트를 만들 때 "해당 항목 평균 ≥ 목표, 주간 거리 ≥ 최소"면 `ACHIEVED`, 아니면 `FAILED`로 판정해 `lastMission`에 담는다. "안전 경고 없는 주"는 경고등 평균 100 · 50km 이상. 미션 테이블(`mission`, 일일 미션)과 따로 두었고, 포인트 원장 사유(4.2)에 주간 미션이 없어 **적립은 하지 않는다**
- 생성 · 저장: 매주 월요일 04:00(Asia/Seoul) 배치가 지난주에 운행한 운전자의 리포트를 만들어 `weekly_report`에 JSON으로 저장. 배치에서 빠진 운전자는 조회할 때 만들어 저장한다. 한 번 만든 리포트는 다시 계산하지 않는다
- 남은 항목: 주간 미션 보상 포인트(원하면 원장 사유 추가), 리그(4.4)와 함께 쓸 "지역 평균 대비" 비교

- 항목별 주간 평균(깜빡이 사용률, 10km당 급가감속, 100km당 경고, 전조등)과 전주 대비 변화
- 가장 약한 항목을 골라 다음 주 "맞춤 개선 미션"(주간 미션) 생성
- 매주 월요일 새벽 배치로 생성 · 저장(`weekly_report`), 조회는 저장본 반환
- 주간 미션 "안전 경고 없는 주"(50km 이상, 경고 0회)도 여기서 함께 판정
- **확장 항목(D18, 제안)**: 항목별 주간 평균과 "가장 약한 항목"을 `supported`로 측정된 모든 항목에서 고른다. 맞춤 미션 후보: "회전 5번 모두 30m 전 깜빡이", "벨트 매고 출발 7일", "깜빡이 끄기 잊지 않기", "비 오는 날 전조등", "어린이보호구역 모두 30km 이하"(4.6 이후). 미션 `condition` JSON에 항목 이름과 목표값을 넣어 코드 변경 없이 추가
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

### 4.6 `feat/score-navi-metrics` — 내비 데이터 점수 항목 (D18, 제안)

상태: **추후 개선(11.10, D35).** 실차에서 데이터를 확인하기 전에는 착수하지 않는다.

차량 앱이 NaviHelper SDK로 받는 데이터로 매기는 항목. **에뮬레이터에서는 값이 오지 않아(제한속도 항상 0, 카메라 · 주의 지점 이벤트 없음) 차량 앱 팀의 실차 확인 후 착수**한다. 확인되지 않으면 이 브랜치는 하지 않고, 앱이 `supported`에 넣지 않으므로 점수에서 자동 제외된다.

- `ScoreItem` 추가: `NAVI_TURN_SIGNAL`, `SPEED_LIMIT`, `ENFORCEMENT`, `CAUTION_ZONE`
- `TripEventType` 추가와 payload (좌표 없음):
  - `NAVI_TURN { turnType, signalOnTime }`
  - `SPEEDING_SUMMARY { knownLimitSec, overLimitSec }` (운행당 1건)
  - `CAMERA_PASS { cameraType, overSpeed, overSectionAvrSpeed }` — 과속 판정은 내비가 한 값을 그대로 씀
  - `CAUTION_ZONE { zoneType, maxSpeedKmh, limitKmh }` — `SCHOOL_ZONE`, `SPEED_BUMP` 등
- 점수:
  - `SPEED_LIMIT`: `max(0, 100 − 200 × overLimitSec ÷ knownLimitSec)`. `knownLimitSec`가 최소 표본(예: 300초) 미만이면 `NOT_APPLICABLE`
  - `ENFORCEMENT`: 준수 통과 수 ÷ 전체 × 100
  - `CAUTION_ZONE`: 준수 구간 비율 × 100, 어린이보호구역 위반은 2배 가중
  - `NAVI_TURN_SIGNAL`: 성공 비율 × 100
- 기준값 추가: `speedLimit.toleranceKmh`(10), `speedLimit.penaltyFactor`(200), `caution.schoolZoneKmh`(30), `caution.bumpKmh`(30), `caution.schoolZoneWeight`(2)
- 의존: `feat/score-full-metrics` 이후, 차량 앱 실차 확인 이후

테스트: 제한속도 초과 비율별 점수, 표본 부족 `NOT_APPLICABLE`, 카메라 통과 준수율, 어린이보호구역 2배 가중, 4개 항목이 `supported`에 없을 때 재분배.

## 5. P2

### 5.1 `feat/reward-exchange` — 보상 교환 (FR-15)

상태: **보류(11.9, D33).** 제휴처가 없어 개발하지 않는다. 포인트는 지금은 적립만 하고 사용처는 미정이다. 게임성은 순위를 올리는 데 둔다. 제휴가 생기면 아래 범위로 다시 연다.

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
| 점수 항목 확장 채택 범위 · 가중치 (D18) | `feat/score-full-metrics` 착수 전 |
| 확장 항목 payload(`TURN` 확장, `SIGNAL_LEFT_ON`, `SEAT_BELT`, `RAIN_SEGMENT`) | `feat/score-full-metrics` 착수 전 |
| 새 이벤트 · 항목 이름을 앱이 보내기 시작하는 시점 (D21) | 서버 P1 배포 전 |
| 차량 속도 권한 `android.car.permission.CAR_SPEED`(dangerous)의 Pleos 심사 통과 여부. 거부되면 속도가 필요한 항목 전부 미지원 | `feat/score-full-metrics` 착수 전 |
| 실차 확인: NaviHelper 제한속도 · 단속 카메라 · 주의 지점, 안전벨트 · 와이퍼 변경 콜백, 경고등 종류 | `feat/score-navi-metrics` 착수 전 (4.6) |

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

---

## 9. 다음에 할 일 (2026-11-10 기준)

### 9.1 지금 상태

- **P0(1차 제출 11.15)**: 6개 feat 모두 완료(#16 · #20 · #24 · #38 · #33, 공통 기반 #5, 계획에 없던 인증 #14 · Swagger #36). 남은 것은 차량 앱과의 연동 확인뿐이다. 릴리스(`develop` → `main`)는 하지 않는다
- **P1(2차 PT 12.2)**: 5개 모두 완료 — 4.1 점수 항목 확장(#41), 4.2 포인트(#37 · #39), 4.3 연속 기록 · 배지(#40), 4.5 주간 리포트(#42), 4.4 지역 리그(#48, 포인트 순위 합산 · 우승 칭호 #49)
- **차량 앱 요청**(android-app `docs/server-requests.md`): 1 · 7 · 8번 반영(#43), 3 · 6 · 9번 반영, 2번 철회, 10번(최소 거리 1km) 반영(#46). 업로드 제한(D16 · D17) 반영(#47)
- **11.10**: 내비 점수 항목(4.6)을 추후 개선으로 결정(D35, #50). 앱 팀 전달용 변경 정리 `docs/app-team-changes-2026-11-10.md`(#51). 역할별 서브에이전트(`.claude/agents/`: lead · tester · developer · reviewer)와 테스트 given / when / then 규칙 추가
- **11.11**: 기준값 변경 검증(`feat/threshold-validation`, 3.3)
- `develop` 테스트 364개 통과, 공개 API 13개(README API 표 · Swagger, `docs/api/openapi.json`과 실행 서버 명세 일치 확인 11.10)

### 9.2 결정 대기 항목 (11.9 정리)

11.9에 대부분 결정했다. 남은 것은 뒤로 미룬 항목이다.

| 순서 | 항목 | 상태 |
| --- | --- | --- |
| 1 | 앱 요청 10번(최소 거리) | **결정(11.9): 1km** → `feat/min-distance-1km` |
| 2 | D19 리그 비교 점수 | **결정(11.9): 공통 항목 리그 점수** |
| 3 | D5 지역 안전지수 | **결정(11.9): 리그 점수 거리 가중 평균** |
| 4 | D11 초기 리그 권역 매핑 | **결정(11.9, D31): 서울 생활권 5개 + 시 · 도** |
| 5 | D8 "리그 순위 공개" 토글 | **결정(11.9): A 토글 구현, 기본 공개, 끈 운전자는 순위 · 안전지수 · 시즌 보상에서 제외(참여자 수에는 남음)** |
| 6 | D16 업로드 상한 | **결정(11.9): 이벤트 2,000개 · 본문 1MB** |
| 7 | D17 운행 시각 허용 범위 | **결정(11.9): 미래 5분 · 과거 7일** |
| 8 | D9 측정 누락 구간 | **뒤로 미룸(11.9).** 서버 작업 없음 |
| 9 | 주간 미션 보상 | **결정(11.9, D32): 적립 안 함** |
| 10 | P0 달성분 소급 적립 | **뒤로 미룸(11.9).** 운영 데이터가 생기기 전이라 해당 없음 |
| 11 | D3 운영 DB | **결정(11.9): PostgreSQL + Flyway**(배포 준비 때 작업) |
| 12 | 포인트 사용처 | **결정(11.9, D34): 리그 순위 점수에 더함.** 보상 교환은 보류(D33). 시즌 보상도 포인트 대신 우승 칭호 · 배지 → `feat/league-point-ranking` |

### 9.3 개발할 것 (우선순위)

1. ~~**앱 요청 10번 반영**~~ → 완료(#46). 운영 DB가 생긴 뒤라면 `ThresholdService.update`로 갱신
2. **P0 마무리 · 차량 앱 연동 확인** (~11.15): 에뮬레이터 시나리오(좌회전 성공 → 우회전 실패 → 운행 종료 점수 · 미션 · 포인트). 앱 팀에 전달할 변경은 `docs/app-team-changes-2026-11-10.md`로 정리함(#51), 아래는 그 요약
   - 응답 변경: `metrics`의 `status` · `reason` · `detail`, `plays`의 `direction`, `mission`, 기준값의 새 그룹(`signalCancel` · `seatBelt` · `rain` · `minSamples`) · `turn` 핸들 각도 값
   - 새 이벤트 · 항목 이름(`signalTiming`, `SIGNAL_LEFT_ON` · `SEAT_BELT` · `RAIN_SEGMENT`)은 서버 배포 후에만 보낸다(D21 A). 미션 · 배지는 지금도 `signalOn`을 쓰므로 계속 함께 보낸다
   - 새 API: `/api/points` · `/api/streak` · `/api/badges` · `/api/reports/weekly` · `/api/leagues/current` · `PUT /api/drivers/me/league-visibility`
   - 11.9 변경: 최소 거리 1km, 업로드 제한(이벤트 2,000개 · 본문 1MB → 400/413, 운행 시각 미래 5분 · 과거 7일 → 400), 리그 순위 점수에 포인트 합산(D34)
3. ~~**4.4 `feat/region-league`**~~ → 완료(11.9, #48 · #49): 리그 권역 매핑표(D31), 달력 월 시즌(D29) · 소속 고정(D12), 리그 점수(D19), 순위 자격(D13) · 10명 미만 미공개, 조회 때 RDB 집계, 순위 점수에 시즌 포인트 합산(D34), 시즌 정산(우승 칭호 + `SEASON_CHAMPION` 배지, D30 · D34), `GET /api/leagues/current`, `PUT /api/drivers/me/league-visibility`
4. **4.6 `feat/score-navi-metrics`** — **추후 개선(D35)**: 제한속도 · 단속 구간 · 보호 구간 · 내비 회전 항목. 실차에서 NaviHelper 이벤트가 오는지 확인한 뒤 착수(7장)
5. ~~**5.1 `feat/reward-exchange`**~~ **보류(D33)**: 제휴처가 없어 개발하지 않음. 포인트는 리그 순위 점수에 더한다(D34)
6. **배포 준비(9.4): 보류(11.10, D36).** 운영 DB(D3)는 `feat/postgres-flyway`로 로컬까지 완료. 배포 위치 · 운영 프로필 · 인프라는 사용자가 다시 열 때 진행(`docs/deployment.md` 7 · 8장). 기존 메모: 남은 feat는 4.6(실차 확인 대기)과 5.1(보류)뿐이라, 이미 결정된 운영 DB(D3, PostgreSQL + Flyway)부터 착수하는 것을 권장

### 9.4 배포 준비 (서버를 실제로 띄우기 전) — 보류(11.10)

설계와 결정이 필요한 항목(D36 ~ D40, 제안)은 [`docs/deployment.md`](docs/deployment.md)에 정리했다.

- **HTTPS**: TLS 1.2 이상(1.3 권장), 공인 인증서 · 도메인, TLS 종단 위치(로드밸런서 또는 `server.ssl.*`)를 이 문서에 기록하고 프로토콜 버전 확인 테스트 추가(CLAUDE.md Pleos 보안 가이드)
- ~~**운영 DB**(D3)와 스키마 마이그레이션~~ → 완료(11.10, `feat/postgres-flyway`): Flyway `V1__init.sql`, `local`은 Docker PostgreSQL + `validate`. 운영 프로필(`prod`)은 `docs/deployment.md` 7장 2번
- **secret**: 환경변수 → Vault(Spring Cloud Vault)
- **기준값 갱신 경로**: 저장된 기준값은 시드가 덮어쓰지 않으므로 `ThresholdService.update`를 부를 관리자 API 또는 운영 절차가 필요
- **배치 다중 서버**: 월요일 04:00 주간 리포트 배치(4.5)는 서버 1대 전제. 여러 대면 ShedLock 등 분산 잠금
- ~~**업로드 제한**(D16 · D17) 적용~~ → 완료(11.9, `feat/upload-limits`)

### 9.5 알려진 제약 · 정리할 것

| 항목 | 내용 | 시점 |
| --- | --- | --- |
| 업로드 비트랜잭션 | 운행 저장 · 적립 · 미션 기록 · 배지가 따로라 중간 실패 시 500, 같은 `tripId` 재전송으로 복구(모두 한 번만 반영) | 필요 시 |
| 집계 비용 | 연속 기록 · 배지 · 주간 리포트 · 지역 리그가 조회 · 판정마다 전체 기록을 집계(리그는 Redis ZSET 대신 조회 때 RDB 집계, 4.4) | 기록이 많아지면 캐시 · 누적 카운터 |
| 경고등 종류 | `safetyWarning.telltales`(점수에 셀 경고등) 미정 | 실차 확인 후 |
| 최소 표본 | `minSamples` 기본 1(D27). 앱이 여러 항목을 보내기 시작하면 3으로 올림 | 2차 앱 배포 시 |
| 다른 운행과 `event_id` 충돌 | 지금은 500(앱이 UUID를 쓰면 사실상 없음) | 필요 시 |
| 토큰 분실 복구 | 로그인이 없어 앱 삭제 · 기기 변경 시 재등록 | 7장, 결정 필요 |
| 감사 시각 타입 | `BaseTimeEntity`가 `LocalDateTime`이라 응답에 내보내면 오프셋 없음(지금은 안 내보냄) | 응답에 넣을 때 |
| Gradle · Boot 버전 고정 | IntelliJ 2021.3.1 때문에 Gradle 7.6.4 · Boot 3.3 · springdoc 2.6 | IDE 업데이트 후 |
