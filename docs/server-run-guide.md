# 서버 실행 가이드

깜빡이리그 서버를 내 PC에서 띄우고, 운전자 등록부터 운행 점수 확인까지 직접 호출해 보는 순서입니다. 작성 2026-11-10.

- **지금(`develop`) 기준**: DB는 H2 인메모리, Redis만 Docker로 띄웁니다(1 ~ 6장).
- **`feat/postgres-flyway` 머지 후**: DB도 Docker PostgreSQL로 바뀝니다. 바뀌는 부분은 7장에 모았고, 설계는 [`docs/deployment.md`](deployment.md) 9장입니다.
- 배포 환경(HTTPS · 운영 DB) 실행은 아직 없습니다. 계획은 `docs/deployment.md` 3 · 5 · 8장입니다.
- API 요청 · 응답 형식은 Swagger가 기준입니다(README "API 명세 관리 규칙"). 이 문서는 실행 순서만 다룹니다.

## 1. 준비물

| 준비물 | 버전 · 확인 | 비고 |
| --- | --- | --- |
| JDK | **17** (`java -version`) | Gradle 툴체인이 17을 찾습니다. 다른 버전만 있으면 빌드가 실패하니 17을 설치하세요. IntelliJ는 Settings → Build Tools → Gradle → Gradle JVM을 17로 |
| Docker | Docker Desktop(`docker --version`, `docker compose version`) | Redis와 Testcontainers 테스트에 필요. 설치는 README "Docker 설정" |
| Git | — | — |
| (선택) IntelliJ IDEA | 2021.3.1 기준 | `http/*.http` 수동 테스트 실행용. Gradle · Boot 버전을 올리지 마세요(CLAUDE.md) |
| (선택) `jq` | `brew install jq` | 아래 curl 예시와 `openapi.json` 재생성에 사용 |

## 2. 처음 한 번

```bash
git clone https://github.com/BlinkerLeague/server.git
cd server
git switch develop

cp .env.example .env
chmod 600 .env
openssl rand -base64 48        # 출력값을 .env의 JWT_SECRET= 뒤에 붙여 넣기
```

- `.env`는 git에 올라가지 않습니다. 값을 메신저나 문서에 붙여 넣지 마세요.
- `JWT_SECRET`이 없거나, `${...}` 그대로이거나, 32바이트보다 짧으면 서버가 시작되지 않습니다(의도된 동작).

## 3. 실행

```bash
docker compose up -d           # Redis 시작
docker compose ps              # STATUS가 healthy인지 확인
./gradlew bootRun              # 서버 시작(프로필 지정이 없으면 local)
```

- 로그에 `Started ServerApplication`이 나오면 준비 완료입니다. 기본 포트는 8080입니다.
- 시작할 때 기준값 · 오늘의 미션 · 배지 카탈로그가 자동으로 들어갑니다(시드).
- 로그는 콘솔과 `logs/server.log`(7일 보관)에 남습니다.
- IntelliJ에서는 `ServerApplication`의 실행 버튼으로 띄워도 됩니다. 작업 디렉터리가 프로젝트 루트여야 `.env`를 읽습니다.

| 할 일 | 명령 |
| --- | --- |
| 서버 중지 | 터미널에서 `Ctrl + C` |
| Redis 중지(데이터 유지) | `docker compose stop` |
| Redis 삭제(토큰까지 삭제) | `docker compose down -v` |
| 다른 포트로 실행 | `SERVER_PORT=8081 ./gradlew bootRun` |
| 빌드 + 전체 테스트 | `./gradlew build` (Docker 엔진이 켜져 있어야 함) |
| 테스트 하나 | `./gradlew test --tests "com.blinkerleague.server.ServerApplicationTests"` |

## 4. 동작 확인 (처음 띄웠을 때 순서)

### 4.1 Swagger로 확인

