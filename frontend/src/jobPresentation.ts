import type { Job } from './api/runs.ts'

// 실습과 제출 상세에 같은 판정 문구와 실행 시간을 적용한다.
export function getJobPresentation(job: Job | null, kind: Job['kind'], pending = false, error = '') {
    const resultKind = job?.kind ?? kind
    const resultPending = (pending || job?.status === 'QUEUED' || job?.status === 'RUNNING') && !error
    const resultTests = job?.tests.filter(test => test.actual.status !== 'NOT_EXECUTED') ?? []
    const preparation = job?.preparation
    const compilationFailed = preparation != null && preparation.status !== 'SUCCEEDED'
    const resultError = Boolean(error || job?.status === 'FAILED' || compilationFailed
        || job?.criteria.some(criterion => criterion.status === 'UNAVAILABLE')
        || resultTests.some(test => test.actual.status !== 'COMPLETED'))
    const resultFailed = Boolean(job?.criteria.some(criterion => criterion.status === 'FAILED') || resultTests.some(test => !test.passed))
    const resultStatus = resultPending ? 'pending' : resultError || resultFailed ? 'failed' : 'passed'
    let resultLabel = resultPending ? (resultKind === 'GRADING' ? '채점 중…' : '실행 중…')
        : resultError ? '오류' : resultFailed ? (resultKind === 'GRADING' ? '실패' : '불일치')
            : resultKind === 'GRADING' ? '통과' : '정상'

    if (resultKind === 'GRADING' && !resultPending) {
        if (error || job?.status === 'FAILED' || preparation?.status === 'ENVIRONMENT_ERROR') {
            resultLabel = '채점 오류'
        } else if (compilationFailed) {
            resultLabel = '컴파일 오류'
        } else if (resultFailed) {
            resultLabel = '일부 기준 실패'
        } else if (resultError) {
            resultLabel = '채점 오류'
        } else {
            resultLabel = '모든 기준 통과'
        }
    }

    const measuredTests = resultTests.filter(test => test.actual.executionTimeNanos != null)
    const executionSeconds = measuredTests.length > 0
        ? measuredTests.reduce((total, test) => total + (test.actual.executionTimeNanos ?? 0), 0) / 1000000000 : null
    const failedTest = resultTests.find(test => !test.passed)

    return {
        resultKind,
        resultPending,
        resultTests,
        preparation,
        compilationFailed,
        resultStatus,
        resultLabel,
        executionSeconds,
        failedTest
    }
}
