import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { getSubmission } from '../api/submissions'
import type { SubmissionDetails } from '../api/submissions'
import ExecutionResultPanel from '../components/ExecutionResultPanel'

const criterionLabels = {
    PENDING: '대기',
    RUNNING: '채점 중',
    PASSED: '통과',
    FAILED: '실패',
    UNAVAILABLE: '검사 불가'
}

function SubmissionDetailsPage() {
    const { submissionId } = useParams()
    const [searchParams] = useSearchParams()
    const historyPage = searchParams.get('page') ?? '0'
    const [submission, setSubmission] = useState<SubmissionDetails | null>(null)
    const [error, setError] = useState('')

    // 제출 원본 조회 및 진행 중인 채점 결과 갱신
    useEffect(() => {
        const controller = new AbortController()
        let timer: ReturnType<typeof setTimeout>

        async function poll() {
            try {
                const result = await getSubmission(submissionId ?? '', controller.signal)
                if (controller.signal.aborted) {
                    return
                }
                setSubmission(result)
                setError('')
                if (result.job.status === 'QUEUED' || result.job.status === 'RUNNING') {
                    timer = setTimeout(poll, 500)
                }
            } catch (error) {
                if (!controller.signal.aborted) {
                    setError(error instanceof Error ? error.message : '제출 상세를 조회하지 못했습니다.')
                }
            }
        }

        void poll()

        return () => {
            controller.abort()
            clearTimeout(timer)
        }
    }, [submissionId])

    if (!submission || submission.submissionId !== submissionId || error) {
        return (
            <main className="workspace">
                <section className="panel unavailable-page">
                    <h1>{error || '제출 기록을 불러오는 중입니다.'}</h1>
                    <Link className="primary-button" to="/exercises">학습 목록으로</Link>
                </section>
            </main>
        )
    }

    const job = submission.job

    return (
        <main className="workspace">
            <div className="breadcrumb">
                <Link to="/exercises">학습 목록</Link> <span>/</span>
                <Link to={`/exercises/${submission.exerciseId}/submissions?page=${encodeURIComponent(historyPage)}`}>제출 기록</Link> <span>/</span> 상세
            </div>
            <section className="exercise-heading">
                <div>
                    <div className="eyebrow">SUBMISSION DETAILS</div>
                    <h1>{job.exerciseTitle} · 제출 상세</h1>
                    <p><time dateTime={submission.submittedAt}>{new Date(submission.submittedAt).toLocaleString('ko-KR')}</time> · {submission.language.toUpperCase()}</p>
                </div>
            </section>
            <section className="panel history-panel">
                <h2>제출 코드</h2>
                <pre className="submission-code">{submission.code}</pre>
            </section>
            <ExecutionResultPanel variant="history" job={job} kind="GRADING" />
            <section className="panel history-panel">
                <h2>기준별 상세</h2>
                <ol className="criteria-list history-criteria">
                    {job.criteria.map(criterion => {
                        const criterionTests = job.tests.filter(test => test.criterionId === criterion.criterionId)
                        return (
                            <li key={criterion.criterionId} data-status={criterion.status}>
                                <details className="criterion-detail">
                                    <summary>
                                        <span className="criterion-circle" />
                                        <strong>{criterion.title}</strong>
                                        <span className="waiting-label">{criterionLabels[criterion.status]}</span>
                                        <span className="criterion-count">{criterion.passedTests} / {criterion.totalTests} 테스트 통과</span>
                                    </summary>
                                    <p className="criterion-description">{criterion.description}</p>
                                    {criterionTests.length === 0 && (
                                        <p className="history-test-note">{criterion.status === 'UNAVAILABLE' ? '검사를 완료하지 못했습니다.' : '아직 실행 결과가 없습니다.'}</p>
                                    )}
                                    {criterionTests.map(test => {
                                        let testLabel = '실패'
                                        if (test.actual.status === 'NOT_EXECUTED') {
                                            testLabel = '미실행'
                                        } else if (test.passed) {
                                            testLabel = '통과'
                                        }
                                        return (
                                            <details className="history-test" key={test.testId}>
                                                <summary>
                                                    <span>{test.description}</span>
                                                    <span className="result-status" data-status={test.passed ? 'passed' : 'failed'}>
                                                        {testLabel}
                                                    </span>
                                                    <span className="result-time">{test.actual.executionTimeNanos == null ? '— s' : `${(test.actual.executionTimeNanos / 1000000000).toFixed(3)} s`}</span>
                                                </summary>
                                                {test.actual.status === 'NOT_EXECUTED' ? (
                                                    <p className="history-test-note">이 테스트는 실행되지 않았습니다.</p>
                                                ) : (
                                                    <div className="test-comparison">
                                                        <div className="test-input">
                                                            <h3>입력</h3>
                                                            <pre>{test.input || '(입력 없음)'}</pre>
                                                        </div>
                                                        <div>
                                                            <h3>기대 출력</h3>
                                                            <pre>{test.expected.stdout || '(출력 없음)'}</pre>
                                                        </div>
                                                        <div>
                                                            <h3>실제 출력</h3>
                                                            <pre>{test.actual.stdout || '(출력 없음)'}</pre>
                                                        </div>
                                                        {(test.actual.stderr || test.actual.message || !test.passed && test.actual.exitCode !== test.expected.exitCode) && (
                                                            <div className="test-error">
                                                                <h3>오류</h3>
                                                                <pre>{test.actual.stderr || test.actual.message || '프로그램이 예상과 다르게 종료되었습니다.'}</pre>
                                                            </div>
                                                        )}
                                                    </div>
                                                )}
                                            </details>
                                        )
                                    })}
                                </details>
                            </li>
                        )
                    })}
                </ol>
            </section>
        </main>
    )
}

export default SubmissionDetailsPage