1. <http://localhost:8080/swagger-ui.html>을 엽니다(`local`에서만 열림).
2. `POST /api/drivers` → `Try it out` → `{ "nickname": "테스트01", "regionCode": "11680" }` → `Execute`.
3. 응답의 `accessToken`을 복사해 오른쪽 위 `Authorize`의 `bearerAuth`에 붙여 넣습니다(`Bearer ` 없이).
4. 자물쇠가 있는 API(`POST /api/trips`, `GET /api/missions/today` 등)를 호출합니다.

자세한 화면 순서는 README "Swagger 접속 방법"에 있습니다.

### 4.2 터미널(curl)로 확인

macOS 기준입니다(Linux는 `date -v-1d` 대신 `date -d yesterday`).

```bash
HOST=http://localhost:8080

# 1) 운전자 등록 → accessToken
TOKEN=$(curl -s -X POST $HOST/api/drivers -H 'Content-Type: application/json' \
  -d "{\"nickname\":\"curl$RANDOM\",\"regionCode\":\"11680\"}" | jq -r .accessToken)

# 2) 어제 운행 업로드(운행 시각은 과거 7일 · 미래 5분 안이어야 함, D17)
D=$(date -v-1d +%F)
TRIP=$(uuidgen)
curl -s -X POST $HOST/api/trips -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "{
  \"tripId\": \"$TRIP\",
  \"startedAt\": \"${D}T08:00:00+09:00\", \"endedAt\": \"${D}T08:40:00+09:00\",
  \"distanceKm\": 12.3, \"supported\": [\"TURN_SIGNAL\"],
  \"events\": [
    {\"eventId\": \"$(uuidgen)\", \"type\": \"TURN\", \"occurredAt\": \"${D}T08:12:30+09:00\", \"payload\": {\"direction\": \"LEFT\", \"signalOn\": true}},
    {\"eventId\": \"$(uuidgen)\", \"type\": \"TURN\", \"occurredAt\": \"${D}T08:20:00+09:00\", \"payload\": {\"direction\": \"RIGHT\", \"signalOn\": false}}
  ]}" | jq '{status, score, grade}'

# 3) 점수 · 포인트 · 오늘의 미션
curl -s $HOST/api/trips/$TRIP/score -H "Authorization: Bearer $TOKEN" | jq '{status, score, summary}'
curl -s $HOST/api/points           -H "Authorization: Bearer $TOKEN" | jq '{balance, todayEarned}'
curl -s $HOST/api/missions/today   -H "Authorization: Bearer $TOKEN" | jq '{status, progress}'
```

기대 결과: 업로드는 201과 `SCORED`, 같은 `tripId`로 다시 보내면 200과 같은 결과입니다.

### 4.3 IntelliJ HTTP Client로 시나리오 실행

`http/` 폴더의 파일을 열고 환경을 `local`로 고른 뒤 위에서부터 순서대로 실행합니다. 앞 요청에서 받은 토큰 · ID를 뒤 요청이 이어받습니다.

| 파일 | 확인하는 것 |
| --- | --- |
| `http/driver-register.http` | 등록, 닉네임 중복 409, 입력 오류 400, 토큰 없음 · 위조 401, 토큰 재발급과 재사용 차단 |
| `http/trip-upload.http` | 업로드, 재전송, 최소 거리(1km), 회전 없는 운행, 업로드 상한(D16 · D17), 미션 결과 |
| `http/daily-mission.http` | 오늘의 미션 진행 · 달성 · 실패 |
| `http/region-league.http` | 지역 리그 조회, 순위 공개 토글 |

### 4.4 저장된 데이터 보기 (H2 콘솔)

- <http://localhost:8080/h2-console>
- JDBC URL `jdbc:h2:mem:blinkerleague`, 사용자 `sa`, 비밀번호 없음
- 예: `select * from trip;`, `select * from point_ledger;`
- **H2는 인메모리라 서버를 재시작하면 데이터가 모두 사라집니다.** 이전 토큰은 서명상 유효해도 운전자가 없으니 등록부터 다시 합니다.

## 5. API를 바꾼 뒤

