# Algosteady

자료구조와 알고리즘을 직접 구현하고, 실행·채점 결과로 확인하는 학습 웹 프로젝트.

## 주요 기능

- 오전 6시 기준 오늘의 학습 추천, 분류·이름 검색 및 페이지당 9개 학습 목록
- OAuth 로그인·회원 자동 등록 및 세션 로그아웃 (Google 연결)
- 비회원 예제 실행, 회원별 제출·제출 기록 조회
- 오늘의 학습 전체 채점 통과 시 일일 완료 저장 및 홈 완료 표시
- 학습 목록·학습 내용·구현 규격·기준 코드 조회
- Monaco 편집기로 코드 작성 및 브라우저 초안 저장
- 공개 예시 실행과 전체 테스트 채점
- 컴파일 오류·실행 출력·실행 시간·기준별 판정 확인
- 제출 기록 목록·상세 조회 및 코드 확인

## 개발 계획

C17을 기본 언어로 운영하고, 서비스 배포와 CI/CD 연결 후 Python을 추가한다.

| 목표 기간 | 개발 항목 | 목표 |
| --- | --- | --- |
| 2026-10-12 ~ 10-14 | 프로젝트 초기 구성·설계 | React·Spring Boot·DB 구성, 실습·실행·저장 구조 설계 |
| 2026-10-15 ~ 10-19 | 핵심 학습 흐름 | 학습 코드 조회, 코드 작성, Docker 실행·전체 채점, 제출 기록 연결 |
| 2026-10-20 ~ 10-23 | 실습 화면·편집기 개선 | 실행 시간 표시, Monaco 문법 강조·들여쓰기 및 브라우저 초안 저장 연결 |
| 2026-10-21 ~ 10-25 | 실습 확대 | [학습 목록](docs/curriculum.md)의 C17 콘텐츠 56개 구현 |
| 2026-10-25 | 오늘의 학습 | 로그인 없이 오전 6시 기준 공통 학습 추천과 학습·실습 이동 제공 |
| 2026-10-25 | 홈 개편 | 오늘의 학습 카드, 분류·이름 검색, 페이지당 9개 카드와 페이지네이션 |
| 2026-10-26 ~ 10-30 | Google 로그인·개인 기록 | OAuth 로그인, 비회원 예제 실행과 회원별 제출·채점 결과 조회 |
| 2026-11-07 ~ 11-12 | 마이페이지 | 학습 현황, 잔디형 완료 기록, 날짜별 제출 기록 연결 |
| 2026-11-13 ~ 11-15 | 회원탈퇴 | 마이페이지 하단 탈퇴 버튼, 데이터 처리 안내·최종 확인 |
| 2026-11-10 ~ 11-15 | 서버 배포 | 서버·PostgreSQL 배포, 도메인·HTTPS·운영 환경변수·OAuth 연결 |
| 2026-11-16 ~ 11-20 | CI/CD | 빌드·테스트·배포 자동화 구성 |
| 2026-11-21 ~ 12-13 | Python 지원 | Python 실행 이미지와 실습 코드·테스트 연결 |

