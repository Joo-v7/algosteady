# API

로컬 기본 주소는 `http://localhost:9090/api`다. 아래 표의 경로를 기본 주소 뒤에 붙이며, 본문은 JSON이다. 204 응답은 본문이 없다. 실습마다 API를 추가하지 않고 `exerciseId`로 콘텐츠를 선택한다.

## 엔드포인트

| 기능 | Method·경로 | 접근 | 입력 | 성공 응답 |
| --- | --- | --- | --- | --- |
| 로그인 수단 | `GET /auth/providers` | 공개 | 없음 | 200, 제공자 배열 |
| 현재 회원 | `GET /auth/me` | 로그인 | 없음 | 200, `id`, `name`, `email` |
| CSRF 토큰 | `GET /auth/csrf` | 공개 | 없음 | 200, `headerName`, `token` |
| 로그아웃 | `POST /auth/logout` | 현재 세션 | CSRF 헤더 | 204 |
| 회원탈퇴 | `DELETE /auth/me` | 로그인 | CSRF 헤더 | 204 |
| 학습 분류 | `GET /exercises/categories` | 공개 | 없음 | 200, 분류 배열 |
| 학습 목록 | `GET /exercises` | 공개 | 없음 | 200, 학습 항목 요약 배열 |
| 학습 상세 | `GET /exercises/{exerciseId}` | 공개 | Query: `language` | 200, 상세 정보 |
| 기준 코드 | `GET /exercises/{exerciseId}/reference` | 공개 | Query: `language`, `contentVersion` | 200, 기준 코드 |
| 오늘의 학습 | `GET /learning/today` | 공개 | 없음 | 200, 배정·본인 상태 |
| 학습 현황·기록 | `GET /learning/history` | 로그인 | 선택 Query: `year` | 200, 현황·연도별 완료 기록 |
| 예제 실행 | `POST /runs` | 공개 | 실행 본문·CSRF 헤더 | 202, `jobId` |
| 제출 | `POST /submissions` | 로그인 | 제출 본문·CSRF 헤더 | 202, `submissionId`, `jobId` |
| 작업 조회 | `GET /jobs/{jobId}` | 작업 유형별 | 없음 | 200, 작업 결과 |
| 제출 목록 | `GET /submissions` | 로그인 | Query: `exerciseId`, `language`, 선택 `page`·`size` | 200, 본인 제출 페이지 |
| 제출 상세 | `GET /submissions/{submissionId}` | 로그인 | 없음 | 200, 본인 제출·채점 결과 |

`jobId`·`submissionId`는 UUID다. 현재 `language` 지원 값은 `c`이며, 선택으로 표시하지 않은 Query는 필수다.

## 인증과 변경 요청

Google OAuth 로그인과 `ALGOSTEADY_SESSION` 쿠키로 인증을 유지한다. 쿠키는 HttpOnly·SameSite=Lax다. 로그인 수단 응답은 `id`, `name`, `authorizationUrl`을 가진다.

`authorizationUrl`은 `/oauth2/authorization/{registrationId}`이며 API 기본 주소에 붙이지 않는다. 브라우저 전체 이동으로 로그인을 시작하고 `/login/oauth2/code/{registrationId}`로 콜백을 받는다. 성공 시 홈, 실패·취소 시 `/login?error=oauth`로 이동한다.

POST·DELETE 요청은 먼저 `GET /auth/csrf`로 받은 `headerName`과 `token`을 요청 헤더에 넣는다. 비로그인 예제 실행도 CSRF 토큰이 필요하다.

회원 ID는 인증 세션에서 결정한다. 요청 본문이나 Query로 다른 회원을 지정할 수 없다. 예제 실행 작업은 공개 조회하며, 전체 채점 작업은 본인만 조회한다. 채점 작업에 비로그인으로 접근하면 401, 다른 회원 또는 소유자가 없는 기존 작업에 접근하면 404다.

로그아웃은 현재 세션을 종료한다. 탈퇴는 계정·본인 제출 코드·채점 결과·학습 기록을 삭제하고 로그인 세션을 만료한다. 같은 제공자 계정으로 다시 로그인하면 기록이 없는 새 회원으로 등록한다.

## 실행·제출 본문

| 공통 필드 | 타입 | 의미 |
| --- | --- | --- |
| `exerciseId` | string | 선택한 실습 ID |
| `contentVersion` | string | 학습 상세에서 받은 `version` |
| `language` | string | 구현 언어 코드 |
| `code` | string | 사용자가 작성한 코드 |

- 실행은 `exampleId`(string)를 추가한다. 상세의 `publicExamples[].id`에서 선택한다.
- 제출은 `referenceViewed`(boolean)를 추가한다. 기준 코드 열람 여부이며, 열람하지 않은 경우 `false`다.

모든 본문 필드는 필수이며 문자열은 공백일 수 없다. 입력·기대값·호출 코드는 서버가 등록된 콘텐츠에서 선택한다. 제출할 때마다 새 제출 원본과 전체 채점 작업을 저장한다.

## 오늘의 학습·완료 기록

오늘의 학습 응답은 `learningDate`, `exerciseId`, `completed`, `attempted`다. 날짜는 한국 시간 오전 6시 기준이며 `YYYY-MM-DD` 형식이다. 날짜별 최초 배정을 DB에 보존한다.

| 상태 | 의미 |
| --- | --- |
| `attempted` | 해당 학습 날짜에 배정된 실습의 제출 기록이 있음 |
| `completed` | 해당 날짜의 배정 실습 전체 채점을 통과함 |

