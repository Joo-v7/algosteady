import type { ExerciseCategory, ExerciseSummary } from './api/exercises.ts'

const categories = {
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
