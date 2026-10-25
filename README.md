# Algosteady

자료구조와 알고리즘을 직접 구현하고, 실행·채점 결과로 확인하는 학습 웹 프로젝트.

## 주요 기능

- 과제 목록·학습 내용·구현 규격·기준 코드 조회
- Monaco 편집기로 코드 작성 및 브라우저 초안 저장
- 공개 예시 실행과 전체 테스트 채점
- 컴파일 오류·실행 출력·실행 시간·기준별 판정 확인
- 제출 기록 목록·상세 조회 및 코드 확인

## 개발 계획

배열 스택으로 조회·구현·실행·채점·기록 흐름을 연결한 뒤, 학습 편의 기능과 과제를 확장하고 다른 언어를 추가한다.

| 목표 기간 | 개발 항목 | 목표 |
| --- | --- | --- |
| 2026-10-12 ~ 10-14 | 프로젝트 초기 구성·설계 | React·Spring Boot·DB 구성, 과제·실행·저장 구조 설계 |
| 2026-10-15 ~ 10-19 | 핵심 학습 흐름 | 학습 코드 조회, 코드 작성, Docker 실행·전체 채점, 제출 기록 연결 |
| 2026-10-20 ~ 10-23 | 실습 화면·편집기 개선 | 실행 시간 표시, Monaco 문법 강조·들여쓰기 및 브라우저 초안 저장 연결 |
| 2026-10-21 ~ 10-25 | 과제 확대 | [학습 목록](docs/curriculum.md)의 C17 콘텐츠 56개 구현 (2026-10-25 조기 완료, 기존 목표 11/01~11/15에서 일정 재설정) |
| 2026-10-26 ~ 10-30 | Google 로그인·개인 기록 | 별도 가입 화면 없이 OAuth 로그인, 회원별 제출·실행 기록 연결 (2026-10-25 추가) |
| 2026-10-31 ~ 11-03 | 오늘의 학습 | 회원별 하루 한 개 랜덤 배정, 전체 채점 통과 시 당일 완료 인정 (2026-10-25 추가) |
| 2026-11-04 ~ 11-06 | 홈 개편 | 오늘의 학습 카드, 분류·이름 검색, 학습 카드 3열 배치 (2026-10-25 추가) |
| 2026-11-07 ~ 11-12 | 마이페이지 | 학습 현황, 잔디형 완료 기록, 날짜별 제출 기록 연결 (2026-10-25 추가) |
| 2026-11-13 ~ 11-15 | 회원탈퇴 | 마이페이지 하단 탈퇴 버튼, 데이터 처리 안내·최종 확인 (2026-10-25 추가) |
| 2026-11-16 ~ 12-06 | 지원 언어 확대 | Python·C++·Java 실행 이미지와 과제 코드·테스트 연결 (2026-10-25 일정 재설정) |
| 2026-12-07 ~ 12-13 | 서버 배포·자동화 | 서비스 배포, 빌드·테스트·배포 CI/CD 구성 (2026-10-25 일정 재설정) |

추가 기능의 화면 구성과 확인할 정책은 [추가 기능·화면 계획](docs/architecture.md#추가-기능화면-계획)에서 관리한다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| 프론트엔드 | React, TypeScript, Vite, Monaco Editor |
| 백엔드 | Java 21, Spring Boot, Spring Data JPA |
| 데이터베이스 | PostgreSQL 18, Flyway |
| 실행 환경 | 언어별 Docker 이미지, 초기 지원 C17 |
| 개발 도구 | OpenAI Codex |

## 로컬 실행

Node.js 24, JDK 21, Docker를 준비한다. 아래 명령은 저장소 루트에서 시작한다.

**1. 환경변수·DB·실행 이미지 준비**

`.env`가 없으면 `.env.example`을 복사하고 `POSTGRES_PASSWORD`를 설정한다. 기존 `.env`는 유지한다.

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

IDE 실행 시에도 JDK 21과 `.env`의 `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`를 실행 설정에 등록한다. 서버는 Flyway로 DB 구조를 적용하고 과제 콘텐츠를 등록한다.

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

Vite가 `/api` 요청을 API 서버로 전달한다. 종료할 때 각 서버를 중지하고 루트에서 `docker compose stop postgres`를 실행한다.

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
./mvnw '-Dtest=JudgingApiPostgresTests,SubmissionQueryPostgresTests' -Dalgosteady.postgres.tests=true test
./mvnw '-Dtest=CodeExecutionDockerTests,ExerciseContentDockerTests' -Dalgosteady.docker.tests=true test
./mvnw -DskipTests package
```

## 개발 이력

| 날짜 | 내용 |
| --- | --- |
| 2026-10-12 | 프로젝트 생성 및 프론트엔드 초기 구성 |
| 2026-10-14 | 프로젝트 설계·개발 계획 문서 작성 |
| 2026-10-19 | 과제 콘텐츠 조회, Docker 실행부, 공개 예시 실행·전체 채점, 제출 기록 연결 |
| 2026-10-21 | 재귀·스택·큐·덱·연결 리스트 콘텐츠 추가, Monaco 적용 및 학습·채점 화면 개선 |
| 2026-10-22 | 트리·힙·해싱·유니온 파인드·이진 탐색 콘텐츠 추가, 시각화 분리 및 콘텐츠·실행 검증 보강 |
| 2026-10-23 | 코드 스타일·주석 정리 |
| 2026-10-25 | 정렬·그래프·문자열·문제 해결 기법 콘텐츠와 시각화 추가, 학습 항목 56개 완료 |
| 2026-10-25 | 실행 예제 번호 표시, 실행·제출 결과의 독립 조회·복구 및 오류 처리 개선 |
| 2026-10-25 | Google 로그인·오늘의 학습·홈 개편·마이페이지·회원탈퇴 요구사항 추가, 지원 언어 확대·배포 일정 재설정 |

## 문서

| 문서 | 내용 |
| --- | --- |
| [학습 목록](docs/curriculum.md) | 자료구조·알고리즘 분류, 기준 구현, 진행 상태 |
| [API](docs/api.md) | 엔드포인트, 요청·응답, 오류 |
| [아키텍처](docs/architecture.md) | 시스템 구성, 모듈 역할, 실행 환경 |
| [콘텐츠 규격](docs/content.md) | 과제 추가 절차, 파일 구성, 테스트·버전 관리 |
