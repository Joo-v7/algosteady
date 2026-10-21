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
    category: string
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

export interface ExerciseDetails {
    id: string
    version: string
    title: string
    description: string
    category: string
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
        steps: {
            operation: string
            argument: number | string | null
            result: number | string | null
            values?: number[]
            tree?: (number | null)[]
            activeNode?: number
            visited?: number[]
            trace?: string[]
            description?: string
            top?: number
            front?: number
            rear?: number
            capacity?: number
        }[]
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
