import { useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { getExercise, getReference } from '../api/exercises'
import type { ExerciseDetails } from '../api/exercises'
import { createRun, getJob } from '../api/runs'
import type { Job } from '../api/runs'
import { createSubmission } from '../api/submissions'

const criterionLabels = {
    PENDING: '대기',
    RUNNING: '판정 중',
    PASSED: '통과',
    FAILED: '실패',
    UNAVAILABLE: '검사 불가'
}

function ExercisePage() {
    const { exerciseId } = useParams()
    const [searchParams, setSearchParams] = useSearchParams()
    const activeTab = searchParams.get('tab') === 'training' ? 'training' : 'learning'
    const [stepIndex, setStepIndex] = useState(0)
    const [code, setCode] = useState('')
    const [saveMessage, setSaveMessage] = useState('이 브라우저에 초안이 저장됩니다.')
    const [referenceOpen, setReferenceOpen] = useState(false)
    const [referenceViewed, setReferenceViewed] = useState(false)
    const [expandedCriterionId, setExpandedCriterionId] = useState<string | null>(null)
    const [exampleIndex, setExampleIndex] = useState(0)
    const [exercise, setExercise] = useState<ExerciseDetails | null>(null)
    const [contentError, setContentError] = useState('')
    const [referenceCode, setReferenceCode] = useState('')
    const [referenceLoading, setReferenceLoading] = useState(false)
    const [executionError, setExecutionError] = useState('')
    const [jobId, setJobId] = useState<string | null>(null)
    const [gradingJobId, setGradingJobId] = useState<string | null>(null)
    const [gradingJob, setGradingJob] = useState<Job | null>(null)
    const [job, setJob] = useState<Job | null>(null)
    const [runBusy, setRunBusy] = useState(false)
    const requests = useRef<AbortController | null>(null)
    const draftKey = exercise ? `algosteady:${exercise.id}:${exercise.version}:c:draft` : ''
    const jobKey = exercise ? `algosteady:${exercise.id}:${exercise.version}:c:last-job` : ''

    const gradingJobKey = exercise ? `algosteady:${exercise.id}:${exercise.version}:c:last-grading-job` : ''

    // URL 변경 시 요청을 취소하고 해당 과제의 초안과 마지막 작업을 복구한다.
    useEffect(() => {
        const controller = new AbortController()
        requests.current = controller

        getExercise(exerciseId ?? '', controller.signal)
            .then(content => {
                if (controller.signal.aborted) {
                    return
                }
                setExercise(content)
                setContentError('')
                setStepIndex(0)
                setExampleIndex(0)
                setExpandedCriterionId(null)
                setReferenceOpen(false)
                setReferenceViewed(false)
                setReferenceCode('')
                setReferenceLoading(false)
                setJob(null)
                setGradingJob(null)
                setExecutionError('')
                let draft = content.starterCode
                let savedJob = null
                let savedGradingJob = null
                try {
                    draft = localStorage.getItem(`algosteady:${content.id}:${content.version}:c:draft`) ?? draft
                    savedJob = localStorage.getItem(`algosteady:${content.id}:${content.version}:c:last-job`)
                    savedGradingJob = localStorage.getItem(`algosteady:${content.id}:${content.version}:c:last-grading-job`)
                } catch {
                    setSaveMessage('브라우저 저장소를 사용할 수 없습니다.')
                }
                setCode(draft)
                setJobId(savedJob)
                setGradingJobId(savedGradingJob)
                setRunBusy(Boolean(savedJob))
            })
            .catch(error => {
                if (!controller.signal.aborted) {
                    setContentError(error.message)
                }
            })

        return () => controller.abort()
    }, [exerciseId])

    // 마지막 작업과 채점 결과를 복구하고 진행 중인 작업만 재조회한다.
    useEffect(() => {
        if ((!jobId && !gradingJobId) || !exercise || exercise.id !== exerciseId) {
            return
        }
        const controller = new AbortController()
        const pendingIds = new Set([jobId, gradingJobId].filter((id): id is string => Boolean(id)))
        let timer: ReturnType<typeof setTimeout>

        async function poll() {
            try {
                const responses = await Promise.all([...pendingIds].map(id => getJob(id, controller.signal)))
                if (controller.signal.aborted) {
                    return
                }
                for (const result of responses) {
                    if (result.exerciseId !== exercise!.id || result.contentVersion !== exercise!.version || result.language !== 'c') {
                        throw new Error('현재 과제와 실행 작업의 정보가 다릅니다.')
                    }
                    if (result.jobId === jobId) {
                        setJob(result)
                    }
                    if (result.jobId === gradingJobId) {
                        setGradingJob(result)
                    }
                    if (result.status === 'COMPLETED' || result.status === 'FAILED') {
                        pendingIds.delete(result.jobId)
                        if (result.jobId === jobId) {
                            setRunBusy(false)
                        }
                    }
                }
                if (pendingIds.size > 0) {
                    timer = setTimeout(poll, 500)
                }
            } catch (error) {
                if (!controller.signal.aborted) {
                    setExecutionError(error instanceof Error ? error.message : '실행 결과를 조회하지 못했습니다.')
                    setRunBusy(false)
                }
            }
        }

        void poll()

        return () => {
            controller.abort()
            clearTimeout(timer)
        }
    }, [jobId, gradingJobId, exercise, exerciseId])

    // 공개 예시는 실행 요청으로, 전체 테스트는 새 제출로 등록한다.
    async function handleRun(kind: 'RUN' | 'GRADING') {
        if (!exercise || !requests.current || runBusy) {
            return
        }
        const signal = requests.current.signal
        setRunBusy(true)
        setExecutionError('')
        setJob(null)
        setJobId(null)
        if (kind === 'GRADING') {
            setExpandedCriterionId(null)
            setGradingJob(null)
            setGradingJobId(null)
        }
        try {
            const response = kind === 'GRADING'
                ? await createSubmission({
                    exerciseId: exercise.id,
                    contentVersion: exercise.version,
                    language: 'c',
                    code,
                    referenceViewed
                }, signal)
                : await createRun({
                    exerciseId: exercise.id,
                    contentVersion: exercise.version,
                    language: 'c',
                    code,
                    exampleId: exercise.publicExamples[exampleIndex].id
                }, signal)
            if (signal.aborted) {
                return
            }
            setJobId(response.jobId)
            if (kind === 'GRADING') {
                setGradingJobId(response.jobId)
            }
            try {
                localStorage.setItem(jobKey, response.jobId)
                if (kind === 'GRADING') {
                    localStorage.setItem(gradingJobKey, response.jobId)
                }
            } catch {
                setSaveMessage('마지막 작업 ID를 저장하지 못했습니다. 새로고침 후 자동 복구할 수 없습니다.')
            }
        } catch (error) {
            if (!signal.aborted) {
                setExecutionError(error instanceof Error ? error.message : '실행 요청을 보내지 못했습니다.')
                setRunBusy(false)
            }
        }
    }

    // 작성한 코드를 브라우저에 저장한다.
    function handleCodeChange(value: string) {
        setCode(value)
        try {
            localStorage.setItem(draftKey, value)
            setSaveMessage('초안 저장됨 · 이 브라우저')
        } catch {
            setSaveMessage('초안을 저장하지 못했습니다. 코드를 별도로 보관해 주세요.')
        }
    }

    // 기준 코드 표시와 열람 여부를 갱신한다.
    async function handleReferenceToggle() {
        if (referenceOpen) {
            setReferenceOpen(false)
            return
        }
        if (!exercise || !requests.current) {
            return
        }
        const signal = requests.current.signal
        setReferenceLoading(true)
        try {
            const reference = await getReference(exercise.id, exercise.version, signal)
            if (signal.aborted) {
                return
            }
            setReferenceCode(reference.code)
            setReferenceOpen(true)
            setReferenceViewed(true)
        } catch (error) {
            if (!signal.aborted) {
                setExecutionError(error instanceof Error ? error.message : '기준 코드를 조회하지 못했습니다.')
            }
        } finally {
            if (!signal.aborted) {
                setReferenceLoading(false)
            }
        }
    }

    // 같은 과제의 탭을 URL에 반영한다.
    function handleTabChange(tab: 'learning' | 'training') {
        const params = new URLSearchParams(searchParams)
        if (tab === 'training') {
            params.set('tab', 'training')
        } else {
            params.delete('tab')
        }
        setSearchParams(params)
    }

    if (contentError || !exercise || exerciseId !== exercise.id) {
        return (
            <main className="workspace">
                <section className="panel unavailable-page">
                    <h1>{contentError || '과제를 불러오는 중입니다.'}</h1>
                    <p>과제 목록에서 학습할 항목을 선택해 주세요.</p>
                    <Link className="primary-button" to="/exercises">과제 목록으로</Link>
                </section>
            </main>
        )
    }

    const publicExamples = exercise.publicExamples
    const step = exercise.learning.steps[stepIndex]
    const example = publicExamples[exampleIndex]

    return (
        <main className="workspace">
            {executionError && <p role="alert">{executionError}</p>}
            <div className="history-link"><Link to={`/exercises/${exerciseId}/submissions`}>제출 기록 보기 →</Link></div>
            {/* 과제 제목 */}
            <div className="breadcrumb">
                <Link to="/exercises">자료구조</Link> <span>/</span> 스택 <span>/</span> <strong>고정 배열 스택</strong>
            </div>
            <section className="exercise-heading">
                <div>
                    <div className="eyebrow">DATA STRUCTURES · 01</div>
                    <h1>
                        {exercise.title}
                        <span className="language-badge">C17</span>
                    </h1>
                    <p>쌓고, 꺼내고, 다시 쌓으며 스택의 원리를 익혀보세요.</p>
                </div>
                <button className="primary-button" onClick={() => handleTabChange(activeTab === 'learning' ? 'training' : 'learning')}>
                    {activeTab === 'learning' ? '직접 구현하기 →' : '학습 내용 보기'}
                </button>
            </section>

            {/* 학습·훈련 탭 */}
            <div className="tabs" role="tablist" aria-label="과제 화면">
                <button
                    id="learning-tab"
                    role="tab"
                    aria-selected={activeTab === 'learning'}
                    aria-controls="exercise-panel"
                    className={activeTab === 'learning' ? 'active' : ''}
                    onClick={() => handleTabChange('learning')}
                >
                    01 <span>학습하기</span>
                </button>
                <button
                    id="training-tab"
                    role="tab"
                    aria-selected={activeTab === 'training'}
                    aria-controls="exercise-panel"
                    className={activeTab === 'training' ? 'active' : ''}
                    onClick={() => handleTabChange('training')}
                >
                    02 <span>구현 훈련</span>
                </button>
                <span className="tab-note">배열 · LIFO · 용량 100</span>
            </div>

            <div id="exercise-panel" role="tabpanel" aria-labelledby={`${activeTab}-tab`} className="content-grid">
                <div className="main-column">
                    {activeTab === 'learning' ? (
                        <>
                            {/* 스택 개념 */}
                            <section className="panel concept-panel">
                                <div className="section-label">먼저, 원리부터</div>
                                <h2>
                                    마지막에 넣은 값이<br />
                                    가장 먼저 나옵니다<span className="accent-dot">.</span>
                                </h2>
                                {exercise.learning.concept.paragraphs.map(paragraph => (
                                    <p key={paragraph}>{paragraph}</p>
                                ))}
                                <div className="concept-tags">
                                    <span>Last In, First Out</span>
                                    <span>삽입과 삭제는 맨 위에서</span>
                                </div>
                            </section>

                            {/* 연산별 상태 변화 */}
                            <section className="panel simulation-panel">
                                <div className="panel-heading">
                                    <div>
                                        <div className="section-label">눈으로 따라가기</div>
                                        <h2>스택은 이렇게 움직여요</h2>
                                    </div>
                                    <span className="step-counter">{stepIndex + 1} / {exercise.learning.steps.length}</span>
                                </div>
                                <div className="simulation">
                                    <div className="stack-diagram" aria-label={`스택 값 ${step.values.join(', ') || '없음'}, top ${step.top}`}>
                                        <div className="stack-cell empty-cell">다음 값이 들어올 자리</div>
                                        {[...step.values].reverse().map((value, index) => (
                                            <div className={`stack-cell ${index === 0 ? 'top-cell' : ''}`} key={step.values.length - index}>
                                                <span className="cell-index">[{step.values.length - index - 1}]</span>
                                                {value}
                                                {index === 0 && (
                                                    <span className="top-marker">← top</span>
                                                )}
                                            </div>
                                        ))}
                                        {step.values.length === 0 && (
                                            <div className="empty-stack">비어 있는 스택</div>
                                        )}
                                        <div className="stack-base">data[100]</div>
                                    </div>
                                    <div className="operation-info">
                                        <span className="operation-caption">현재 연산</span>
                                        <code>{step.operation}({step.argument ?? ''})</code>
                                        <div className="operation-values">
                                            <span>top <strong>{step.top}</strong></span>
                                            <span>반환값 <strong>{step.result ?? '—'}</strong></span>
                                        </div>
                                        <p>{exercise.learning.operations.find(operation => operation.function === step.operation)?.description}</p>
                                    </div>
                                </div>
                                <div className="step-controls">
                                    <button className="text-button" disabled={stepIndex === 0} onClick={() => setStepIndex(stepIndex - 1)}>
                                        ← 이전
                                    </button>
                                    <div className="step-dots">
                                        {exercise.learning.steps.map((item, index) => (
                                            <button
                                                key={`${item.operation}-${index}`}
                                                aria-label={`${index + 1}단계: ${item.operation}`}
                                                aria-current={stepIndex === index ? 'step' : undefined}
                                                className={stepIndex === index ? 'selected' : ''}
                                                onClick={() => setStepIndex(index)}
                                            />
                                        ))}
                                    </div>
                                    <button className="text-button" disabled={stepIndex === exercise.learning.steps.length - 1} onClick={() => setStepIndex(stepIndex + 1)}>
                                        다음 →
                                    </button>
                                </div>
                            </section>

                            {/* 함수별 구현 규격 */}
                            <section className="panel">
                                <div className="section-label">구현할 연산</div>
                                <h2>여섯 개의 함수로 완성하기</h2>
                                <div className="function-list">
                                    {exercise.contract.functions.map(item => (
                                        <details key={item.name}>
                                            <summary>
                                                <code>{item.signature}</code>
                                                <span>+</span>
                                            </summary>
                                            <p>{item.behavior}</p>
                                        </details>
                                    ))}
                                </div>
                            </section>
                        </>
                    ) : (
                        <>
                            {/* 구현 훈련 안내 */}
                            <section className="panel training-intro">
                                <div className="section-label">이제, 직접 작성해 보세요</div>
                                <h2>선언 아래에 함수 전체를 구현하세요.</h2>
                                <p>{exercise.contract.scope} 입력 처리와 <code>main()</code>은 테스트 호출 코드가 담당합니다.</p>
                            </section>

                            {/* 코드 작성과 초안 저장 */}
                            <section className="panel editor-panel">
                                <div className="editor-heading">
                                    <span>
                                        <span className="file-dot" />
                                        submission.c
                                    </span>
                                    <span>C17 · 시작 코드 제공</span>
                                </div>
                                <label className="sr-only" htmlFor="code-editor">C 스택 구현 코드</label>
                                <textarea
                                    id="code-editor"
                                    className="code-editor"
                                    value={code}
                                    onChange={event => handleCodeChange(event.target.value)}
                                    spellCheck={false}
                                    autoCapitalize="off"
                                    autoCorrect="off"
                                />
                                <div className="editor-footer">
                                    <span aria-live="polite">{saveMessage}</span>
                                    <span>{code.split('\n').length}줄</span>
                                </div>
                            </section>

                            {/* 공개 예시와 실행 버튼 */}
                            <section className="panel example-panel">
                                <div className="panel-heading">
                                    <h2>공개 예시</h2>
                                    <label className="sr-only" htmlFor="example-select">공개 테스트 선택</label>
                                    <select id="example-select" value={exampleIndex} onChange={event => setExampleIndex(Number(event.target.value))}>
                                        {publicExamples.map((item, index) => (
                                            <option key={item.id} value={index}>{item.description}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="example-columns">
                                    <div>
                                        <h3>입력</h3>
                                        <pre>{example.input}</pre>
                                    </div>
                                    <div>
                                        <h3>기대 결과</h3>
                                        <pre>{example.expected.stdout || '(출력 없음)'}</pre>
                                    </div>
                                </div>
                                <div className="execution-actions">
                                    <button className="secondary-button" disabled={runBusy} onClick={() => handleRun('RUN')}>{runBusy ? '실행 중…' : '컴파일·실행'}</button>
                                    <button className="primary-button" disabled={runBusy} onClick={() => handleRun('GRADING')}>채점하기</button>
                                    <p>컴파일·실행은 선택한 예시를, 채점하기는 전체 테스트를 검사합니다.</p>
                                </div>
                            </section>
                            {job && (
                                <section className="panel execution-result" aria-live="polite">
                                    <h2>{job.kind === 'GRADING' ? '채점 결과' : '실행 결과'} · {job.status}</h2>
                                    {job.code !== code && <p>수정 전 코드의 결과</p>}
                                    {job.kind === 'RUN' && <p>공개 예시: {publicExamples.find(item => item.id === job.exampleId)?.description ?? job.exampleId}</p>}
                                    {job.message && <p>{job.message}</p>}
                                    {job.preparation && (
                                        <div>
                                            <h3>컴파일 · {job.preparation.status}</h3>
                                            <pre>{job.preparation.stdout}{job.preparation.stderr}</pre>
                                            <p>종료 코드: {job.preparation.exitCode ?? '—'} · {job.preparation.message}</p>
                                        </div>
                                    )}
                                    {job.kind === 'RUN' && job.tests.map(test => (
                                        <div key={test.testId}>
                                            <h3>{test.passed ? '예시 일치' : '예시 불일치 또는 실행 불가'} · {test.actual.status}</h3>
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
                                </section>
                            )}
                            {gradingJob && (
                                <section className="panel execution-result grading-result" aria-live="polite">
                                    <h2>기준별 채점 상세</h2>
                                    <p>통과 {gradingJob.criteria.filter(item => item.status === 'PASSED').length} / {gradingJob.criteria.length}</p>
                                    {gradingJob.code !== code && <p>수정 전 코드의 결과</p>}
                                    {gradingJob.message && <p>{gradingJob.message}</p>}
                                    {job?.jobId !== gradingJob.jobId && gradingJob.preparation && (
                                        <div>
                                            <h3>컴파일 · {gradingJob.preparation.status}</h3>
                                            <pre>{gradingJob.preparation.stdout}{gradingJob.preparation.stderr}</pre>
                                        </div>
                                    )}
                                    {gradingJob.criteria.map(result => (
                                        <details
                                            className="criterion-detail"
                                            id={`criterion-${result.criterionId}`}
                                            key={result.criterionId}
                                            open={expandedCriterionId === result.criterionId}
                                            onToggle={event => {
                                                if (event.currentTarget.open) {
                                                    setExpandedCriterionId(result.criterionId)
                                                }
                                                else if (expandedCriterionId === result.criterionId) {
                                                    setExpandedCriterionId(null)
                                                }
                                            }}
                                        >
                                            <summary>
                                                {exercise.criteria.find(item => item.id === result.criterionId)?.title} · {criterionLabels[result.status]}
                                                <span> {result.passedTests} / {result.totalTests}</span>
                                            </summary>
                                            {result.status === 'UNAVAILABLE' && <p>컴파일 실패, 실행 환경 장애 또는 작업 중단으로 검사를 완료하지 못했습니다.</p>}
                                            {gradingJob.tests.filter(test => test.criterionId === result.criterionId && !test.passed).map(test => (
                                                <div className="failed-test" key={test.testId}>
                                                    <h3>{test.description} · {test.actual.status}</h3>
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
                            )}
                        </>
                    )}

                    {/* 기준 코드 열람 */}
                    <section className="panel reference-panel">
                        <div className="panel-heading">
                            <div>
                                <h2>기준 구현</h2>
                                <p>먼저 직접 작성한 뒤 비교해 보세요.</p>
                            </div>
                            <button
                                className="secondary-button"
                                aria-expanded={referenceOpen}
                                aria-controls="reference-code"
                                onClick={handleReferenceToggle}
                                disabled={referenceLoading}
                            >
                                {referenceOpen ? '코드 접기' : '기준 코드 보기'}
                            </button>
                        </div>
                        {referenceOpen && (
                            <pre id="reference-code">{referenceCode}</pre>
                        )}
                        {referenceViewed && (
                            <span className="reference-note">이번 화면에서 기준 코드를 열람했습니다.</span>
                        )}
                    </section>
                </div>

                <aside className="side-column">
                    {/* 채점 기준 */}
                    <section className="panel criteria-panel">
                        <div className="section-label">훈련의 체크포인트</div>
                        <h2>채점 기준 <span>{exercise.criteria.length}</span></h2>
                        <p>각 기준에 연결된 테스트로<br />구현을 확인합니다.</p>
                        <ol className="criteria-list">
                            {exercise.criteria.map(criterion => {
                                const result = gradingJob?.criteria.find(item => item.criterionId === criterion.id)
                                const status = result?.status ?? 'PENDING'
                                return (
                                    <li key={criterion.id} data-status={status}>
                                        <span className="criterion-circle" />
                                        <div>
                                            {gradingJob && activeTab === 'training' ? (
                                                <a href={`#criterion-${criterion.id}`} onClick={() => setExpandedCriterionId(criterion.id)}><strong>{criterion.title}</strong></a>
                                            ) : <strong>{criterion.title}</strong>}
                                            <p>{criterion.description}</p>
                                            {result && <p>{result.passedTests} / {result.totalTests} 테스트 통과</p>}
                                        </div>
                                        <span className="waiting-label">{criterionLabels[status]}</span>
                                    </li>
                                )
                            })}
                        </ol>
                        <div className="criteria-note">
                            {gradingJob ? `통과 ${gradingJob.criteria.filter(item => item.status === 'PASSED').length} / ${gradingJob.criteria.length}` : '아직 채점하지 않았습니다.'}
                            {gradingJob && gradingJob.code !== code && <p>수정 전 코드의 결과</p>}
                        </div>
                    </section>

                    {/* 구현 시 주의사항 */}
                    <section className="contract-note">
                        <span className="section-label">기억해 두세요</span>
                        <h3>빈 스택의 top은 -1</h3>
                        <p>값 하나가 있으면 0, 가득 차면 99입니다. 저장된 값의 개수는 <code>top + 1</code>이에요.</p>
                        <div className="contract-divider" />
                        <h3>오류도 구현의 일부</h3>
                        <p>포화 상태의 push는 값을 유지하고 반환합니다. 빈 상태의 pop·peek는 오류 출력 후 종료합니다.</p>
                    </section>
                </aside>
            </div>

            <footer className="workspace-footer">
                <span>작은 매일이 쌓여, 단단한 기반이 됩니다.</span>
                <span>Algosteady · C 자료구조 훈련</span>
            </footer>
        </main>
    )
}

export default ExercisePage
