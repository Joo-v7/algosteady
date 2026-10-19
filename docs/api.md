# API

로컬 API 서버는 `http://localhost:8090`이다. 요청·응답은 JSON이며, 식별자 `{id}`는 아래 API별 설명을 따른다. 지원 언어 값은 `c`다.

## API 목록

| 기능 | Method | URL | 성공 응답 |
| --- | --- | --- | --- |
| 과제 목록 | GET | `/api/exercises` | 200, 과제 요약 배열 |
| 과제 상세 | GET | `/api/exercises/{id}` | 200, 과제 상세 |
| 기준 코드 | GET | `/api/exercises/{id}/reference` | 200, 기준 코드 |
| 공개 예시 실행 | POST | `/api/runs` | 202, `jobId` |
| 전체 채점 제출 | POST | `/api/submissions` | 202, `submissionId`·`jobId` |
| 작업 조회 | GET | `/api/jobs/{id}` | 200, 상태·실행 결과 |
| 제출 목록 | GET | `/api/submissions` | 200, 제출 목록·페이지 정보 |
| 제출 상세 | GET | `/api/submissions/{id}` | 200, 제출 원본·작업 결과 |

## 과제 조회

`{id}`는 과제 ID다. 목록·상세는 최신 등록 버전을 반환한다.

| API | Query | 주요 응답 |
| --- | --- | --- |
| 목록 | 없음 | `id`, `contentVersion`, `title`, `description`, `category`, `languages`, `contract`, `criteria` |
| 상세 | `language` 필수 | `id`, `version`, `title`, `description`, `category`, `language`, `standard`, `learning`, `contract`, `criteria`, `starterCode`, `publicExamples` |
| 기준 코드 | `language`, `contentVersion` 필수 | `exerciseId`, `contentVersion`, `language`, `code` |

예: `GET /api/exercises/stack-array?language=c`

공개 예시는 `id`, `description`, `input`, `expected`를 갖는다. 상세 조회에는 기준 코드·호출 코드·비공개 테스트를 포함하지 않는다.

## 실행·채점 요청

필수 문자열은 공백일 수 없다. `contentVersion`은 조회한 콘텐츠 버전을 사용한다.

**공개 예시 실행 — `POST /api/runs`**

```json
{
  "exerciseId": "stack-array",
  "contentVersion": "1.0.0",
  "language": "c",
  "code": "작성한 코드",
  "exampleId": "public-example"
}
```

`exampleId`는 상세 응답의 공개 예시 ID다. 응답은 `{ "jobId": "UUID" }`다.

**전체 채점 — `POST /api/submissions`**

위 요청에서 `exampleId`를 제외하고 필수 boolean인 `"referenceViewed": false`를 추가한다. 기준 코드를 열람했으면 `true`다.

응답은 `{ "submissionId": "UUID", "jobId": "UUID" }`다. 요청마다 새 제출을 만들고 공개·비공개 전체 테스트를 채점한다.

## 작업 결과 조회

`GET /api/jobs/{id}`의 `{id}`는 작업 UUID다. 진행 중에는 500ms 간격으로 조회하고 완료·실패·통신 오류·화면 이탈 시 중단한다.

| 응답 필드 | 내용 |
| --- | --- |
| `jobId`, `exerciseId`, `exerciseTitle`, `contentVersion`, `language` | 작업·과제 정보 |
| `code`, `exampleId`, `kind` | 실행 원본, 예시 ID, `RUN` 또는 `GRADING` |
| `status`, `message` | 작업 상태와 장애·중단 사유 |
| `queuedAt`, `startedAt`, `completedAt` | 등록·시작·종료 시각, 시작·종료 전에는 해당 값이 null |
| `preparation` | 컴파일 상태·stdout·stderr·종료 코드·메시지, 준비 전 null |
| `tests` | 저장된 테스트 결과 배열 |
| `criteria` | 기준별 상태·테스트 수, 공개 예시 실행은 빈 배열 |

- 작업 상태: `QUEUED` → `RUNNING` → `COMPLETED` 또는 `FAILED`.
- `COMPLETED`는 처리 완료를 뜻하며 정답을 뜻하지 않는다. 컴파일 실패·오답·시간 초과도 결과로 반환한다. 서버·실행 환경 장애는 `FAILED`다.
- 테스트 항목: `testId`, `criterionId`, `description`, `input`, `expected`, `actual`, `passed`.
- `expected`: `stdout`, `exitCode`, `stderrRequired`.
- `actual`: `testId`, `status`, `stdout`, `stderr`, `exitCode`, `executionTimeNanos`, `message`. 미실행 테스트의 종료 코드·시간은 null이다.
- 기준 항목: `criterionId`, `title`, `description`, `status`, `totalTests`, `completedTests`, `passedTests`.
- 기준 상태: `PENDING`(대기), `RUNNING`(검사 중), `PASSED`(통과), `FAILED`(실패), `UNAVAILABLE`(검사 불가).

## 제출 기록 조회

**목록 — `GET /api/submissions`**

| Query | 필수 | 설명 |
| --- | --- | --- |
| `exerciseId` | O | 과제 ID |
| `language` | O | 언어 코드 |
| `page` | X | 0부터 시작, 기본값 0 |
| `size` | X | 1 이상, 기본값 20 |

응답은 `content`, `page`, `size`, `totalElements`, `totalPages`다. 최신 제출부터 반환하고 기록이 없거나 페이지 범위를 벗어나면 `content`는 빈 배열이다.

각 항목은 `submissionId`, `exerciseId`, `contentVersion`, `language`, `referenceViewed`, `submittedAt`, `job`이다. `job`은 `jobId`, `status`, `passedCriteria`, `totalCriteria`를 포함한다.

**상세 — `GET /api/submissions/{id}`**

`{id}`는 제출 UUID다. 목록 항목에 원본 `code`가 추가되고 `job`은 작업 조회와 같은 전체 결과를 반환한다. 코드는 상세 화면에서 직접 복사해 사용한다.

## 오류 응답

| HTTP 상태 | 원인 |
| --- | --- |
| 400 | 필수값·JSON·UUID·페이지 오류, 지원하지 않는 언어, 잘못된 공개 예시 |
| 404 | 없는 과제·콘텐츠 버전·작업·제출 |
| 500 | 예상하지 못한 서버 처리 오류 |

오류 본문은 `{ "message": "오류 설명" }`이다.
