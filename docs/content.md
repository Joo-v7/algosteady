# 콘텐츠 규격

## 파일 구성

콘텐츠 원본은 `content/`의 파일로 관리한다. 공통 설명·실습 규격과 언어별 코드·테스트를 분리한다.

```text
content/<실습-id>/
├── exercise.json
└── <언어>/
    ├── starter.*
    ├── reference.*
    ├── harness.*
    └── tests.json
```

| 파일 | 역할 |
| --- | --- |
| `exercise.json` | 제목·설명·분류·학습 단계·실습 규격·채점 기준·언어별 파일 경로 |
| `starter.*` | 사용자에게 제공하는 타입·함수 선언 |
| `reference.*` | 전체 테스트를 통과하는 기준 코드 |
| `harness.*` | 테스트 입력 해석, 사용자 함수 호출, 결과 출력 |
| `tests.json` | 입력·기대 결과·연결 기준·공개 예제 여부 |

## 실습 추가 절차

1. [학습 목록](curriculum.md)의 분류에 맞춰 항목과 함수·동작 규격을 정한다.
2. 콘텐츠 파일을 작성하고 채점 기준에 테스트를 연결한다.
3. 등록 검증과 기준 코드·대표 오답 검증을 수행한다.
4. 학습·실습·실행·제출·기록 조회를 확인하고 학습 목록에 링크를 추가한다.

새로운 시각화가 필요하면 프론트 타입·화면도 함께 추가한다.

공통 테스트의 대상·버전은 [ExerciseContentSource](../backend/src/test/java/com/algosteady/support/ExerciseContentSource.java)가 콘텐츠 파일에서 자동으로 수집한다.

| 검증 | 참고 파일 |
| --- | --- |
| 콘텐츠 등록·조회·공개 범위 | [ExerciseContentTests.java](../backend/src/test/java/com/algosteady/exercise/ExerciseContentTests.java) |
| API 바인딩·DB 저장 | [JudgingApiPostgresTests.java](../backend/src/test/java/com/algosteady/judging/JudgingApiPostgresTests.java) |
| 실제 C 실행 | [ExerciseContentDockerTests.java](../backend/src/test/java/com/algosteady/judging/runner/service/ExerciseContentDockerTests.java) |

## 메타데이터

| 항목 | 내용 |
| --- | --- |
| `id`, `version` | 실습 ID와 콘텐츠 버전 |
| `title`, `description`, `category`, `kind` | 제목·설명·분류·유형 |
| `learning` | 개념·구조·연산 과정과 시각화 상태 |
| `contract` | 타입·함수·입력 범위·정상 및 오류 동작 |
| `prerequisites` | 선행 실습 ID |
| `criteria` | 채점 기준 ID·제목·설명 |
| `languages` | 언어별 표준·시작 코드·기준 코드·호출 코드·테스트 경로 |

각 실습의 구체적인 함수·상태 필드·수치는 해당 콘텐츠에서 관리한다. 학습 단계는 비어 있을 수 없으며, 등록 시 시각화별 필수 필드·인덱스·연결 구조를 검증한다.

## 테스트 규격

| 항목 | 내용 |
| --- | --- |
| `id`, `criterionId` | 테스트 ID와 연결된 채점 기준 ID |
| `public` | 공개 예제 여부 |
| `description`, `input` | 검사 목적과 표준 입력 |
| `expected.stdout` | 기대 표준 출력 |
| `expected.exitCode` | 기대 종료 코드 |
| `expected.stderrRequired` | 공백이 아닌 오류 출력이 필요한지 여부 |

각 기준에 연결된 모든 테스트가 통과해야 해당 기준을 통과한다. 정상·경계·오류·반복 사용을 검사하며, 예제 실행과 전체 채점은 구분한다.

표준 출력은 줄바꿈 형식을 통일하고 마지막 줄바꿈 한 개의 유무를 제외해 비교한다. 종료 코드와 오류 출력 존재 여부도 함께 비교한다.

## 등록·버전 관리

서버 시작 시 Maven 리소스에 포함된 콘텐츠 파일을 검증하고 `exercise_content`에 JSONB 스냅샷으로 등록한다. 파일 경로는 실습 폴더 기준 상대 경로이며, 언어별 경로는 `languages`에서 지정한다. 조회·실행·채점은 DB에 등록된 스냅샷을 사용한다.

등록된 같은 버전은 덮어쓰지 않는다. 설명·학습 내용·코드·테스트·채점 기준을 변경하면 버전을 올린다. 기존 버전과 내용이 다르면 서버 시작이 실패하며, 이전 제출은 평가 당시 버전을 참조한다.

## 관리 기준

| 대상 | 관리 위치 |
| --- | --- |
| 학습 설명·실습 규격·코드·테스트·채점 기준 | 콘텐츠 파일에서 편집하고 버전별로 DB 등록 |
| 분류 이름·표시 순서 | `exercise_category` |
| 목록 분류와 콘텐츠 `category`의 연결 | `exercise_category_member` |
| 화면 배치·시각화 방식 | React 컴포넌트·CSS |

분류 추가·변경은 Flyway 마이그레이션으로 반영한다. 콘텐츠의 `category`는 학습 화면·검증에서 사용하는 코드이며, 새로운 표현이 필요하면 해당 코드의 지원도 함께 맞춘다.
