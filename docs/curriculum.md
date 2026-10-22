# 학습 목록

자료구조와 알고리즘의 기준 코드를 이해하고 반복해서 직접 구현한다. 학습은 개념 설명 → 기준 코드 → 직접 구현 → 실행·제출 순서로 구성한다. 대표 문제가 필요한 기법은 짧은 문제 설명과 함께 기준 구현을 제공한다.

완료한 학습 항목은 체크하고 콘텐츠 링크를 연결한다.

## 재귀

- [x] [팩토리얼](../content/recursion-factorial/exercise.json)
- [x] [거듭제곱](../content/recursion-power/exercise.json)
- [x] [피보나치](../content/recursion-fibonacci/exercise.json)
- [x] [하노이탑](../content/recursion-hanoi/exercise.json)

## 스택

- [x] [고정 배열 스택](../content/stack-array/exercise.json)
- [x] [동적 배열 스택](../content/stack-dynamic/exercise.json)
- [x] [연결 리스트 스택](../content/stack-linked/exercise.json)

## 큐

- [x] [선형 큐](../content/queue-linear/exercise.json)
- [x] [원형 큐](../content/queue-circular/exercise.json)
- [x] [연결 리스트 큐](../content/queue-linked/exercise.json)

## 덱

- [x] [원형 배열 덱](../content/deque-circular/exercise.json)
- [x] [이중 연결 리스트 덱](../content/deque-linked/exercise.json)

## 연결 리스트

- [x] [단일 연결 리스트](../content/list-singly/exercise.json)
- [x] [원형 연결 리스트](../content/list-circular/exercise.json)
- [x] [이중 연결 리스트](../content/list-doubly/exercise.json)

## 트리

- [x] [이진 트리 구성](../content/tree-binary/exercise.json)
- [x] [전위 순회](../content/tree-preorder/exercise.json)
- [x] [중위 순회](../content/tree-inorder/exercise.json)
- [x] [후위 순회](../content/tree-postorder/exercise.json)
- [x] [레벨 순회](../content/tree-level-order/exercise.json)
- [x] [이진 탐색 트리 — 탐색·삽입·삭제](../content/tree-bst/exercise.json)

## 힙

- [x] [최대 힙 — 삽입·삭제](../content/heap-max/exercise.json)
- [x] [최소 힙 — 삽입·삭제](../content/heap-min/exercise.json)

## 해싱

- [x] [체이닝 — 삽입·검색·삭제](../content/hash-chaining/exercise.json)

## 집합

- [x] [유니온 파인드 — 집합 찾기·합치기, 경로 압축](../content/set-union-find/exercise.json)

## 정렬

- [ ] 선택 정렬
- [ ] 버블 정렬
- [ ] 삽입 정렬
- [ ] 병합 정렬
- [ ] 퀵 정렬
- [ ] 힙 정렬

## 탐색

- [x] [이진 탐색 - 반복](../content/search-binary-iterative/exercise.json)
- [x] [이진 탐색 - 재귀](../content/search-binary-recursive/exercise.json)

## 그래프

- [ ] 인접 행렬
- [ ] 인접 리스트
- [ ] 깊이 우선 탐색(DFS)
- [ ] 너비 우선 탐색(BFS)
- [ ] 크루스칼 — 최소 신장 트리
- [ ] 프림 — 최소 신장 트리
- [ ] 다익스트라 — 최단 경로
- [ ] 벨만–포드 — 최단 경로
- [ ] 플로이드–워셜 — 모든 정점 쌍의 최단 경로
- [ ] 위상 정렬 — 진입 차수 기반

## 문자열

- [ ] 단순 문자열 검색 (Brute Force)
- [ ] 문자열 파싱 및 기본 변환 (분리·결합·뒤집기·정렬)
- [ ] 트라이 — 삽입·검색·삭제

## 문제 해결 기법

- [ ] 완전 탐색 — 순열 생성
- [ ] 완전 탐색 — 조합 생성
- [ ] 완전 탐색 — 부분집합 생성
- [ ] 백트래킹 — N-Queen
- [ ] 그리디 — 활동 선택
- [ ] 동적 계획법 — 피보나치의 메모이제이션·타뷸레이션
- [ ] 동적 계획법 — 0/1 배낭
- [ ] 동적 계획법 — 최장 공통 부분 수열(LCS)
- [ ] 투 포인터 — 정렬된 배열에서 합이 목표값인 두 원소 찾기
- [ ] 슬라이딩 윈도 — 길이가 K인 연속 구간의 최대 합

병합·퀵 정렬도 분할 정복에 해당하며 [정렬](#정렬)에서 관리한다. 재귀 피보나치와 동적 계획법 피보나치는 서로 다른 구현으로 비교한다.

## 관리 기준

- 새 과제는 해당 분류의 항목을 기준으로 추가하고, 검증 완료 후 체크하고 콘텐츠 링크를 연결한다.
- 같은 구현은 한 곳에서 관리하고 관련 기법은 링크로 연결한다. 시간·공간 복잡도는 각 구현의 학습 내용에 포함한다.
- 과제 추가 방법은 [콘텐츠 규격](content.md#과제-추가-절차)을 따른다. API·콘텐츠 문서에는 공통 계약이 바뀔 때만 반영한다.
