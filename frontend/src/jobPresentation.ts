import type { Job } from './api/runs.ts'

// 실습·제출 상세의 공통 결과 문구와 실행 시간 계산
export function getJobPresentation(job: Job | null, kind: Job['kind'], pending = false, error = '') {
    const resultKind = job?.kind ?? kind
    const resultPending = (pending || job?.status === 'QUEUED' || job?.status === 'RUNNING') && !error
    // 미실행 테스트를 제외한 판정 대상 선택
    const resultTests = job?.tests.filter(test => test.actual.status !== 'NOT_EXECUTED') ?? []
    const preparation = job?.preparation
    const compilationFailed = preparation != null && preparation.status !== 'SUCCEEDED'
    const resultError = Boolean(error || job?.status === 'FAILED' || compilationFailed
        || job?.criteria.some(criterion => criterion.status === 'UNAVAILABLE')
        || resultTests.some(test => test.actual.status !== 'COMPLETED'))
    const resultFailed = Boolean(job?.criteria.some(criterion => criterion.status === 'FAILED') || resultTests.some(test => !test.passed))

    // 진행 상태와 오류 여부에 따른 결과 색상 구분
    let resultStatus = 'passed'
    if (resultPending) {
        resultStatus = 'pending'
    } else if (resultError || resultFailed) {
        resultStatus = 'failed'
    }

    // 실행·제출 구분 및 오류 우선순위에 따른 결과 문구 결정
    let resultLabel: string
    if (resultPending) {
        resultLabel = resultKind === 'GRADING' ? '채점 중…' : '실행 중…'
    } else {
        switch (resultKind) {
            case 'GRADING':
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
                break
            case 'RUN':
                if (resultError) {
                    resultLabel = '오류'
                } else if (resultFailed) {
                    resultLabel = '불일치'
                } else {
                    resultLabel = '정상'
                }
                break
        }
    }

    // 측정된 테스트 실행 시간의 초 단위 합산
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
