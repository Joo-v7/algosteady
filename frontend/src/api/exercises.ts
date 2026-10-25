export type ExerciseCategory = 'stack' | 'queue' | 'deque' | 'linked-list' | 'recursion' | 'tree' | 'heap' | 'hash' | 'set' | 'search' | 'sort' | 'graph' | 'string' | 'trie' | 'exhaustive' | 'backtracking' | 'greedy' | 'dp' | 'two-pointer' | 'sliding-window'

export interface ExpectedOutput {
    stdout: string
    exitCode: number
    stderrRequired: boolean
}

export interface PublicExample {
    id: string
    description: string
    input: string
    expected: ExpectedOutput
}

export interface ExerciseSummary {
    id: string
    contentVersion: string
    title: string
    description: string
    category: ExerciseCategory
    languages: string[]
    contract: {
        problem?: 'fibonacci' | 'knapsack' | 'lcs'
        capacity?: number
        circular?: boolean
        doubly?: boolean
        storage?: 'array' | 'linked'
        weighted?: boolean
        directed?: boolean
        representation?: 'matrix' | 'list' | 'edges'
        usableCapacity?: number
        initialCapacity?: number
        growthFactor?: number
    }
    criteria: {
        id: string
        title: string
        description: string
    }[]
}

export interface LearningStep {
    operation: string
    argument: number | string | null
    result: number | string | null
    description?: string
}

export interface SequenceStep extends LearningStep {
    values: number[]
    top?: number
    front?: number
    rear?: number
    capacity?: number
}

export interface TreeStep extends LearningStep {
    tree: (number | null)[]
    activeNode?: number
    visited?: number[]
    trace?: string[]
}

export interface HeapStep extends TreeStep {
    values: number[]
}

export interface SlidingWindowStep extends LearningStep {
    values: number[]
    k: number
    start: number
    end: number
    sum: number
    best: number | null
    previousSum?: number
    phase: 'initial' | 'slide' | 'done'
    trace: string[]
}

export interface TwoPointerStep extends LearningStep {
    values: number[]
    left: number
    right: number
    target: number
    sum?: number
    decision: 'move-left' | 'move-right' | 'found' | 'absent'
    trace: string[]
}

export interface SearchStep extends LearningStep {
    values: number[]
    low: number
    high: number
    target: number
    activeNode?: number
    trace: string[]
}

export interface SetStep extends LearningStep {
    parents: number[]
    activeNode?: number
    visited?: number[]
    trace: string[]
}

export interface HashStep extends LearningStep {
    buckets: number[][]
    activeBucket?: number
    trace: string[]
}

export interface RecursionStep extends LearningStep {
    trace: string[]
}

export interface SortStep extends LearningStep {
    values: number[]
    active: number[]
    sorted: number[]
    trace: string[]
}

export interface GraphStep extends LearningStep {
    adjacency: number[][]
    weights?: (number | null)[][]
    selectedEdges?: [number, number][]
    activeEdge?: [number, number]
    totalWeight?: number
    source?: number
    distances?: (number | null)[]
    distanceMatrix?: (number | null)[][]
    intermediate?: number
    inDegrees?: number[]
    visited: number[]
    activeNode?: number
    frontier?: number[]
    frontierLabel?: string
    trace: string[]
}

export interface StringStep extends LearningStep {
    text: string
    pattern?: string
    offset?: number
    patternIndex?: number
    tokens?: string[]
    output?: string
    trace: string[]
}

export interface TrieStep extends LearningStep {
    word: string
    nodes: {
        prefix: string
        terminal: boolean
    }[]
    activePrefix?: string
    trace: string[]
}

export interface ExhaustiveStep extends LearningStep {
    values: number[]
    selected: number[]
    depth: number
    activeValue?: number
    trace: string[]
}

export interface NQueenStep extends LearningStep {
    size: number
    queens: number[]
    row: number
    candidateColumn?: number
    safe?: boolean
    solutions: number
    trace: string[]
}

export interface ActivityStep extends LearningStep {
    activities: {
        id: number
        start: number
        end: number
    }[]
    processed: number
    selected: number[]
    lastEnd?: number
    trace: string[]
}

