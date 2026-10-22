export type ExerciseCategory = 'stack' | 'queue' | 'deque' | 'linked-list' | 'recursion' | 'tree' | 'heap' | 'hash' | 'set' | 'search'

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
        capacity?: number
        circular?: boolean
        doubly?: boolean
        storage?: 'array' | 'linked'
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

export type ExerciseDetails =
    | ExerciseContent<'stack' | 'queue' | 'deque' | 'linked-list', SequenceStep>
    | ExerciseContent<'tree', TreeStep>
    | ExerciseContent<'heap', HeapStep>
    | ExerciseContent<'search', SearchStep>
    | ExerciseContent<'set', SetStep>
    | ExerciseContent<'hash', HashStep>
    | ExerciseContent<'recursion', RecursionStep>

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
        capacity?: number
        circular?: boolean
        doubly?: boolean
        storage?: 'array' | 'linked'
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

// 콘텐츠와 실행 API의 오류 응답을 같은 방식으로 처리한다.
export async function requestApi<T>(url: string, options?: RequestInit): Promise<T> {
    const response = await fetch(url, options)

    if (!response.ok) {
        const error = await response.json().catch(() => null)
        throw new Error(error?.message ?? '서버에 연결하지 못했습니다.')
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
