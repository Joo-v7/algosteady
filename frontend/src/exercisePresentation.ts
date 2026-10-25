import type { ExerciseCategory, ExerciseSummary } from './api/exercises.ts'

const categories = {
    'sliding-window': {
        label: '슬라이딩 윈도',
        introduction: '고정 길이의 연속 구간을 옮기며 슬라이딩 윈도의 기본 코드를 익혀보세요.',
        family: 'ALGORITHMS',
        note: '길이 K 고정 · 연속 구간 · 최대 합',
        tag: 'Sliding Window',
        detail: '이전 합 − 빠지는 값 + 들어오는 값',
        listTag: '정수 배열 · 길이 K의 최대 구간 합',
        simulationTitle: '구간이 이동하며 합이 바뀌는 과정을 보세요'
    },
    'two-pointer': {
        label: '투 포인터',
        introduction: '정렬 순서와 두 위치의 이동을 따라가며 투 포인터의 기본 코드를 익혀보세요.',
        family: 'ALGORITHMS',
        note: '정렬된 배열 · 양 끝 포인터 · 목표 합',
        tag: 'Two Pointers',
        detail: '작으면 left 증가 · 크면 right 감소',
        listTag: '정렬된 배열 · 목표 합의 두 인덱스',
        simulationTitle: '합에 따라 두 포인터를 이동해 보세요'
    },
    'dp': {
        label: '동적 계획법',
        introduction: '부분 문제의 답을 저장하고 재사용하며 동적 계획법을 익혀보세요.',
        family: 'ALGORITHMS',
        note: '메모이제이션 · 타뷸레이션',
        tag: 'Dynamic Programming',
        detail: '기저값 · 저장 · 재사용',
        listTag: '하향식 메모 · 상향식 표 채우기',
        simulationTitle: '저장과 재사용 순서를 비교해 보세요'
    },
    'greedy': {
        label: '그리디',
        introduction: '지금의 선택이 다음 선택에 남기는 여유를 살펴보며 그리디를 익혀보세요.',
        family: 'ALGORITHMS',
        note: '종료 시각 정렬 · 겹치지 않는 활동',
        tag: 'Greedy',
        detail: '빠른 종료 · 최대 선택 개수',
        listTag: '종료 시각순 · 비겹침 선택',
        simulationTitle: '종료 시각과 선택 과정을 따라가 보세요'
    },
    'backtracking': {
        label: '백트래킹',
        introduction: '조건을 검사해 불가능한 분기를 제외하며 백트래킹의 선택·복구를 익혀보세요.',
        family: 'ALGORITHMS',
        note: '충돌 검사 · 가지치기 · 선택 복구',
        tag: 'Backtracking',
        detail: '가능한 분기만 재귀 탐색',
        listTag: '조건 검사 · 재귀 · 상태 복구',
        simulationTitle: '충돌 검사와 선택 복구를 따라가 보세요'
    },
    'exhaustive': {
        label: '완전 탐색',
        introduction: '모든 선택을 탐색하고 재귀 복귀 과정을 따라가며 완전 탐색을 익혀보세요.',
        family: 'ALGORITHMS',
        note: '선택 · 재귀 호출 · 상태 복구',
        tag: 'Exhaustive Search',
        detail: '분기 · 종료 조건 · 결과 생성',
        listTag: '재귀 · 모든 경우 생성',
        simulationTitle: '선택과 복귀를 따라가 보세요'
    },
    'string': {
        label: '문자열',
        introduction: '문자열의 비교와 변환 과정을 따라가며 기본 코드를 익혀보세요.',
        family: 'ALGORITHMS',
        note: 'ASCII · 널 종료 문자열',
        tag: 'String',
        detail: '문자 비교 · 토큰 · 널 종료',
        listTag: 'ASCII · 문자 배열',
        simulationTitle: '문자와 토큰의 변화를 따라가 보세요'
    },
    'trie': {
        label: '트라이',
        introduction: '접두사 경로를 공유하는 트라이의 삽입·검색·삭제를 익혀보세요.',
        family: 'DATA STRUCTURES',
        note: '소문자 a~z · 자식 포인터 26개',
        tag: 'Trie',
        detail: '공유 접두사 · 단어 끝 표시',
        listTag: '접두사 트리 · 동적 메모리',
        simulationTitle: '문자 경로와 단어 끝 표시를 따라가 보세요'
    },
    'stack': {
        'label': '스택',
        'introduction': '쌓고, 꺼내고, 다시 쌓으며 스택의 원리를 익혀보세요.',
        'family': 'DATA STRUCTURES',
        'note': '',
        'tag': 'Last In, First Out',
        'detail': '삽입과 삭제는 맨 위에서',
        'listTag': '',
        'simulationTitle': '스택은 이렇게 움직여요'
    },
    'queue': {
        'label': '큐',
        'introduction': '뒤에 넣고, 앞에서 꺼내며 큐의 원리를 익혀보세요.',
        'family': 'DATA STRUCTURES',
        'note': '',
        'tag': 'First In, First Out',
        'detail': '뒤에서 삽입 · 앞에서 삭제',
        'listTag': '',
        'simulationTitle': '큐는 이렇게 움직여요'
    },
    'deque': {
        'label': '덱',
        'introduction': '앞과 뒤 양쪽에서 넣고 꺼내며 덱의 원리를 익혀보세요.',
        'family': 'DATA STRUCTURES',
        'note': '',
        'tag': 'Double-ended Queue',
        'detail': '양쪽 끝에서 삽입 · 삭제 · 조회',
        'listTag': '',
        'simulationTitle': '덱은 이렇게 움직여요'
    },
    'linked-list': {
        'label': '연결 리스트',
        'introduction': '노드를 연결하고 끊으며 포인터와 메모리 관리를 익혀보세요.',
        'family': 'DATA STRUCTURES',
        'note': '노드 · 포인터 · 동적 메모리',
        'tag': 'Singly Linked List',
        'detail': 'head에서 link를 따라 순회',
        'listTag': '노드 · 동적 메모리',
        'simulationTitle': '노드는 이렇게 연결돼요'
    },
    'recursion': {
        'label': '재귀',
        'introduction': '종료 조건과 호출·복귀를 익히고 직접 구현해 보세요.',
        'family': 'ALGORITHMS',
        'note': '종료 조건 · 재귀 호출',
        'tag': 'Recursion',
        'detail': '종료 조건과 작은 문제',
        'listTag': '종료 조건 · 재귀 호출',
        'simulationTitle': '호출과 복귀를 따라가 보세요'
    },
    'tree': {
        'label': '트리',
        'introduction': '노드와 자식을 연결하며 트리의 구조를 익혀보세요.',
        'family': 'DATA STRUCTURES',
        'note': '루트 · 왼쪽 자식 · 오른쪽 자식',
        'tag': 'Binary Tree',
        'detail': '노드마다 최대 두 자식',
        'listTag': '이진 트리 · 자식 포인터',
        'simulationTitle': '부모와 자식은 이렇게 연결돼요'
    },
    'heap': {
        'label': '힙',
        'introduction': '부모와 자식의 우선순위를 유지하며 힙의 삽입과 삭제를 익혀보세요.',
        'family': 'DATA STRUCTURES',
        'note': '',
        'tag': 'Binary Heap',
        'detail': '부모와 자식의 우선순위 · 중복 키 허용',
        'listTag': '',
        'simulationTitle': '배열과 트리를 함께 따라가 보세요'
    },
    'hash': {
        'label': '해싱',
        'introduction': '키를 버킷에 배치하고 체인으로 충돌을 처리해 보세요.',
        'family': 'DATA STRUCTURES',
        'note': '',
        'tag': 'Chaining Hash',
        'detail': '충돌은 같은 체인에 · 중복 키는 무시',
        'listTag': '',
        'simulationTitle': '버킷과 체인을 따라가 보세요'
    },
    'set': {
        'label': '집합',
        'introduction': '대표 원소를 찾고 집합을 합치며 경로 압축을 익혀보세요.',
        'family': 'DATA STRUCTURES',
        'note': '부모 배열 · 루트는 -1 · 경로 압축',
        'tag': 'Disjoint Set',
        'detail': '대표 찾기 · 집합 합치기 · 경로 압축',
        'listTag': '부모 배열 · 경로 압축',
        'simulationTitle': '부모 연결과 경로 압축을 따라가 보세요'
    },
    'graph': {
        'label': '그래프',
        'introduction': '정점과 간선을 연결하고 방문 순서를 따라가며 그래프를 익혀보세요.',
        'family': 'DATA STRUCTURES',
        'note': '',
        'tag': 'Graph',
        'detail': '정점 · 간선 · 연결 요소',
        'listTag': '',
        'simulationTitle': '연결 관계와 방문 순서를 따라가 보세요'
    },
    'sort': {
        'label': '정렬',
        'introduction': '값을 비교하고 이동하며 정렬의 기본 코드를 익혀보세요.',
        'family': 'ALGORITHMS',
        'note': '오름차순 · 배열 원소와 중복 개수 보존',
        'tag': 'Sorting',
        'detail': '비교 · 교환 · 이동',
        'listTag': '정수 배열 · 오름차순 정렬',
        'simulationTitle': '배열이 정렬되는 과정을 따라가 보세요'
    },
    'search': {
        'label': '탐색',
        'introduction': '정렬된 배열의 탐색 구간을 줄이며 이진 탐색을 익혀보세요.',
        'family': 'ALGORITHMS',
        'note': '정렬된 배열 · low · middle · high',
        'tag': 'Binary Search',
        'detail': '정렬된 배열 · 인덱스 반환 · 없으면 -1',
        'listTag': '정렬된 배열 · 탐색 구간 절반 줄이기',
        'simulationTitle': '탐색 구간이 줄어드는 과정을 따라가 보세요'
    }
} satisfies Record<ExerciseCategory, {
    label: string
    introduction: string
    family: string
    note: string
    tag: string
    detail: string
    listTag: string
    simulationTitle: string
}>

