# BlinkerLeague Server

BlinkerLeague 백엔드 서버입니다.

## 기술 스택

- Java 17
- Spring Boot 3.3.13 (Web, Data JPA, Security, Data Redis, Spring Session)
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
| `POST` | `/api/auth/refresh` | 없음 | `{ "refreshToken": "..." }` → `{ "accessToken", "refreshToken" }`. 사용한 Refresh 토큰은 무효, 재사용하면 그 운전자의 토큰 전체 무효 |

보호된 API는 `Authorization: Bearer <accessToken>` 헤더가 필요합니다. 토큰이 없으면 401 `UNAUTHORIZED`, 위조 · 만료된 토큰은 401 `INVALID_TOKEN`으로 응답합니다.

## Docker 설정 (Redis)

Spring Session이 Redis를 사용하므로 서버 실행 전에 Redis 컨테이너를 먼저 띄웁니다.
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
