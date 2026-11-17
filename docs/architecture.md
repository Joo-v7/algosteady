# 아키텍처

## 전체 구성

- 프론트엔드는 코드 입력·화면 표시·결과 조회를 담당한다.
- 백엔드는 콘텐츠·회원·제출을 관리한다.
- Docker는 실행한 코드의 결과를 DB에 저장한다.

```text
React 화면 → Spring Boot API → PostgreSQL
                    │
                    ▼
              단일 실행 작업자
                    │
                    ▼
              Docker 실행 환경
```

## 프로젝트 구성

| 디렉터리 | 역할 |
| --- | --- |
| `frontend/` | React 페이지, API 호출·타입, 스타일 |
| `backend/` | 콘텐츠·인증·학습 기록·제출·채점 API |
| `content/` | 학습 설명·실습 규격·언어별 코드·테스트 원본 |
| `runners/` | 사용자 코드 실행용 Docker 이미지 |
| `docs/` | API·구조·콘텐츠 규격·학습 목록 |

## 백엔드 모듈

| 모듈 | 역할 |
| --- | --- |
| `exercise` | 파일 검증·버전별 콘텐츠 등록, 분류·목록·상세·기준 코드 조회 |
| `auth` | OAuth 로그인, 세션·CSRF, 현재 회원 조회·로그아웃·탈퇴 API |
| `member` | 제공자 계정별 회원 등록·프로필 갱신·탈퇴 삭제 |
| `learning` | 오늘의 학습 배정·완료 기록, 학습 현황·연도별 기록 조회 |
| `training` | 제출 원본 저장, 본인 제출 목록·상세 조회 |
| `judging` | 작업 상태 관리, 기대값 비교, 채점 결과 저장·집계 |
| `judging.runner` | Docker 준비·컴파일·실행, 출력 수집·제한·자원 정리 |
| `common` | 공통 오류 응답·연결 종료 처리 |

Controller는 HTTP, Service는 업무 처리·트랜잭션, Repository는 DB 접근을 담당한다. 실행·채점의 기본 호출 방향은 `training → judging → exercise`이며, runner는 DB와 채점 기준을 참조하지 않는다.

## 콘텐츠와 실행 흐름

1. 서버 시작 시 `content/` 파일을 검증해 DB에 버전별 스냅샷으로 등록한다.
2. 목록·상세·기준 코드 조회와 실행·채점은 등록된 DB 콘텐츠를 사용한다.
3. 실행 요청을 대기 작업으로 저장한다. 제출은 원본 코드와 전체 채점 작업을 같은 트랜잭션으로 저장한다.
4. 단일 작업자가 커밋된 대기 작업을 순서대로 처리한다. Docker 실행 동안 DB 트랜잭션을 유지하지 않는다.
5. 테스트별 결과를 저장하고 화면은 작업 API를 500ms 간격으로 조회한다. 제출 기록은 원본 코드와 해당 작업 결과를 연결한다.

콘텐츠는 파일에서 편집하고, 변경 시 새 버전으로 등록한다. 기존 콘텐츠 버전과 제출 당시의 평가 기준은 보존한다. 초안은 브라우저에 저장하며, DB 구조는 Flyway로 관리한다. 세부 규격은 [콘텐츠 문서](content.md)를 따른다.

## 인증과 개인 기록

- Google OAuth 로그인과 서버 세션을 사용한다. 별도 회원가입 화면·비밀번호 로그인은 없다.
- 학습·기준 코드 조회와 예제 실행은 비로그인도 허용한다. 제출·학습 기록·제출 기록은 로그인한 본인만 조회한다.
- 회원은 OAuth 제공자 ID와 제공자의 사용자 ID로 식별한다. 이메일이 같은 다른 제공자 계정을 자동 연결하지 않는다.
- 로그아웃은 현재 세션을 종료한다. 탈퇴는 계정·본인 제출·채점·학습 기록을 삭제하고 등록된 로그인 세션을 만료한다.
- 제출·채점 결과 저장과 탈퇴는 회원 잠금으로 순서를 맞춘다. 삭제된 작업의 후속 결과는 저장하지 않는다.

## 오늘의 학습과 마이페이지

