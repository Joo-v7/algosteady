import type { Job } from '../api/runs'
import { getJobPresentation } from '../jobPresentation'

// 실습·제출 상세의 결과 상태와 실행 시간 표시
export default function ExecutionResultPanel({ variant, job, kind, pending = false, error = '', currentCode }: {
    variant: 'practice' | 'history'
    job: Job | null
    kind: Job['kind']
    pending?: boolean
    error?: string
    currentCode?: string
}) {
    const { resultKind, resultPending, resultTests, preparation, compilationFailed, resultStatus, resultLabel, executionSeconds, failedTest } = getJobPresentation(job, kind, pending, error)
    const practice = variant === 'practice'
    const panelClassName = practice ? 'panel execution-result practice-result' : 'panel history-panel execution-result history-result'
    let resultTitle: string
    let timeTitle: string
    let pendingMessage: string
    if (practice) {
        resultTitle = resultKind === 'GRADING' ? '제출 결과' : '실행 결과'
        timeTitle = resultKind === 'GRADING' ? '테스트 실행 시간 합계' : '코드 실행 시간'
        pendingMessage = resultKind === 'GRADING' ? '전체 테스트를 채점하고 있습니다.' : '선택한 예제를 실행하고 있습니다.'
    } else {
        resultTitle = '제출 결과 '
        timeTitle = '테스트 실행 시간 합계'
        pendingMessage = '전체 테스트를 채점하고 있습니다.'
    }
    const timeLabel = executionSeconds == null ? '— s' : `${executionSeconds.toFixed(3)} s`

    return (
        <section className={panelClassName} aria-live="polite" aria-busy={resultPending}>
            <div className="result-heading">
                <h2>{resultTitle}<span className="result-status" data-status={resultStatus}>{resultLabel}</span></h2>
                <span className="result-time" title={timeTitle}>{timeLabel}</span>
            </div>
            {practice && job && job.code !== currentCode && <p>수정 전 코드의 결과</p>}
            {resultPending && <p>{pendingMessage}</p>}
            {error && <pre role="alert">{error}</pre>}
            {job?.message && <pre>{job.message}</pre>}
            {compilationFailed && preparation && (
                <pre>{preparation.stderr || preparation.stdout || preparation.message || '컴파일하지 못했습니다.'}</pre>
            )}
            {practice && job?.kind === 'RUN' && resultTests.map(test => (
                <pre key={test.testId}>
                    {[test.actual.stdout, test.actual.stderr, test.actual.message
                        || (test.actual.status !== 'COMPLETED' ? '프로그램이 정상적으로 실행되지 않았습니다.' : '')].filter(Boolean).join('\n') || '(출력 없음)'}
                </pre>
            ))}
            {practice && job?.kind === 'GRADING' && !resultPending && !compilationFailed && failedTest && (
                <pre>{failedTest.actual.stderr || failedTest.actual.message || '일부 테스트를 통과하지 못했습니다. 우측 채점 기준을 확인해 주세요.'}</pre>
            )}
        </section>
    )
}
