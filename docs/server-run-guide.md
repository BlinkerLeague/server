# 서버 실행 가이드

깜빡이리그 서버를 내 PC에서 띄우고, 운전자 등록부터 운행 점수 확인까지 직접 호출해 보는 순서입니다. 작성 2026-11-10.

- 로컬 DB는 Docker PostgreSQL, Refresh 토큰은 Docker Redis입니다(`feat/postgres-flyway`부터). DB 다루는 법은 7장, 설계는 [`docs/deployment.md`](deployment.md) 9장입니다.
- 새 Mac이라면 8장(Docker · Testcontainers · JDK 17 셋팅)부터 하세요.
- 배포 환경(HTTPS · 운영 DB) 실행은 아직 없습니다. 계획은 `docs/deployment.md` 3 · 5 · 8장입니다.
- API 요청 · 응답 형식은 Swagger가 기준입니다(README "API 명세 관리 규칙"). 이 문서는 실행 순서만 다룹니다.

## 1. 준비물

| 준비물 | 버전 · 확인 | 비고 |
| --- | --- | --- |
| JDK | **17** (`/usr/libexec/java_home -v 17`) | **Gradle 7.6.4 자체가 JDK 17에서 실행되어야 합니다**(기본 `java`가 19 · 20이면 `Unsupported class file major version` 오류). `JAVA_HOME`을 17로(8.4). IntelliJ는 Gradle JVM을 17로 |
| Docker | Docker Desktop(`docker --version`, `docker compose version`) | PostgreSQL · Redis와 Testcontainers 테스트에 필요. **설치 · Testcontainers 설정은 8장**(macOS), Windows는 README "Docker 설정" |
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
openssl rand -hex 24           # 출력값을 .env의 DB_PASSWORD= 뒤에 붙여 넣기
```

- `.env`는 git에 올라가지 않습니다. 값을 메신저나 문서에 붙여 넣지 마세요.
- `DB_NAME` · `DB_USERNAME`은 예시값(`blinkerleague`) 그대로 두면 됩니다. docker compose와 서버가 같은 `.env`를 읽으므로 한 곳만 채웁니다.
- `JWT_SECRET`이 없거나, `${...}` 그대로이거나, 32바이트보다 짧으면, `DB_PASSWORD`가 비어 있으면 서버가 시작되지 않습니다(의도된 동작).
- **DB를 처음 만든 뒤 `DB_PASSWORD`를 바꾸면 접속이 실패합니다.** PostgreSQL은 볼륨을 처음 만들 때의 비밀번호를 계속 씁니다. 바꾸려면 DB만 초기화하세요(7장).

## 3. 실행

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # 기본 java가 17이 아니면
docker compose up -d           # PostgreSQL + Redis 시작
docker compose ps              # STATUS가 둘 다 healthy인지 확인
./gradlew bootRun              # 서버 시작(프로필 지정이 없으면 local)
```

- **PostgreSQL이 꺼져 있으면 서버가 시작되지 않습니다.** 항상 compose를 먼저 띄웁니다.
- 처음 시작하면 Flyway가 스키마를 만듭니다(로그 `Successfully applied 1 migration`). 다음부터는 `Schema "public" is up to date`입니다.
- 로그에 `Started ServerApplication`이 나오면 준비 완료입니다. 기본 포트는 8080입니다.
- 처음 시작할 때 기준값 · 오늘의 미션 · 배지 카탈로그가 자동으로 들어갑니다(시드, 이미 있으면 건너뜀).
- 로그는 콘솔과 `logs/server.log`(7일 보관)에 남습니다.
- IntelliJ에서는 `ServerApplication`의 실행 버튼으로 띄워도 됩니다. 작업 디렉터리가 프로젝트 루트여야 `.env`를 읽습니다.

| 할 일 | 명령 |
| --- | --- |
| 서버 중지 | 터미널에서 `Ctrl + C` |
| DB · Redis 중지(데이터 유지) | `docker compose stop` |
| DB만 초기화 · 전부 초기화 | 7장 |
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

### 4.4 저장된 데이터 보기

```bash
docker exec -it blinkerleague-postgres psql -U blinkerleague blinkerleague
```