추가 기능의 화면 구성과 확인할 정책은 [추가 기능·화면 계획](docs/architecture.md#추가-기능화면-계획)에서 관리한다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| 프론트엔드 | React, TypeScript, Vite, Monaco Editor |
| 백엔드 | Java 21, Spring Boot, Spring Data JPA |
| 데이터베이스 | PostgreSQL 18, Flyway |
| 실행 환경 | 언어별 Docker 이미지, 기본 C17·Python 추가 예정 |
| 개발 도구 | OpenAI Codex |

## 로컬 실행

Node.js 24, JDK 21, Docker를 준비한다. 아래 명령은 저장소 루트에서 시작한다.

**1. 환경변수·DB·실행 이미지 준비**

`.env`가 없으면 `.env.example`을 복사하고 DB·OAuth 값을 설정한다. 기존 `.env`는 유지하고 누락된 값을 추가한다.

| 환경변수 | 설정 |
| --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | 로컬 PostgreSQL 접속 정보 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth 웹 애플리케이션 클라이언트 자격 증명 |
| `FRONTEND_URL` | 웹 화면 주소. 로컬 기본값은 `http://localhost:5173` |

Google Console의 승인된 리디렉션 URI에 `http://localhost:5173/login/oauth2/code/google`을 등록한다([Google 설정 안내](https://developers.google.com/identity/protocols/oauth2/web-server#creatingcred)). 로그인은 프론트 화면에서 시작하며, 주소를 바꾸면 `FRONTEND_URL`과 등록된 리디렉션 URI를 함께 맞춘다. 별도 회원가입·비밀번호 입력 화면은 제공하지 않는다.

Google Auth Platform의 **클라이언트 → 웹 애플리케이션**에서 발급한 실제 ID·시크릿을 사용한다. 클라이언트 ID는 `…apps.googleusercontent.com`으로 끝난다. `.env.example`의 예시값은 로그인에 사용할 수 없다. `401 invalid_client`와 `The OAuth client was not found`가 나오면 실행 중인 백엔드의 클라이언트 ID를 먼저 확인한다. IntelliJ는 `.env`를 자동으로 읽지 않으므로 실행 구성의 환경변수에 등록하고, 값을 바꾼 뒤 백엔드를 재시작한다.

로그인 버튼은 [Google 브랜드 가이드](https://developers.google.com/identity/branding-guidelines?hl=ko)의 밝은 테마와 공식 로고를 사용한다.

```bash
cp .env.example .env
# .env 수정 후 실행
docker compose up -d postgres
docker build -t algosteady-runner-c:14.4.0 runners/c
```

**2. 백엔드 실행**

```bash
set -a
source .env
set +a
cd backend
./mvnw spring-boot:run
```

IDE 실행 시에도 JDK 21과 `.env`의 DB·OAuth 환경변수를 실행 설정에 등록한다. 서버는 Flyway로 DB 구조를 적용하고 실습 콘텐츠를 등록한다.

**3. 프론트엔드 실행 — 별도 터미널**

```bash
cd frontend
npm ci
npm run dev
```

| 대상 | 주소 |
| --- | --- |
| 웹 화면 | http://localhost:5173 |
| API 서버 | http://localhost:8090 |
| PostgreSQL | localhost:5432 |

Vite가 `/api`, `/oauth2`, `/login/oauth2` 요청을 API 서버로 전달한다. 종료할 때 각 서버를 중지하고 루트에서 `docker compose stop postgres`를 실행한다.

## 빌드·검증

프론트엔드에서 실행한다.

```bash
npm run build
npm run lint
npm test
```

백엔드는 환경변수를 전달한 터미널에서 실행한다. 기본 테스트는 PostgreSQL이 필요하고, Docker 테스트는 실행 이미지도 필요하다.

```bash
./mvnw test
./mvnw -Dalgosteady.postgres.tests=true test
./mvnw '-Dtest=CodeExecutionDockerTests,ExerciseContentDockerTests' -Dalgosteady.docker.tests=true test
./mvnw -DskipTests package
```

## 개발 이력

| 날짜 | 내용 |
| --- | --- |
| 2026-10-12 | 프로젝트 생성 및 프론트엔드 초기 구성 |
| 2026-10-14 | 프로젝트 설계·개발 계획 문서 작성 |
| 2026-10-19 | 실습 콘텐츠 조회, Docker 실행부, 공개 예시 실행·전체 채점, 제출 기록 연결 |
| 2026-10-21 | 재귀·스택·큐·덱·연결 리스트 콘텐츠 추가, Monaco 적용 및 학습·채점 화면 개선 |
| 2026-10-22 | 트리·힙·해싱·유니온 파인드·이진 탐색 콘텐츠 추가, 시각화 분리 및 콘텐츠·실행 검증 보강 |
| 2026-10-23 | 코드 스타일·주석 정리 |
| 2026-10-25 | 정렬·그래프·문자열·문제 해결 기법 콘텐츠와 시각화 추가, 학습 항목 56개 완료 |
| 2026-10-25 | 실행 예제 번호 표시, 실행·제출 결과의 독립 조회·복구 및 오류 처리 개선 |
| 2026-10-25 | Google 로그인·오늘의 학습·홈 개편·마이페이지·회원탈퇴 요구사항 추가, 지원 언어 확대·배포 일정 재설정 |
| 2026-10-25 | 홈 개편: 오전 6시 기준 공통 학습 추천, 분류·이름 검색, 9개 카드 페이지네이션과 URL 상태 복원 구현 |
| 2026-10-25 | 학습 분류 이름·표시 순서·소속 관계를 DB와 조회 API로 관리하고 상세 페이지의 분류 이동 연결 |
| 2026-10-28 | 분류·실습 콘텐츠·화면 코드의 관리 기준과 향후 관리자 콘텐츠 편집·버전 발행 방향 정리 |
| 2026-10-28 | OAuth 로그인·회원 자동 등록, 로그인 상태별 헤더·기본 회원 화면, 세션 로그아웃 및 CSRF 처리 구현 |
| 2026-10-29 | 비회원 예제 실행 유지, 제출·기록 로그인 제한, 회원별 제출·채점 결과 조회 및 브라우저 결과 복구 구분 |
| 2026-10-29 | 서버 기준 일일 학습 배정, 제출 접수 시각 기준의 회원별 완료 저장·조회, 홈 완료 표시 구현 |
| 2026-11-05 | 마이페이지 오늘 완료·연속·누적 현황, 연도별 잔디와 최초 통과 제출 상세 연결 구현 |
| 2026-11-06 | 회원탈퇴·본인 기록 삭제·세션 만료, 오늘의 학습 진행 상태 및 통과 문구 정리 |

## 문서

| 문서 | 내용 |
| --- | --- |
| [학습 목록](docs/curriculum.md) | 자료구조·알고리즘 분류, 기준 구현, 진행 상태 |
| [API](docs/api.md) | 엔드포인트, 요청·응답, 오류 |
| [아키텍처](docs/architecture.md) | 시스템 구성, 모듈 역할, 실행 환경 |
| [콘텐츠 규격](docs/content.md) | 실습 추가 절차, 파일 구성, 테스트·버전 관리 |