- 오늘의 학습은 한국 시간 오전 6시를 날짜 경계로 삼고, 날짜·등록된 실습 ID를 기준으로 공통 항목을 배정한다. 최초 배정을 DB에 저장하므로 같은 날에는 바뀌지 않는다.
- 완료 후 다시 실패해도 완료 기록은 유지한다.
- 완료 날짜는 채점 종료 시각이 아닌 **제출 접수 시각**으로 결정한다. `judging`의 통과 이벤트를 `learning`이 같은 트랜잭션에서 기록한다.
- 회원·날짜별 완료는 한 번만 인정한다. 예제 실행·기준 코드 열람·다른 실습 통과는 일일 완료에 포함하지 않는다.
- 마이페이지는 일일 완료 기록으로 오늘 완료 여부·연속 일수·누적 일수·연도별 잔디를 표시한다. 완료 날짜를 누르면 최초 통과 제출 상세로 이동한다.
- 홈의 분류·이름 검색과 페이지당 9개 표시는 프론트에서 처리한다. 제출 목록의 페이지 조회는 DB에서 처리한다.

## 실행 환경

현재 실행 언어는 C17이다. Python은 추가 예정이며, 일정은 [README](../README.md#개발-계획)에서 관리한다.

- 작업마다 독립된 컨테이너에서 준비·컴파일하고, 테스트마다 새 프로세스로 실행한다.
- 네트워크·권한·CPU·메모리·프로세스 수·실행 시간·출력량을 제한한다.
- 실행 완료·실패 시 컨테이너와 임시 파일을 정리한다. 서버 재시작 시 `RUNNING` 작업은 중단으로 기록하고 `QUEUED` 작업은 이어서 처리한다.

제한값·이미지는 [설정 파일](../backend/src/main/resources/application.properties), 명령은 [LanguageSpec](../backend/src/main/java/com/algosteady/judging/runner/language/LanguageSpec.java)에서 관리한다.

## 애플리케이션 실행 구성

| 구성요소 | 실행 조건 |
| --- | --- |
| 백엔드 | Java 21 실행 JAR. DB·OAuth 환경변수 필요 |
| 프론트 | 개발 시 Vite, 배포 시 빌드한 `dist`를 웹 서버에서 제공. 현재 JAR에 포함되지 않음 |
| DB | PostgreSQL 별도 실행 |
| 코드 실행부 | Docker 엔진과 C 실행 이미지 필요. POSIX 파일 권한 지원 환경 필요. Windows에서는 WSL2 등 Linux 환경에서 실행 |

로컬 실행 방법은 [README](../README.md#로컬-실행)를 따른다.

개발 시 Vite가 `/api`, `/oauth2`, `/login/oauth2` 요청을 백엔드로 전달한다. IntelliJ 실행 구성에는 JDK 21과 DB·OAuth 환경변수를 등록한다. `.env`는 자동으로 읽지 않는다.

## 실행 프로필

`application.properties`는 공통 설정이며, 활성 프로필에 따라 환경별 설정을 추가로 읽는다.

| 프로필 | 설정 파일 | 실행 방식 |
| --- | --- | --- |
| `dev` | `application-dev.properties` | `SPRING_PROFILES_ACTIVE=dev` 또는 IntelliJ Active profiles에 `dev` 지정 |
| `prod` | `application-prod.properties` | `SPRING_PROFILES_ACTIVE=prod` 지정 |

`prod`는 공통 DB·OAuth 환경변수 외에 `POSTGRES_HOST`와 `FRONTEND_URL`이 필요하다. `POSTGRES_PORT`의 기본값은 `5432`다. Blue·Green은 모두 `prod`를 사용하고 포트와 인스턴스 이름으로 구분한다.

## 로그

[Logback 설정](../backend/src/main/resources/logback-spring.xml)으로 로컬은 콘솔, `prod` 프로필은 콘솔과 파일에 기록한다. 파일 경로는 `<로그 경로>/<인스턴스>/application.log`·`error.log`이며, 파일 로그에 코드 버전을 함께 표시한다.

| 환경변수 | 용도 | 기본값 |
| --- | --- | --- |
| `LOGGING_FILE_PATH` | 로그 저장 경로 | `logs` |
| `ALGOSTEADY_INSTANCE_NAME` | 실행 인스턴스 이름 | `default` |
| `ALGOSTEADY_RELEASE_VERSION` | 배포 코드의 Git 커밋 ID | `local` |
