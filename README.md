# BlinkerLeague Server

BlinkerLeague 백엔드 서버입니다.

## 기술 스택

- Java 17
- Spring Boot 3.3.13 (Web, Validation, Data JPA, Data Redis, Security + OAuth2 Resource Server(JWT))
- H2 (개발용 인메모리 DB)
- Gradle 7.6.4 (wrapper)
- Lombok

> IntelliJ IDEA 2021.3.1의 Gradle 동기화 호환 문제로 Gradle/Boot 버전을 낮춰 두었습니다.
> IDE를 업데이트하기 전에는 버전을 올리지 마세요.

## 실행 방법

JDK 17이 필요합니다. (IntelliJ: Settings → Build Tools → Gradle → Gradle JVM을 17로)

1. 로컬 secret 파일을 만듭니다. `.env`는 git에 올라가지 않습니다.

```bash
cp .env.example .env
openssl rand -base64 48       # 출력값을 .env의 JWT_SECRET= 뒤에 붙여 넣기
```

2. Redis를 띄우고(아래 Docker 설정) 서버를 실행합니다.

```bash
./gradlew bootRun      # 서버 실행 (기본 포트 8080)
./gradlew build        # 빌드 + 테스트 (Testcontainers 테스트가 있어 Docker 엔진이 실행 중이어야 함)
./gradlew test         # 테스트만 실행
```

Windows에서는 `gradlew.bat`을 사용해도 됩니다. `JWT_SECRET`이 없거나 32바이트보다 짧으면 서버가 시작되지 않습니다.

운영 secret은 저장소에 두지 않고 환경변수(추후 Vault)로 주입합니다.

