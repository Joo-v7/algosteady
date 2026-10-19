import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { getSubmission } from '../api/submissions'
import type { SubmissionDetails } from '../api/submissions'

const jobLabels = {
    QUEUED: '대기',
    RUNNING: '채점 중',
    COMPLETED: '채점 완료',
    FAILED: '작업 실패'
}
const criterionLabels = {
    PENDING: '대기',
    RUNNING: '판정 중',
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

    // 제출 원본은 그대로 조회하며 진행 중인 채점 결과만 갱신한다.
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
                    <Link className="primary-button" to="/exercises">과제 목록으로</Link>
                </section>
            </main>
        )
    }

    const job = submission.job

    return (
        <main className="workspace">
            <div className="breadcrumb">
                <Link to="/exercises">과제 목록</Link> <span>/</span>
                <Link to={`/exercises/${submission.exerciseId}/submissions?page=${encodeURIComponent(historyPage)}`}>제출 기록</Link> <span>/</span> 상세
            </div>
            <section className="exercise-heading">
                <div>
                    <div className="eyebrow">SUBMISSION DETAILS</div>
                    <h1>{job.exerciseTitle} · 제출 상세</h1>
                    <p><time dateTime={submission.submittedAt}>{new Date(submission.submittedAt).toLocaleString('ko-KR')}</time> · {submission.language.toUpperCase()} · 버전 {submission.contentVersion}</p>
                </div>
                <span className="language-badge">{jobLabels[job.status]}</span>
            </section>
            <section className="panel history-panel">
                <h2>제출 당시 코드</h2>
                <p>기준 코드 {submission.referenceViewed ? '열람' : '미열람'} · 제출 ID {submission.submissionId}</p>
                <pre className="submission-code">{submission.code}</pre>
            </section>
            <section className="panel history-panel execution-result">
                <h2>채점 결과 · {jobLabels[job.status]}</h2>
                <p>통과 {job.criteria.filter(item => item.status === 'PASSED').length} / {job.criteria.length}</p>
                {job.message && <p>{job.message}</p>}
                {job.preparation && (
                    <div>
                        <h3>컴파일 · {job.preparation.status}</h3>
                        <pre>{job.preparation.stdout}{job.preparation.stderr}</pre>
                        <p>종료 코드: {job.preparation.exitCode ?? '—'} · {job.preparation.message}</p>
                    </div>
                )}
                {job.criteria.map(criterion => (
                    <details className="criterion-detail" key={criterion.criterionId}>
                        <summary>{criterion.title} · {criterionLabels[criterion.status]} · {criterion.passedTests} / {criterion.totalTests}</summary>
                        <p>{criterion.description}</p>
                        {criterion.status === 'UNAVAILABLE' && <p>컴파일 실패, 실행 환경 장애 또는 작업 중단으로 검사를 완료하지 못했습니다.</p>}
                        {job.tests.filter(test => test.criterionId === criterion.criterionId).map(test => (
                            <div className="failed-test" key={test.testId}>
                                <h3>{test.description} · {test.passed ? '통과' : test.actual.status}</h3>
                                <h3>입력</h3>
                                <pre>{test.input || '(입력 없음)'}</pre>
                                <h3>기대 출력</h3>
                                <pre>{test.expected.stdout || '(출력 없음)'}</pre>
                                <p>기대 종료 코드: {test.expected.exitCode} · stderr: {test.expected.stderrRequired ? '출력 필요' : '출력 없음'}</p>
                                <h3>실제 출력</h3>
                                <pre>{test.actual.stdout || '(출력 없음)'}</pre>
                                <h3>stderr</h3>
                                <pre>{test.actual.stderr || '(출력 없음)'}</pre>
                                <p>종료 코드: {test.actual.exitCode ?? '—'} · 실행 시간: {test.actual.executionTimeNanos == null ? '—' : `${(test.actual.executionTimeNanos / 1000000).toFixed(3)} ms`}</p>
                                {test.actual.message && <p>{test.actual.message}</p>}
                            </div>
                        ))}
                    </details>
                ))}
            </section>
        </main>
    )
}

export default SubmissionDetailsPage
