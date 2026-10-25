import { requestApi } from './exercises'
import type { ExpectedOutput } from './exercises'

export interface RunRequest {
    exerciseId: string
    contentVersion: string
    language: string
    code: string
    exampleId: string
}

export interface Job {
    jobId: string
    exerciseId: string
    exerciseTitle: string
    contentVersion: string
    language: string
    code: string
    exampleId: string | null
    kind: 'RUN' | 'GRADING'
    criteria: {
        criterionId: string
        title: string
        description: string
        status: 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED' | 'UNAVAILABLE'
        totalTests: number
        completedTests: number
        passedTests: number
    }[]
    status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED'
    preparation: {
        status: string
        stdout: string
        stderr: string
        exitCode: number | null
        message: string | null
    } | null
    tests: {
        testId: string
        criterionId: string
        description: string
        input: string
        expected: ExpectedOutput
        actual: {
            status: string
            stdout: string
            stderr: string
            exitCode: number | null
            executionTimeNanos: number | null
            message: string | null
        }
        passed: boolean
    }[]
    message: string | null
}

export function createRun(request: RunRequest, signal: AbortSignal) {
    return requestApi<{ jobId: string }>('/api/runs', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
        signal,
    })
}

export function getJob(id: string, signal: AbortSignal) {
    return requestApi<Job>(`/api/jobs/${encodeURIComponent(id)}`, { signal })
}
