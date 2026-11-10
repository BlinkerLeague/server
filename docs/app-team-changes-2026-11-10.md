# 차량 앱 팀 전달: 서버 변경 사항 (2026-11-10 기준)

11.9 ~ 11.10에 서버 `develop`에 머지된 변경(#46 ~ #50)입니다. 아직 배포 전입니다. 요청 · 응답의 정확한 형식은 Swagger(`docs/api/openapi.json`)가 기준입니다.

## 한눈에 보기

| 변경 | 앱이 할 일 |
| --- | --- |
| 최소 운행 거리 3km → **1km** | 1km 미만 운행은 계속 `EXCLUDED_SHORT`로 처리 |
| 업로드 제한: 이벤트 2,000개, 본문 1MB, 운행 시각 범위 | 400 · 413을 받으면 같은 요청을 재시도하지 않기 |
| 새 API: 지역 리그 조회, 순위 공개 설정 | 리그 화면(SCR-04), 설정 화면(SCR-08) 연결 |
| 리그 순위 점수 = 평균 점수 + 이번 시즌 포인트 | 순위 화면에 `averageScore` · `points` · `score` 표시 |
| 시즌 1위 보상 = 우승 칭호 + "시즌 우승" 배지 (포인트 아님) | 칭호 목록 표시, 배지 진열장이 4종으로 늘어남 |
| 기준값 응답에 `trip` 제한값과 `league` 그룹 추가 | 앱이 쓰는 값만 반영 |

## 1. 운행 업로드 (`POST /api/trips`)

- **최소 거리 1km:** 1km 미만은 `EXCLUDED_SHORT`입니다. 점수, 포인트, 미션, 리그에서 모두 빠집니다. 정확히 1.0km부터 참여합니다.
- **400 `INVALID_INPUT`:** 아래 조건은 새 운행일 때만 검사합니다.
  - `events`가 2,000개를 넘을 때
  - `endedAt`이 서버 시각보다 5분 넘게 미래일 때
  - `startedAt`이 서버 시각보다 7일 넘게 지났을 때
  - 시각은 서버 기준입니다. 기기 시계가 틀리면 400이 날 수 있습니다.
- **413 `PAYLOAD_TOO_LARGE`:** 요청 본문이 1MB를 넘을 때입니다. 길이를 미리 알 수 없는 chunked 전송도 포함합니다.
- **재전송:** 이미 저장된 `tripId`를 다시 보내면 7일이 지났어도 200과 처음 결과를 돌려줍니다. 네트워크 실패 뒤의 재전송은 안전합니다.
- **권장:** 400과 413은 다시 보내도 결과가 같습니다. 재시도 큐에서 빼 주세요. 5xx와 네트워크 오류만 같은 `tripId`로 재시도하면 됩니다.

## 2. 지역 리그 API

### `GET /api/leagues/current` (Bearer 토큰 필요)

```json
{
  "season": { "seasonId": "2026-11", "startsAt": "2026-11-01T00:00:00+09:00", "endsAt": "2026-12-01T00:00:00+09:00" },
  "league": { "leagueCode": "BUSAN", "name": "부산", "participants": 10, "rankingPublished": true,
              "safetyIndex": 90.0, "rank": 1,
              "rankings": [{ "rank": 1, "nickname": "...", "averageScore": 100.0, "points": 50, "score": 150.0 }] },
  "me": { "visible": true, "eligible": true, "averageScore": 100.0, "points": 50, "score": 150.0,
          "rank": 1, "trips": 1, "distanceKm": 60.0,
          "championships": [{ "seasonId": "2026-10", "leagueCode": "BUSAN", "name": "부산" }] },
  "regions": [{ "leagueCode": "BUSAN", "name": "부산", "safetyIndex": 90.0, "rank": 1 }]
}
```

- **시즌:** 한국 시간 달력 월 단위입니다. D-day는 `endsAt`으로 계산하면 됩니다.
- **권역:** 서울은 생활권 5개, 나머지는 시 · 도 16개입니다. 등록 지역 코드가 매핑표에 없으면 `league`가 `null`입니다.
- **순위 자격과 공개:** 시즌 누적 50km 이상이거나 운행 10회 이상이어야 순위에 오릅니다. 참여자가 10명 미만인 권역은 `rankingPublished: false`이고 `rankings`가 빈 목록입니다.
- **`rankings`:** 상위 10명이고 닉네임만 담깁니다. 같은 점수는 같은 순위입니다(1, 2, 2, 4).
- **`regions`:** 월드맵용입니다. 리그 점수가 없는 권역은 `safetyIndex`와 `rank`가 `null`입니다.

### `PUT /api/drivers/me/league-visibility`

- 요청과 응답 모두 `{ "visible": false }` 형식입니다. 기본값은 공개입니다.
- 오류: `visible`이 없으면 400, 토큰이 없으면 401, 운전자가 없으면 404입니다.
- 공개를 끈 운전자는 순위 목록, 지역 안전지수, 시즌 우승에서 빠집니다. 본인 순위(`me.rank`)도 `null`이지만, 본인 점수는 보입니다.
- 공개를 끈 운전자도 참여자 수(10명 기준)에는 들어갑니다. 그래서 한 명이 꺼도 다른 운전자의 순위는 계속 공개됩니다.

## 3. 포인트, 시즌 우승, 배지

- **포인트 적립:** 운행, 오늘의 미션, 7일 연속 보상과 하루 상한 150P는 그대로입니다.
- **포인트의 쓰임:** 교환할 상품이 없어서, 이번 시즌 안에 적립한 포인트가 리그 순위 점수에 더해집니다. 잔액은 줄지 않습니다.
- **지역 안전지수:** 포인트를 넣지 않고 리그 점수만으로 계산합니다.
- **시즌 정산:** 매월 1일 00:10에 합니다. 지난 시즌 안전지수 1위 권역에서 순위 자격을 채운 운전자에게 우승 칭호를 남기고 `SEASON_CHAMPION` 배지를 줍니다. 칭호는 `me.championships`에 시즌마다 쌓입니다.
- **`GET /api/badges`:** `FIRST_TRIP`, `STREAK_7`, `SIGNAL_100`, `SEASON_CHAMPION` 4종입니다.

## 4. 기준값과 미션 판정

- **`GET /api/config/thresholds`의 `trip`:** `minDistanceKm` 1.0, `maxEvents` 2000, `maxFutureMinutes` 5, `maxPastDays` 7.
- **새 `league` 그룹:** `commonItems`(`TURN_SIGNAL`, `SMOOTH_DRIVING`, `TURN_SPEED`), `minParticipants` 10, `minSeasonDistanceKm` 50, `minSeasonTrips` 10, `topN` 10.
- **업로드 응답의 `mission`:** 운행이 끝난 날의 미션 결과입니다.
  - 이미 지난 날인데 조건을 못 채웠으면 `FAILED`입니다.
  - 지난 날의 운행을 늦게 올려 조건을 채우면 그날이 `ACHIEVED`로 바뀝니다(7일 이내).

## 5. 이전 P1 변경 (아직 반영하지 않았다면)

- 운행 결과의 `metrics`에 `status`, `reason`, `detail`이 추가됐고, `plays`에 `direction`, 결과에 `mission`이 추가됐습니다.
- 기준값에 `signalCancel`, `seatBelt`, `rain`, `minSamples` 그룹과 `turn` 핸들 각도 값이 추가됐습니다.
- 새 이벤트 · 항목 이름(`signalTiming`, `SIGNAL_LEFT_ON`, `SEAT_BELT`, `RAIN_SEGMENT`)은 서버 배포 후에만 보내 주세요. 미션과 배지는 아직 `signalOn`을 쓰므로 계속 함께 보내 주세요.
- 새 API: `/api/points`, `/api/streak`, `/api/badges`, `/api/reports/weekly`

## 6. 앱 팀에 확인 요청

1. **내비 데이터(D35):** 에뮬레이터에서 내장 내비의 모의 주행이나 GPS 경로 재생을 켠 상태로, NaviHelper의 제한속도, 단속 카메라, 주의 지점 이벤트가 오는지 다시 확인해 주세요. 안 오면 Pleos(42dot)에 테스트 방법을 문의해 주세요. 확인되기 전까지 내비 점수 항목은 만들지 않습니다.
2. **재시도 처리:** 400과 413을 재시도 큐에서 빼는지 확인해 주세요.
3. **기존 미결 사항:** 차량 속도 권한(`CAR_SPEED`) 심사 결과, 안전 경고 점수에 셀 경고등 종류.