// 목록·상세의 분류와 저장 방식별 표시 문구 결정
export function getExercisePresentation(exercise: Pick<ExerciseSummary, 'category' | 'contract'>) {
    const presentation = { ...categories[exercise.category] }
    const { category, contract } = exercise

    switch (category) {
        case 'dp':
            switch (contract.problem) {
                case 'knapsack':
                    presentation.note = '물건별 0/1 선택 · 2차원 DP'
                    presentation.tag = '0/1 Knapsack'
                    presentation.detail = '이전 행에서 포함·제외 비교'
                    presentation.listTag = presentation.note
                    presentation.simulationTitle = '물건과 용량에 따라 표를 채워보세요'
                    break
                case 'lcs':
                    presentation.note = '두 문자열 · 접두사 · 2차원 DP'
                    presentation.tag = 'Longest Common Subsequence'
                    presentation.detail = '일치: 대각선 + 1 · 불일치: 위·왼쪽 비교'
                    presentation.listTag = presentation.note
                    presentation.simulationTitle = '문자 비교와 표 갱신을 따라가 보세요'
                    break
            }
            break
        case 'graph':
            presentation.note = contract.storage === 'linked' ? '무방향 · 비가중치 · 인접 리스트' : '무방향 · 비가중치 · 인접 행렬'
            if (contract.directed && !contract.weighted) {
                presentation.note = contract.storage === 'linked' ? '방향 · 비가중치 · 인접 리스트' : '방향 · 비가중치 · 인접 행렬'
                presentation.introduction = '간선의 방향과 정점의 선후 관계를 따라가며 그래프 알고리즘을 익혀보세요.'
                presentation.simulationTitle = '간선 방향과 처리 순서를 따라가 보세요'
            }
            if (contract.weighted) {
                const direction = contract.directed ? '방향' : '무방향'
                const representation = contract.representation === 'edges' ? '간선 목록' : '인접 행렬'
                presentation.note = `${direction} · 가중치 · ${representation}`
                presentation.introduction = '간선의 가중치를 비교하고 선택 과정을 따라가며 그래프 알고리즘을 익혀보세요.'
                presentation.simulationTitle = '가중치와 선택 간선을 따라가 보세요'
                if (contract.directed) {
                    presentation.simulationTitle = '간선 방향과 거리 갱신을 따라가 보세요'
                }
            }
            presentation.listTag = presentation.note
            break
        case 'heap':
            presentation.note = `완전 이진 트리 · 배열 · 저장 ${contract.usableCapacity}개`
            presentation.listTag = presentation.note
            break
        case 'hash':
            presentation.note = `버킷 ${contract.capacity}개 · 연결 리스트 · 충돌 처리`
            presentation.listTag = `버킷 ${contract.capacity}개 · 연결 리스트`
            break
        case 'linked-list':
            if (contract.doubly) {
                presentation.note = '헤드 노드 · 양방향 연결'
                presentation.tag = 'Doubly Linked List'
                presentation.detail = 'llink와 rlink로 앞뒤 이동'
                presentation.listTag = '양방향 연결 · 헤드 노드'
            } else if (contract.circular) {
                presentation.note = '순환 연결 · head는 마지막 노드'
                presentation.tag = 'Circular Linked List'
                presentation.detail = 'head->link에서 한 바퀴 순회'
                presentation.listTag = '원형 연결 · 동적 메모리'
            }
            break
        case 'stack':
        case 'queue':
        case 'deque':
            if (contract.storage === 'linked') {
                presentation.simulationTitle = '노드는 이렇게 연결돼요'
                presentation.listTag = '연결 리스트 · 동적 메모리'
                if (category === 'stack') {
                    presentation.note = '노드 · top 포인터 · 동적 메모리'
                    presentation.detail = 'top에서 link를 따라 순회'
                } else {
                    presentation.note = `노드 · front와 rear 포인터 · ${category === 'deque' ? '양방향 연결' : 'FIFO'}`
                    presentation.detail = category === 'deque' ? '양쪽 끝에서 삽입 · 삭제 · 조회' : 'front에서 꺼내고 rear 뒤에 연결'
                }
            } else if (contract.circular) {
                presentation.note = `원형 배열 · ${category === 'deque' ? '양쪽 삽입·삭제' : 'FIFO'} · 저장 ${contract.usableCapacity}개`
                presentation.listTag = `원형 배열 · 저장 ${contract.usableCapacity}개`
            } else if (contract.initialCapacity !== undefined) {
                presentation.note = `동적 배열 · LIFO · 초기 용량 ${contract.initialCapacity}`
                presentation.listTag = `동적 배열 · 초기 용량 ${contract.initialCapacity}`
            } else {
                presentation.note = `배열 · ${category === 'queue' ? 'FIFO' : 'LIFO'} · 용량 ${contract.capacity}`
                presentation.listTag = `고정 배열 · 용량 ${contract.capacity}`
            }
            break
    }
    return presentation
}
