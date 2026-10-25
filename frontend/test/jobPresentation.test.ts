import assert from 'node:assert/strict'
import { test } from 'node:test'
import type { Job } from '../src/api/runs.ts'
import { getJobPresentation } from '../src/jobPresentation.ts'

function completedJob(kind: Job['kind'] = 'GRADING'): Job {
    return {
        jobId: 'job',
        exerciseId: 'stack-array',
        exerciseTitle: '고정 배열 스택',
        contentVersion: '1.0.1',
        language: 'c',
        code: 'code',
        exampleId: null,
        kind,
        criteria: [],
        status: 'COMPLETED',
        preparation: {
            status: 'SUCCEEDED',
            stdout: '',
            stderr: '',
            exitCode: 0,
            message: null
        },
        tests: [{
            testId: 'example',
            criterionId: 'init',
            description: '초기화',
            input: '',
            expected: {
                stdout: 'OK\n',
                exitCode: 0,
                stderrRequired: false
            },
            actual: {
                status: 'COMPLETED',
                stdout: 'OK\n',
                stderr: '',
                exitCode: 0,
                executionTimeNanos: 300000000,
                message: null
            },
            passed: true
        }],
        message: null
    }
}

test('실행과 제출은 같은 결과를 서로 다른 문구로 표시한다', () => {
    assert.equal(getJobPresentation(completedJob('RUN'), 'RUN').resultLabel, '정상')
    assert.equal(getJobPresentation(completedJob(), 'GRADING').resultLabel, '모든 기준 통과')
    const job = completedJob()
    job.tests[0].passed = false
    assert.equal(getJobPresentation(job, 'GRADING').resultLabel, '일부 기준 실패')
    job.kind = 'RUN'
    assert.equal(getJobPresentation(job, 'RUN').resultLabel, '불일치')
})

test('요청 등록과 작업 복구 중에는 완료 결과를 표시하지 않는다', () => {
    assert.equal(getJobPresentation(null, 'GRADING', true).resultLabel, '채점 중…')
    assert.equal(getJobPresentation(null, 'RUN', true).resultLabel, '실행 중…')
    for (const status of ['QUEUED', 'RUNNING'] as const) {
        const job = completedJob()
        job.status = status
        assert.equal(getJobPresentation(job, 'GRADING').resultStatus, 'pending')
    }
})

test('컴파일 오류·서버 장애·검사 불가를 통과로 표시하지 않는다', () => {
    const job = completedJob()
    job.preparation!.status = 'FAILED'
    job.tests[0].actual.status = 'NOT_EXECUTED'
    job.tests[0].actual.executionTimeNanos = null
    job.tests[0].passed = false
    let presentation = getJobPresentation(job, 'GRADING')
    assert.equal(presentation.resultLabel, '컴파일 오류')
    assert.equal(presentation.executionSeconds, null)
    job.preparation!.status = 'ENVIRONMENT_ERROR'
    assert.equal(getJobPresentation(job, 'GRADING').resultLabel, '채점 오류')
    job.preparation!.status = 'SUCCEEDED'
    job.status = 'FAILED'
    assert.equal(getJobPresentation(job, 'GRADING').resultLabel, '채점 오류')
    presentation = getJobPresentation(completedJob(), 'GRADING', false, '통신 오류')
    assert.equal(presentation.resultLabel, '채점 오류')
    assert.equal(presentation.resultStatus, 'failed')
    const unavailable = completedJob()
    unavailable.tests = []
    unavailable.criteria = [{
        criterionId: 'init',
        title: '초기화',
        description: '',
        status: 'UNAVAILABLE',
        totalTests: 2,
        completedTests: 0,
        passedTests: 0
    }]
    assert.equal(getJobPresentation(unavailable, 'GRADING').resultLabel, '채점 오류')
})

test('시간 초과는 기준 실패로 표시하고 실행은 오류로 표시한다', () => {
    const job = completedJob()
    job.tests[0].actual.status = 'TIME_LIMIT_EXCEEDED'
    job.tests[0].passed = false
    assert.equal(getJobPresentation(job, 'GRADING').resultLabel, '일부 기준 실패')
    job.kind = 'RUN'
    assert.equal(getJobPresentation(job, 'RUN').resultLabel, '오류')
})

test('측정된 테스트 시간만 초 단위로 합산하고 0초도 보존한다', () => {
    const job = completedJob()
    job.tests.push({
        ...job.tests[0],
        testId: 'second',
        actual: {
            ...job.tests[0].actual,
            executionTimeNanos: 700000000
        }
    })
    job.tests.push({
        ...job.tests[0],
        testId: 'skipped',
        actual: {
            ...job.tests[0].actual,
            status: 'NOT_EXECUTED',
            executionTimeNanos: null
        }
    })
    assert.equal(getJobPresentation(job, 'GRADING').executionSeconds, 1)
    job.tests = [job.tests[0]]
    job.tests[0].actual.executionTimeNanos = 0
    assert.equal(getJobPresentation(job, 'GRADING').executionSeconds, 0)
})