- 예: `select * from trip;`, `select * from point_ledger;`, 끝내기는 `\q`
- IntelliJ Database 도구로도 접속할 수 있습니다(`localhost:5432`, `.env`의 사용자 · 비밀번호).
- 데이터는 재시작해도 남습니다. 처음 상태로 돌리려면 7장의 "DB만 초기화".

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
| 시작 시 `DB 비밀번호가 없습니다. 환경변수 DB_PASSWORD를 설정하세요.` | `.env`에 `DB_PASSWORD`가 없거나 비어 있음. 2장 |
| 시작 시 `Connection to localhost:5432 refused` | PostgreSQL이 꺼져 있음. `docker compose up -d` 후 `healthy` 확인 |
| 시작 시 `password authentication failed` | DB를 만든 뒤 `.env`의 `DB_PASSWORD`를 바꿈. 원래 값으로 되돌리거나 DB만 초기화(7장) |
| 시작 시 `Schema-validation: ...` | 엔티티를 바꾸고 마이그레이션을 추가하지 않음. 새 `V{n}__*.sql` 추가(7장) |
| 시작 시 `Validate failed: Migrations have failed validation` | 이미 적용된 마이그레이션 파일을 고침. 되돌리거나 DB만 초기화(7장) |
| `Unsupported class file major version 64` 등으로 Gradle이 바로 실패 | Gradle이 JDK 17이 아닌 JDK로 실행됨. `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`(8.4) |
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

## 7. 로컬 DB 다루기 (PostgreSQL)

로컬 DB는 docker compose의 PostgreSQL 16입니다(D3, D37). 설계는 [`docs/deployment.md`](deployment.md) 4 · 9장입니다.

| 할 일 | 명령 · 설명 |
| --- | --- |
| 접속 | `docker exec -it blinkerleague-postgres psql -U blinkerleague blinkerleague` 또는 IntelliJ Database(`localhost:5432`, `.env`의 사용자 · 비밀번호) |
| 마이그레이션 이력 | `select version, description, success from flyway_schema_history;` |
| **DB만 초기화** | `docker compose rm -sf postgres && docker volume rm server_postgres-data && docker compose up -d postgres` (Redis 토큰은 유지, 앱에서 쓰던 운전자는 사라지므로 재등록) |
| 전부 초기화 | `docker compose down -v` (DB와 Redis 토큰 모두 삭제) |
| 볼륨 이름 확인 | `docker volume ls` (폴더 이름 `server`가 앞에 붙음) |

- **데이터가 남습니다.** 서버를 재시작해도 운전자 · 운행 · 포인트가 그대로입니다.
- **스키마는 Flyway만 만듭니다**(`src/main/resources/db/migration`). 엔티티를 바꾸면 새 `V{n}__설명.sql`을 추가하세요. 엔티티와 스키마가 다르면 `Schema-validation: ...` 오류로 서버가 시작되지 않습니다.
- **이미 적용된 마이그레이션 파일은 고치지 않습니다.** 고치면 체크섬 오류로 시작되지 않습니다. 로컬에서 실수로 고쳤다면 파일을 되돌리거나 DB만 초기화합니다.
- **시드 JSON(기준값 · 미션 · 배지)을 고쳐도 기존 DB에는 반영되지 않습니다**(값이 없을 때만 넣음). 로컬은 DB만 초기화하면 다시 들어갑니다.
- 테스트는 H2 인메모리를 쓰고, `PostgresSchemaTest`만 Testcontainers PostgreSQL에서 마이그레이션과 엔티티가 맞는지 확인합니다.

## 8. macOS 개발 환경 셋팅 (Docker · JDK)

새 Mac이나 Docker가 없는 Mac에서 처음 한 번 하는 설정입니다(Apple Silicon 기준, Intel도 같음). 2026-11-10 개발 Mac에 Docker가 없어 `feat/postgres-flyway`가 멈춘 일을 계기로 정리했고, 같은 날 이 순서로 셋팅해 확인했습니다(Docker Desktop 4.99 · Engine 29.9).

### 8.1 먼저 확인

```bash
docker version                   # Client와 Server가 모두 나오면 8.2는 건너뜀
ls ~/.docker/run/docker.sock     # Docker Desktop 소켓
/usr/libexec/java_home -V        # 설치된 JDK 목록(17이 있어야 함)
```

| 결과 | 할 일 |
| --- | --- |
| `docker: command not found` | 8.2 설치 |
| Client만 나오고 `Cannot connect to the Docker daemon` | Docker 앱이 꺼져 있음. 8.2의 2번(앱 실행) |
| JDK 목록에 17이 없음 | 8.4 |

### 8.2 Docker Desktop 설치