## API

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/drivers` | 없음 | `{ "nickname": "깜빡왕", "regionCode": "11680" }` → 201 `{ "driverId", "nickname", "regionCode", "accessToken", "refreshToken" }`. 닉네임 2~12자 · 중복 시 409 `CONFLICT`, 지역 코드는 시군구 5자리 숫자 |
| `POST` | `/api/auth/refresh` | 없음 | `{ "refreshToken": "..." }` → `{ "accessToken", "refreshToken" }`. 사용한 Refresh 토큰은 무효, 재사용하면 그 운전자의 토큰 전체 무효 |
| `GET` | `/api/config/thresholds` | 없음 | 판정 기준값 · 점수 가중치 · 포인트 규칙 · 등급 기준(`grades`) · 항목별 팁 문구(`tips`)와 `version`. 앱은 시작할 때 내려받고 `version`이 바뀐 경우에만 갱신 |
| `POST` | `/api/trips` | Bearer | 운행 + 이벤트 업로드(형식은 plan.md 3.4). 새로 저장하면 201, 같은 `tripId` 재전송은 저장하지 않고 처음 결과를 200으로 반환. 응답은 점수 조회와 같은 운행 결과(아래 행). 최소 거리(기준값 `trip.minDistanceKm`, 기본 3km) 미만이면 `EXCLUDED_SHORT`(점수 없음), 아니면 업로드하면서 점수를 매겨 `SCORED`, 회전이 없으면 `NO_TURN`(점수 없음). 다른 운전자의 `tripId`는 409 `CONFLICT`, 필수 값 누락 · 음수 거리 · 구간 밖 이벤트 · 종료가 시작 이전이면 400 `INVALID_INPUT` |
| `GET` | `/api/trips/{tripId}/score` | Bearer | 운행 결과 `{ "tripId", "status", "score", "grade", "metrics": [{ "metric", "value", "score", "weight" }], "turns": { "total", "success" }, "summary": { "goals", "conceded", "fouls" }, "plays": [{ "occurredAt", "type", "result" }], "mission": null }`. 깜빡이 점수 = `signalOn`이 `true`인 회전 비율 × 100(측정한 항목만 가중치 재분배). `plays`는 모든 이벤트를 시각순으로 담고 `TURN`만 `result`(`GOAL`/`CONCEDED`)가 있음. 없는 운행 · 남의 운행은 404 `NOT_FOUND` |
| `GET` | `/api/trips?limit=10` | Bearer | 최근 경기 목록. 내 운행을 종료 시각 최신순으로 `[{ "tripId", "endedAt", "distanceKm", "status", "score", "grade" }]`. `limit` 기본 10, 50을 넘으면 50으로 자름, 1 미만이면 400 `INVALID_INPUT` |

보호된 API는 `Authorization: Bearer <accessToken>` 헤더가 필요합니다. 토큰이 없으면 401 `UNAUTHORIZED`, 위조 · 만료된 토큰은 401 `INVALID_TOKEN`으로 응답합니다.

### API 명세 (OpenAPI · Swagger)

요청 · 응답 필드, 상태 코드, 오류 코드 전체는 OpenAPI 명세에 있습니다. 컨트롤러에서 자동으로 만들어집니다(springdoc-openapi).

- **Swagger UI**: `local` 프로필로 서버를 띄운 뒤 <http://localhost:8080/swagger-ui.html>. 오른쪽 위 `Authorize`에 Access 토큰을 넣으면 보호된 API도 바로 호출할 수 있습니다.
- **명세 JSON**: <http://localhost:8080/v3/api-docs>
- **전달용 파일**: [`docs/api/openapi.json`](docs/api/openapi.json). 서버 없이 [Swagger Editor](https://editor.swagger.io/), Postman, IntelliJ에서 열 수 있습니다.
- 배포 환경에서는 노출하지 않습니다(`springdoc.*.enabled`는 기본 `false`, `local` 프로필에서만 `true`).

### Swagger 접속 방법

Swagger는 내 PC에서 띄운 서버(`local` 프로필)에서만 열립니다. 공용 주소는 없습니다.

**1. 서버 실행 준비** (처음 한 번)

```bash
git clone https://github.com/BlinkerLeague/server.git
cd server
cp .env.example .env
openssl rand -base64 48       # 출력값을 .env의 JWT_SECRET= 뒤에 붙여 넣기
docker compose up -d          # Redis (Docker 엔진이 켜져 있어야 함)
```

- JDK 17이 필요합니다.
- Docker 설치는 아래 "Docker 설정"을 참고하세요.
- Redis가 없어도 서버와 Swagger는 뜹니다. 다만 운전자 등록과 토큰 재발급이 실패해서 보호된 API를 호출할 수 없습니다.

**2. 서버 실행**

```bash
./gradlew bootRun
```

- 로그에 `Started ServerApplication`이 나오면 준비된 것입니다.
- 프로필을 지정하지 않으면 `local`로 실행됩니다.

**3. 브라우저에서 열기**

- Swagger UI: <http://localhost:8080/swagger-ui.html> (`/swagger-ui/index.html`로 이동합니다)
- 명세 JSON: <http://localhost:8080/v3/api-docs>

**4. 토큰 받기**

1. Swagger UI에서 `POST /api/drivers`를 펼칩니다.
2. `Try it out`을 누릅니다.
3. 아래 본문을 넣고 `Execute`를 누릅니다.

   ```json
   { "nickname": "테스트01", "regionCode": "11680" }
   ```

   - 닉네임은 2~12자이고, 이미 있는 닉네임이면 409가 납니다.
   - 지역 코드는 시군구 5자리 숫자입니다.
4. 응답의 `accessToken` 값을 복사합니다.

**5. 인증하기**

1. 오른쪽 위 `Authorize`를 누릅니다.
2. `bearerAuth` 칸에 `accessToken` 값만 붙여 넣습니다. `Bearer `는 붙이지 않습니다.
3. `Authorize` → `Close`를 누릅니다.

이제 자물쇠가 표시된 API(예: `POST /api/trips`)도 `Try it out`으로 호출할 수 있습니다.

**6. 토큰이 만료되면**

- Access 토큰은 1시간짜리입니다. 만료되면 `POST /api/auth/refresh`에 `{ "refreshToken": "..." }`를 보내 새 토큰을 받고, 다시 `Authorize`합니다.
- 서버를 재시작하면 DB(H2 메모리)가 비워져서 등록한 운전자와 운행이 사라집니다. 이전 토큰은 서명상 유효해도 가리키는 운전자가 없으니, 4번부터 다시 등록하세요.

**문제가 생기면**

| 증상 | 원인 · 해결 |
| --- | --- |
| `/swagger-ui.html`이 401 `UNAUTHORIZED` 또는 404 | Swagger가 없는 코드로 서버가 떠 있습니다. `git pull`로 최신 `develop`을 받고 서버를 **완전히 재시작**하세요(IntelliJ devtools 자동 재시작으로는 새 라이브러리가 반영되지 않음). `local`이 아닌 프로필로 띄워도 꺼져 있습니다. |
| 서버가 시작되지 않고 `JWT_SECRET` 오류 | `.env`가 없거나 `JWT_SECRET`이 비었거나 32바이트보다 짧습니다. 1번을 다시 하세요. |
| `Port 8080 was already in use` | 이미 서버가 떠 있습니다. 기존 프로세스를 끄거나 `SERVER_PORT=8081 ./gradlew bootRun`으로 띄우고 주소의 포트를 바꾸세요. |
| 등록 · 토큰 재발급이 500 `INTERNAL_ERROR` | Redis가 꺼져 있습니다. `docker compose up -d` 후 다시 시도하세요. |
| 보호된 API가 401 `UNAUTHORIZED` | `Authorize`를 하지 않았습니다. 5번을 보세요. |
| 보호된 API가 401 `INVALID_TOKEN` | Access 토큰이 만료됐거나(6번), `.env`의 `JWT_SECRET`을 바꾼 뒤 이전 토큰을 쓰고 있습니다(4번부터 다시). |

API를 바꿨으면 `docs/api/openapi.json`을 다시 만들어 함께 커밋합니다(`servers`는 지워 실행 포트가 박히지 않게 합니다).

```bash
./gradlew bootRun   # 다른 터미널에서
curl -s http://localhost:8080/v3/api-docs | jq 'del(.servers)' > docs/api/openapi.json
```

### API 명세 관리 규칙

API 명세는 **Swagger(OpenAPI)로 관리합니다.** 컨트롤러 코드의 어노테이션이 명세의 원본이고, 따로 손으로 쓰는 명세 문서는 두지 않습니다.

- **원본은 코드입니다.** 요청 · 응답 형식, 검증 규칙, 상태 코드, 오류 코드는 컨트롤러와 DTO의 어노테이션(`@Tag`, `@Operation`, `@ApiResponse`, Bean Validation)에 적습니다. 코드와 문서가 어긋나지 않도록 명세를 코드 밖에 따로 두지 않습니다.
- **API를 추가하거나 바꾸는 PR에는 명세도 함께 들어갑니다.**
  - 컨트롤러 어노테이션을 갱신합니다.
  - `ApiDocsTest`에 해당 API의 행을 추가합니다.
  - `docs/api/openapi.json`을 다시 만들어 같은 PR에서 커밋합니다.
- **다른 사람에게는 Swagger로 전달합니다.**
  - 서버를 직접 띄울 수 있는 사람: 저장소를 받아 `local` 프로필로 실행한 뒤 Swagger UI를 엽니다. 실행 순서는 위 "실행 방법"을 따릅니다.
  - 서버를 띄우기 어려운 사람: `docs/api/openapi.json`을 Swagger Editor나 Postman에서 엽니다.
- **다른 문서의 역할**
  - `plan.md`: 기능 설계와 결정 기록입니다. 명세가 아닙니다.
  - 위 API 표: 한눈에 보는 요약입니다.
  - `http/*.http`: 로컬 수동 테스트용입니다.
  - 형식이 서로 다르면 Swagger가 기준입니다.

## Docker 설정 (Redis)

Redis는 Refresh 토큰(SHA-256 해시)만 저장합니다. 서버 세션은 쓰지 않습니다(JWT, stateless).
Redis 연결은 처음 쓸 때 맺으므로 Redis 없이도 서버는 뜨지만, 운전자 등록(`POST /api/drivers`)과 토큰 재발급(`POST /api/auth/refresh`)은 실패합니다. 서버 실행 전에 Redis 컨테이너를 먼저 띄웁니다.
설정은 프로젝트 루트의 `docker-compose.yml`에 있습니다.

| 항목 | 값 |
| --- | --- |
| 이미지 | `redis:7-alpine` |
| 컨테이너 이름 | `blinkerleague-redis` |
| 포트 | `6379` (호스트 `localhost:6379`) |
| 데이터 볼륨 | `redis-data` (컨테이너를 지워도 유지) |
| 헬스체크 | `redis-cli ping` (5초 간격) |

서버의 접속 설정은 `application.yaml`의 `spring.data.redis.host/port`(`localhost:6379`)이며 compose 설정과 일치합니다.

### Windows

1. [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)를 설치합니다. (WSL 2 백엔드 권장, Windows 10/11 64bit)
2. 설치 후 Docker Desktop을 실행하고 왼쪽 아래 상태가 `Engine running`이 될 때까지 기다립니다.
3. PowerShell 또는 Git Bash에서 프로젝트 루트로 이동해 실행합니다.

```powershell
docker --version              # 설치 확인
docker compose up -d          # Redis 시작
docker compose ps             # STATUS가 healthy인지 확인
docker exec blinkerleague-redis redis-cli ping   # PONG 이 나오면 정상
.\gradlew.bat bootRun         # 서버 실행
```

주의:

- `docker` 명령이 `daemon is not running` 오류를 내면 Docker Desktop이 아직 실행되지 않은 것입니다.
- 6379 포트를 이미 쓰는 프로세스(로컬에 설치한 Redis 등)가 있으면 컨테이너가 뜨지 않습니다. `netstat -ano | findstr :6379`로 확인하세요.

### macOS

1. [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/)(Apple Silicon / Intel 중 맞는 버전)을 설치하거나 Homebrew를 사용합니다.

```bash
brew install --cask docker    # Docker Desktop 설치
```

Docker Desktop 대신 화면 없는 colima를 써도 됩니다.

```bash
brew install colima docker docker-compose
colima start
# docker compose 플러그인 경로를 ~/.docker/config.json에 추가
#   "cliPluginsExtraDirs": ["/opt/homebrew/lib/docker/cli-plugins"]
# Testcontainers용: ~/.testcontainers.properties
#   docker.host=unix:///Users/<사용자>/.colima/default/docker.sock
```

2. `Docker` 앱을 한 번 실행해 데몬을 시작합니다. (메뉴바 고래 아이콘이 안정되면 준비 완료)
3. 터미널에서 프로젝트 루트로 이동해 실행합니다.

```bash
docker --version              # 설치 확인
docker compose up -d          # Redis 시작
docker compose ps             # STATUS가 healthy인지 확인
docker exec blinkerleague-redis redis-cli ping   # PONG 이 나오면 정상
./gradlew bootRun             # 서버 실행
```

주의:

- `redis:7-alpine` 이미지는 Apple Silicon(arm64)과 Intel(amd64)을 모두 지원해 별도 설정이 필요 없습니다.
- 6379 포트 사용 여부는 `lsof -i :6379`로 확인합니다. Homebrew로 설치한 Redis가 떠 있다면 `brew services stop redis`로 먼저 멈추세요.

### 자주 쓰는 명령 (공통)

```bash
docker compose up -d              # 시작 (백그라운드)
docker compose stop               # 중지 (컨테이너 유지)
docker compose down               # 중지 + 컨테이너 삭제 (볼륨 유지)
docker compose down -v            # 컨테이너 + 데이터 볼륨까지 삭제
docker compose logs -f redis      # 로그 보기
docker exec -it blinkerleague-redis redis-cli   # Redis CLI 접속
```

## 브랜치 규칙

| 브랜치 | 용도 | 분기 기준 | 병합 대상 |
| --- | --- | --- | --- |
| `main` | 배포 가능한 안정 버전 | - | - |
| `develop` | 다음 배포를 위한 통합 브랜치 | `main` | `main` |
| `feat/<이름>` | 새 기능 개발 | `develop` | `develop` |
| `fix/<이름>` | 버그 수정 | `develop` | `develop` |
| `refactor/<이름>` | 동작 변경 없는 구조 개선 | `develop` | `develop` |
| `docs/<이름>` | 문서 작성 및 수정 | `develop` | `develop` |
| `hotfix/<이름>` | 배포 중인 버전의 긴급 수정 | `main` | `main`, `develop` |

규칙:

- `main`, `develop`에는 직접 push하지 않고 Pull Request로만 병합합니다.
- 브랜치 이름은 소문자와 하이픈(`-`)을 사용합니다. 예: `feat/match-schedule`, `fix/login-session-expire`
- 한 브랜치에는 하나의 목적만 담습니다.
- 병합이 끝난 브랜치는 삭제합니다.
- 초기 개발 단계에서는 리뷰 승인 없이 PR을 병합할 수 있습니다. (추후 팀 규모가 커지면 리뷰 승인을 필수로 전환)

## 커밋 컨벤션

형식:

```
<type>: <제목>

<본문 (선택)>
```

| type | 설명 | 예시 |
| --- | --- | --- |
| `feat` | 새로운 기능 추가 | `feat: 경기 일정 조회 API 추가` |
| `fix` | 버그 수정 | `fix: 세션 만료 시 로그인 실패 수정` |
| `refactor` | 동작 변경 없는 코드 개선 | `refactor: 팀 서비스 중복 로직 분리` |
| `docs` | 문서 수정 | `docs: README에 브랜치 규칙 추가` |
| `test` | 테스트 추가/수정 | `test: 팀 서비스 단위 테스트 추가` |
| `chore` | 빌드 설정, 의존성 등 기타 작업 | `chore: Gradle 버전 변경` |

규칙:

- 제목은 50자 이내, 마침표 없이 작성합니다.
- 제목은 "무엇을 했는지"가 드러나게 씁니다.
- "왜" 바꿨는지가 필요하면 본문에 적습니다.
- 한 커밋에는 하나의 변경 목적만 담습니다.

## Pull Request 규칙

- 제목은 커밋 컨벤션과 같은 형식을 따릅니다. 예: `feat: 경기 일정 조회 API 추가`
- 변경 내용과 확인 방법(테스트 결과 등)을 본문에 적습니다.
- `./gradlew build`가 통과한 상태에서 올립니다.
- API를 추가하거나 바꿨다면 컨트롤러 어노테이션 · `ApiDocsTest` · `docs/api/openapi.json`을 같은 PR에 포함합니다([API 명세 관리 규칙](#api-명세-관리-규칙)).
