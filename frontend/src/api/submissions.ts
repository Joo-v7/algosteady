import { requestApi } from './exercises'
import type { Job } from './runs'

export interface SubmissionRequest {
    exerciseId: string
    contentVersion: string
    language: string
    code: string
    referenceViewed: boolean
}

export function createSubmission(request: SubmissionRequest, signal: AbortSignal) {
    return requestApi<{
        submissionId: string
        jobId: string
    }>('/api/submissions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
        signal,
    })
}

export interface SubmissionSummary {
    submissionId: string
    exerciseId: string
    contentVersion: string
    language: string
    referenceViewed: boolean
    submittedAt: string
    job: {
        jobId: string
        status: Job['status']
        passedCriteria: number
        totalCriteria: number
    }
}

export interface SubmissionPage {
    content: SubmissionSummary[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export interface SubmissionDetails {
    submissionId: string
    exerciseId: string
    contentVersion: string
    language: string
    code: string
    referenceViewed: boolean
    submittedAt: string
    job: Job
}

export function getSubmissions(exerciseId: string, page: string, signal: AbortSignal) {
    return requestApi<SubmissionPage>(`/api/submissions?exerciseId=${encodeURIComponent(exerciseId)}&language=c&page=${encodeURIComponent(page)}`, { signal })
}

export function getSubmission(id: string, signal: AbortSignal) {
    return requestApi<SubmissionDetails>(`/api/submissions/${encodeURIComponent(id)}`, { signal })
}