```bash
./gradlew build                                   # 테스트 전체 통과 확인
./gradlew bootRun                                 # 다른 터미널에서
curl -s http://localhost:8080/v3/api-docs | jq -S 'del(.servers)' > docs/api/openapi.json
```

`docs/api/openapi.json`을 같은 PR에 커밋합니다(CLAUDE.md "API specification").

## 6. 문제 해결

| 증상 | 원인 · 해결 |
| --- | --- |
| 시작 시 `JWT_SECRET` 오류 | `.env`가 없거나 값이 비었거나 32바이트 미만. 2장을 다시 하세요. IntelliJ 실행이면 작업 디렉터리가 프로젝트 루트인지 확인 |
| `Port 8080 was already in use` | 서버가 이미 떠 있음. 기존 프로세스를 끄거나 `SERVER_PORT=8081` |
| 등록 · 토큰 재발급이 500 `INTERNAL_ERROR` | Redis가 꺼져 있음. `docker compose up -d` |
| 보호된 API가 401 `UNAUTHORIZED` | `Authorization: Bearer <accessToken>` 헤더가 없음 |
| 보호된 API가 401 `INVALID_TOKEN` | Access 토큰 만료(1시간, `POST /api/auth/refresh`로 재발급) 또는 `JWT_SECRET`을 바꾼 뒤 이전 토큰 사용 |
| 업로드가 400 `INVALID_INPUT` | 운행 시각이 과거 7일보다 오래됐거나 미래 5분을 넘음(D17), 이벤트 2,000개 초과(D16), `endedAt <= startedAt`, 이벤트 시각이 운행 밖 |
| 업로드가 413 `PAYLOAD_TOO_LARGE` | 본문 1MB 초과(D16) |
| 업로드가 409 `CONFLICT` | 다른 운전자가 이미 쓴 `tripId` |
| `/swagger-ui.html`이 401 · 404 | 오래된 코드로 떠 있거나 `local`이 아닌 프로필. 최신 `develop`을 받고 서버를 완전히 재시작 |
| `./gradlew build`에서 Testcontainers 오류 | Docker 엔진이 꺼져 있음. Docker Desktop을 켜고 다시 실행(소켓 설정은 CLAUDE.md "Environment gotcha") |
| `docker: command not found` | Docker Desktop을 설치 · 실행했는지, 터미널을 새로 열었는지 확인 |
| Gradle이 JDK 17을 못 찾음 | JDK 17 설치 후 `JAVA_HOME`을 17로, IntelliJ Gradle JVM도 17로 |

## 7. PostgreSQL 전환 후 바뀌는 것 (`feat/postgres-flyway` 머지 후)

설계: [`docs/deployment.md`](deployment.md) 9장. 머지되면 이 장을 3 · 4장에 합치고 H2 내용을 지웁니다.

| 항목 | 지금 | 전환 후 |
| --- | --- | --- |
| `.env` | `JWT_SECRET` | + `DB_NAME` · `DB_USERNAME` · `DB_PASSWORD`(`openssl rand -base64 24`) |
| `docker compose up -d` | Redis | **PostgreSQL + Redis**(둘 다 `healthy` 확인) |
| 실행 순서 | Redis 없이도 서버는 뜸 | **DB가 꺼져 있으면 서버가 뜨지 않음.** 항상 compose 먼저 |
| 스키마 | Hibernate가 매번 새로 만듦 | Flyway가 `db/migration`을 적용하고 Hibernate는 검증만 |
| 데이터 | 재시작하면 사라짐 | Docker 볼륨에 남음 |
| 데이터 보기 | H2 콘솔 | `docker exec -it blinkerleague-postgres psql -U blinkerleague blinkerleague` 또는 IntelliJ Database(`localhost:5432`) |
| DB만 초기화 | 서버 재시작 | `docker compose rm -sf postgres && docker volume rm server_postgres-data && docker compose up -d postgres` |
| 시드 JSON 수정 반영 | 재시작하면 반영 | 기존 DB에는 반영 안 됨 → DB만 초기화 |