1. Homebrew로 설치합니다(관리자 비밀번호를 물을 수 있음). Homebrew가 없으면 [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/)에서 칩에 맞는 버전을 받아도 됩니다.

   ```bash
   brew install --cask docker
   ```

2. `응용 프로그램`의 **Docker** 앱을 한 번 실행하고 서비스 약관에 동의합니다. 로그인은 하지 않아도 됩니다.
3. 메뉴바의 고래 아이콘이 움직임을 멈추면(엔진 실행 중) 확인합니다.

   ```bash
   docker version                # Server: Docker Engine ... 이 나와야 함
   docker compose version
   docker run --rm hello-world   # 이미지를 받아 실행되면 정상
   ```

4. (권장) Docker Desktop → Settings → General → **Start Docker Desktop when you sign in**을 켜 두면 재부팅 뒤에도 바로 쓸 수 있습니다.

- colima도 설치할 수 있지만 **Docker Desktop과 함께 켜지 마세요**(소켓이 엇갈림). 이 프로젝트는 Docker Desktop을 기준으로 합니다(CLAUDE.md "Environment gotcha").

### 8.3 Testcontainers 설정 (테스트용, 한 번만)

`./gradlew build`의 Testcontainers 테스트(Redis, PostgreSQL)는 Docker 소켓을 찾아야 합니다. Docker Desktop의 소켓은 `~/.docker/run/docker.sock`이고, Mac에는 `/var/run/docker.sock`이 없을 수 있어서 홈 폴더에 설정 파일을 둡니다.

```bash
echo "docker.host=unix://$HOME/.docker/run/docker.sock" > ~/.testcontainers.properties
cat ~/.testcontainers.properties
```

- 저장소에는 넣지 않는 개인 설정입니다(사용자 경로가 들어감).
- 나머지 두 설정은 이미 `build.gradle`에 있어 따로 할 일이 없습니다.
  - `api.version=1.44`: Docker Engine 29 이상이 오래된 API 버전(1.32)을 거부하기 때문
  - `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`: Ryuk(정리용 컨테이너)가 Docker VM 안에서 쓸 소켓 경로
- 확인: `./gradlew test --tests "*RedisRefreshTokenStoreTest" --tests "*PostgresSchemaTest"`가 통과하면 정상입니다(2026-11-10 개발 Mac에서 확인).

### 8.4 JDK 17

**Gradle 7.6.4는 JDK 17에서 실행해야 합니다.** 기본 `java`가 19 · 20이면 `./gradlew`가 `Unsupported class file major version 64` 같은 오류로 바로 실패합니다(2026-11-10 개발 Mac에서 확인, 기본이 JDK 20이었음). 17이 없으면 설치하고, 터미널에서 `JAVA_HOME`을 17로 맞춥니다.

```bash
brew install --cask corretto@17
/usr/libexec/java_home -v 17     # 경로가 나오면 정상
```

- **터미널 기본값을 17로 맞춥니다**: `~/.zshrc`에 `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`를 추가하고 터미널을 새로 엽니다. 한 번만 쓸 때는 명령 앞에서 같은 `export`를 실행합니다.
- IntelliJ: Settings → Build Tools → Gradle → **Gradle JVM**을 17로, Project Structure → SDK도 17로.

### 8.5 셋팅 후 전체 확인

```bash
cd server
docker compose up -d && docker compose ps     # postgres · redis 모두 healthy
./gradlew build                                # Testcontainers 포함 전체 통과
./gradlew bootRun                              # Started ServerApplication
```

여기까지 되면 2 ~ 4장 순서로 사용하면 됩니다.

### 8.6 자주 막히는 곳

| 증상 | 원인 · 해결 |
| --- | --- |
| `Cannot connect to the Docker daemon at unix:///var/run/docker.sock` | Docker 앱이 꺼져 있음. 앱을 실행하고 고래 아이콘이 멈출 때까지 대기 |
| 테스트에서 `Could not find a valid Docker environment` | `~/.testcontainers.properties`가 없거나 경로가 틀림(8.3). Docker 앱 실행 여부도 확인 |
| 테스트에서 `client version 1.32 is too old` | `build.gradle`의 `api.version` 설정이 빠짐. 최신 `develop`을 받기 |
| `docker compose up`에서 `port is already allocated` | 같은 포트를 다른 프로그램이 사용 중. Redis는 `brew services stop redis`, PostgreSQL은 `brew services stop postgresql@16` 등으로 끄기 |
| `brew install --cask docker`가 권한 오류 | 관리자 계정으로 실행하거나 공식 사이트에서 dmg로 설치 |
