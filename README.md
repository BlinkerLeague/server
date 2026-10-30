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

```bash
./gradlew bootRun      # 서버 실행 (기본 포트 8080)
./gradlew build        # 빌드 + 테스트
./gradlew test         # 테스트만 실행
```

Windows에서는 `gradlew.bat`을 사용해도 됩니다.

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
