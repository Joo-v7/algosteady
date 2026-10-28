# API

기본 주소는 `http://localhost:8090/api`이며 요청·응답은 JSON이다. 모든 과제는 아래 API를 공유한다. 경로 또는 본문의 `exerciseId`로 과제를 선택하며, 과제 추가 시 API를 따로 만들거나 이 문서에 과제별 항목을 추가하지 않는다.

## 경로와 요청

경로의 `{exerciseId}`는 콘텐츠 ID, `{jobId}`·`{submissionId}`는 UUID다. `language`는 언어 코드이며 지원 값은 `c`다. 아래 Query는 별도 표시가 없으면 필수다.

| 기능 | Method·경로 | Query·본문 | 성공 응답 |
| --- | --- | --- | --- |
| 과제 목록 | `GET /exercises` | 없음 | 200, 과제 요약 배열 |
| 학습 분류 | `GET /exercises/categories` | 없음 | 200, DB 표시 순서의 분류 배열 |
| 과제 상세 | `GET /exercises/{exerciseId}` | Query: `language` | 200, 과제 상세 |
| 기준 코드 | `GET /exercises/{exerciseId}/reference` | Query: `language`, `contentVersion` | 200, 기준 코드 |
| 예제 실행 | `POST /runs` | 본문: 공통 실행 필드 + `exampleId` | 202, `{ "jobId": "UUID" }` |
| 전체 채점 제출 | `POST /submissions` | 본문: 공통 실행 필드 + `referenceViewed` | 202, `{ "submissionId": "UUID", "jobId": "UUID" }` |
| 작업 조회 | `GET /jobs/{jobId}` | 없음 | 200, 작업 결과 |
| 제출 목록 | `GET /submissions` | Query: `exerciseId`, `language`, 선택 `page`·`size` | 200, 제출 페이지 |
| 제출 상세 | `GET /submissions/{submissionId}` | 없음 | 200, 제출 상세 |

### 실행·제출 본문

| 공통 필드 | 타입 | 값의 출처 |
| --- | --- | --- |
| `exerciseId` | string | 선택한 과제의 ID |
| `contentVersion` | string | 조회한 과제 상세의 `version` |
| `language` | string | 선택한 언어 코드 |
| `code` | string | 사용자가 작성한 코드 |

실행은 `exampleId`(string)를 추가한다. 값은 과제 상세의 `publicExamples[].id`에서 선택한다. 제출은 대신 `referenceViewed`(boolean)를 추가한다. 기준 코드를 열람했으면 `true`, 아니면 `false`다. 모든 본문 필드는 필수이며 문자열은 공백일 수 없다.

호출 코드·입력·기대값은 서버가 해당 콘텐츠에서 선택한다. 제출할 때마다 새 제출과 전체 테스트 채점 작업을 생성한다.

## 조회 응답 구조

| 응답 | 필드·구조 |
| --- | --- |
| 과제 요약 | `id`, `contentVersion`, `title`, `description`, `category`, `languages`, `contract`, `criteria` |
| 학습 분류 | `id`, `label`, `categories`(소속 콘텐츠 분류 코드 배열). 이름·순서·소속 관계는 DB에서 관리 |
| 과제 상세 | `id`, `version`, `title`, `description`, `category`, `language`, `standard`, `learning`, `contract`, `criteria`, `starterCode`, `publicExamples` |
| 기준 코드 | `exerciseId`, `contentVersion`, `language`, `code` |
| 제출 페이지 | `content`(제출 요약 배열), `page`, `size`, `totalElements`, `totalPages` |
| 제출 요약 | `submissionId`, `exerciseId`, `contentVersion`, `language`, `referenceViewed`, `submittedAt`, `job`(작업 요약) |
| 작업 요약 | `jobId`, `status`, `passedCriteria`, `totalCriteria` |
| 제출 상세 | 제출 요약과 같은 제출 메타데이터에 `code` 추가. `job`은 아래의 전체 작업 결과 |

과제 목록·상세는 최신 등록 버전을 반환하고, 기준 코드는 지정한 버전을 반환한다. 상세에는 기준 코드·호출 코드·비공개 테스트를 포함하지 않는다. `publicExamples`는 `id`, `description`, `input`, `expected`를 가진다. 콘텐츠 내부 구조와 기대값 규격은 [콘텐츠 규격](content.md)을 따른다.

제출 목록은 최신순이며 `page`는 0부터 시작한다. 기본값은 `page=0`, `size=20`이고 `size`는 1 이상이다. 기록이 없거나 페이지 범위를 벗어나면 `content`는 빈 배열이다.

## 작업 결과 구조

예제 실행·전체 채점·제출 상세는 같은 작업 결과 구조를 사용한다.

| 필드 | 내용 |
| --- | --- |
| `jobId`, `exerciseId`, `exerciseTitle`, `contentVersion`, `language` | 작업·과제 정보 |
| `code`, `exampleId`, `kind` | 실행 원본, 예제 ID, `RUN` 또는 `GRADING` |
| `status`, `message` | 작업 상태와 장애·중단 사유 |
| `queuedAt`, `startedAt`, `completedAt` | 등록·시작·종료 시각. 시작·종료 전에는 해당 값이 null |
| `preparation` | 컴파일 상태·stdout·stderr·종료 코드·메시지. 준비 전 null |
| `tests` | 테스트 결과 배열: `testId`, `criterionId`, `description`, `input`, `expected`, `actual`, `passed` |
| `criteria` | 기준별 결과 배열: `criterionId`, `title`, `description`, `status`, `totalTests`, `completedTests`, `passedTests`. 예제 실행은 빈 배열 |

`expected`는 `stdout`, `exitCode`, `stderrRequired`다. `actual`은 `testId`, `status`, `stdout`, `stderr`, `exitCode`, `executionTimeNanos`, `message`다. 미실행 테스트의 종료 코드·시간은 null이며, 실행 시간은 나노초로 반환한다. 화면에서는 초로 표시하고 제출 결과의 실행 시간은 테스트 실행 시간의 합계로 표시한다.

- 작업 상태: `QUEUED` → `RUNNING` → `COMPLETED` 또는 `FAILED`.
- `COMPLETED`는 처리 완료를 뜻한다. 컴파일 실패·오답·시간 초과도 결과로 반환하며, 서버·실행 환경 장애는 `FAILED`다.
- 기준 상태: `PENDING`(대기), `RUNNING`(검사 중), `PASSED`(통과), `FAILED`(실패), `UNAVAILABLE`(검사 불가).
- 화면은 진행 중인 작업을 500ms 간격으로 조회하고 완료·실패·통신 오류·화면 이탈 시 중단한다.

## 오류 응답

공통 본문은 `{ "message": "오류 설명" }`이다.

| HTTP 상태 | 원인 |
| --- | --- |
| 400 | 필수값·JSON·UUID·페이지 오류, 지원하지 않는 언어, 잘못된 공개 예제 |
| 404 | 없는 과제·콘텐츠 버전·작업·제출 |
| 500 | 예상하지 못한 서버 처리 오류 |