export interface FibonacciStep extends LearningStep {
    problem?: 'fibonacci'
    method: 'memoization' | 'tabulation'
    target: number
    values: (number | null)[]
    activeIndex?: number
    reused?: boolean
    pending: number[]
    trace: string[]
}

export interface KnapsackStep extends LearningStep {
    problem: 'knapsack'
    items: {
        weight: number
        value: number
    }[]
    capacity: number
    table: (number | null)[][]
    activeCell?: [number, number]
    comparison?: {
        excluded: number
        included: number | null
    }
    trace: string[]
}

export interface LcsStep extends LearningStep {
    problem: 'lcs'
    a: string
    b: string
    table: (number | null)[][]
    activeCell?: [number, number]
    trace: string[]
}

export type ExerciseDetails =
    | ExerciseContent<'stack' | 'queue' | 'deque' | 'linked-list', SequenceStep>
    | ExerciseContent<'tree', TreeStep>
    | ExerciseContent<'heap', HeapStep>
    | ExerciseContent<'search', SearchStep>
    | ExerciseContent<'two-pointer', TwoPointerStep>
    | ExerciseContent<'sliding-window', SlidingWindowStep>
    | ExerciseContent<'set', SetStep>
    | ExerciseContent<'hash', HashStep>
    | ExerciseContent<'recursion', RecursionStep>
    | ExerciseContent<'sort', SortStep>
    | ExerciseContent<'graph', GraphStep>
    | ExerciseContent<'string', StringStep>
    | ExerciseContent<'trie', TrieStep>
    | ExerciseContent<'exhaustive', ExhaustiveStep>
    | ExerciseContent<'backtracking', NQueenStep>
    | ExerciseContent<'greedy', ActivityStep>
    | ExerciseContent<'dp', FibonacciStep | KnapsackStep | LcsStep>

interface ExerciseContent<Category extends ExerciseCategory, Step extends LearningStep> {
    id: string
    version: string
    title: string
    description: string
    category: Category
    language: string
    standard: string
    starterCode: string
    publicExamples: PublicExample[]
    learning: {
        concept: {
            title: string
            paragraphs: string[]
        }
        operations: {
            function: string
            description: string
        }[]
        steps: Step[]
        notes?: {
            title: string
            description: string
        }[]
    }
    contract: {
        problem?: 'fibonacci' | 'knapsack' | 'lcs'
        capacity?: number
        circular?: boolean
        doubly?: boolean
        storage?: 'array' | 'linked'
        weighted?: boolean
        directed?: boolean
        representation?: 'matrix' | 'list' | 'edges'
        usableCapacity?: number
        initialCapacity?: number
        growthFactor?: number
        scope: string
        functions: {
            name: string
            signature: string
            behavior: string
        }[]
    }
    criteria: {
        id: string
        title: string
        description: string
    }[]
}

// 응답 상태를 보존해 없는 작업과 일시적인 조회 실패 구분
export class ApiError extends Error {
    readonly status: number

    constructor(message: string, status: number) {
        super(message)
        this.name = 'ApiError'
        this.status = status
    }
}

// API 요청 및 공통 오류 응답 처리
export async function requestApi<T>(url: string, options?: RequestInit): Promise<T> {
    const response = await fetch(url, options)

    if (!response.ok) {
        const error = await response.json().catch(() => null)
        throw new ApiError(error?.message ?? '서버에 연결하지 못했습니다.', response.status)
    }

    return response.json()
}

export function getExercises(signal: AbortSignal) {
    return requestApi<ExerciseSummary[]>('/api/exercises', { signal })
}

export function getExercise(id: string, signal: AbortSignal) {
    return requestApi<ExerciseDetails>(`/api/exercises/${encodeURIComponent(id)}?language=c`, { signal })
}

export function getReference(id: string, version: string, signal: AbortSignal) {
    return requestApi<{ code: string }>(`/api/exercises/${encodeURIComponent(id)}/reference?language=c&contentVersion=${encodeURIComponent(version)}`, { signal })
}