비로그인은 두 값 모두 `false`다. 화면은 `completed`를 우선해 `완료`, 미완료 제출이 있으면 `진행 중`으로 표시한다.

완료 날짜는 **제출 접수 시각**으로 결정한다. 오전 6시 전에 접수하고 이후 채점이 끝나도 이전 학습 날짜로 인정한다. 회원·날짜별 최초 완료만 유지하며, 예제 실행·기준 코드 열람·다른 실습 통과는 일일 완료로 인정하지 않는다.

`GET /learning/history?year=2026`은 본인의 학습 현황과 해당 연도의 완료 날짜를 반환한다. `year` 생략 시 현재 학습 연도이며, 1부터 현재 학습 연도까지 허용한다.

| 필드 | 의미 |
| --- | --- |
| `learningDate`, `completedToday` | 서버 학습 날짜·오늘 완료 여부 |
| `currentStreak` | 오늘 완료 시 오늘부터, 미완료 시 어제부터 이어지는 연속 완료 일수 |
| `totalCompletedDays` | 선택 연도와 관계없는 오늘까지의 누적 완료 일수 |
| `year` | 조회 연도 |
| `days` | 완료 날짜 `learningDate`와 최초 통과 제출 `submissionId` 배열 |

미완료 날짜는 `days`에 포함하지 않는다. 연결된 제출이 없으면 `submissionId`는 null이다. 연도 변경은 누적·연속 일수에 영향을 주지 않는다.

## 콘텐츠·제출 응답

| 응답 | 필드 |
| --- | --- |
| 학습 분류 | `id`, `label`, `categories`(소속 콘텐츠 분류 코드 배열) |
| 학습 항목 요약 | `id`, `contentVersion`, `title`, `description`, `category`, `languages`, `contract`, `criteria` |
| 학습 상세 | `id`, `version`, `title`, `description`, `category`, `language`, `standard`, `learning`, `contract`, `criteria`, `starterCode`, `publicExamples` |
| 기준 코드 | `exerciseId`, `contentVersion`, `language`, `code` |
| 제출 페이지 | `content`(제출 요약 배열), `page`, `size`, `totalElements`, `totalPages` |
| 제출 요약 | `submissionId`, `exerciseId`, `contentVersion`, `language`, `referenceViewed`, `submittedAt`, `job` |
| 작업 요약 | `jobId`, `status`, `passedCriteria`, `totalCriteria` |
| 제출 상세 | 제출 메타데이터와 `code`, 전체 작업 결과 `job` |

목록·상세는 실습별 최신 등록 버전을 반환하고 기준 코드는 지정한 버전을 반환한다. 상세에는 기준 코드·호출 코드·비공개 테스트가 없으며, `publicExamples`는 `id`, `description`, `input`, `expected`를 가진다.

학습 목록 API는 전체 배열을 반환하며 홈 검색·페이지 분할은 프론트에서 처리한다. 제출 목록은 DB에서 최신순 페이지 조회한다. 제출의 `page`는 0부터 시작하고 기본값은 `page=0`, `size=20`이다. `size`는 1~20만 허용하며 범위를 벗어나면 400을 반환한다. 결과가 없거나 페이지 범위를 벗어나면 `content`는 빈 배열이다.

## 작업 결과

예제 실행·전체 채점은 같은 작업 결과 구조를 사용한다.

| 필드 | 내용 |
| --- | --- |
| `jobId`, `exerciseId`, `exerciseTitle`, `contentVersion`, `language` | 작업·실습 정보 |
| `code`, `exampleId`, `kind` | 실행 원본·예제 ID·`RUN` 또는 `GRADING`. 제출의 `exampleId`는 null |
| `status`, `message` | 작업 상태와 장애·중단 사유 |
| `queuedAt`, `startedAt`, `completedAt` | 등록·시작·종료 시각. 시작·종료 전에는 null |
| `preparation` | 컴파일 상태·stdout·stderr·종료 코드·메시지. 준비 전 null |
| `tests` | 저장된 테스트 결과 배열: `testId`, `criterionId`, `description`, `input`, `expected`, `actual`, `passed` |
| `criteria` | 기준별 배열: `criterionId`, `title`, `description`, `status`, `totalTests`, `completedTests`, `passedTests`. 예제 실행은 빈 배열 |

`expected`는 `stdout`, `exitCode`, `stderrRequired`다. `actual`은 `testId`, `status`, `stdout`, `stderr`, `exitCode`, `executionTimeNanos`, `message`다. 실행 시간은 나노초이며 미실행 테스트의 종료 코드·시간은 null이다.

- 작업 상태: `QUEUED` → `RUNNING` → `COMPLETED` 또는 `FAILED`.
- `COMPLETED`는 처리 완료를 뜻한다. 컴파일 실패·오답·시간 초과도 포함하며 서버·실행 환경 장애는 `FAILED`다.
- 기준 상태: `PENDING`, `RUNNING`, `PASSED`, `FAILED`, `UNAVAILABLE`.
- 프론트는 500ms 간격으로 조회하고 완료·실패·통신 오류·화면 이탈 시 중단한다.

## 오류 응답

본문은 `{ "message": "오류 설명" }`이다.

| 상태 | 원인 |
| --- | --- |
| 400 | 필수값·JSON·UUID·페이지·연도 오류, 지원하지 않는 언어, 잘못된 공개 예제 |
| 401 | 인증 필요 또는 만료·탈퇴한 회원의 인증 |
| 403 | 접근 거부·CSRF 검증 실패 |
| 404 | 없는 실습·콘텐츠 버전·작업·제출 또는 본인 소유가 아닌 제출·채점 작업 |
| 500 | 예상하지 못한 서버 처리 오류 |
