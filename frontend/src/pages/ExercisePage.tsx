import type { ReactNode } from 'react'
import { useEffect, useRef, useState } from 'react'
import Editor, { loader } from '@monaco-editor/react'
import * as monaco from 'monaco-editor'
import EditorWorker from 'monaco-editor/editor/editor.worker?worker'
import { Link, useParams, useSearchParams } from 'react-router'
import { ApiError, getExercise, getReference } from '../api/exercises'
import type { ExerciseDetails } from '../api/exercises'
import { createRun, getJob } from '../api/runs'
import type { Job } from '../api/runs'
import ExecutionResultPanel from '../components/ExecutionResultPanel'
import { createSubmission } from '../api/submissions'
import LearningVisualization from '../components/LearningVisualization'
import { getExercisePresentation } from '../exercisePresentation'

// 편집기와 작업자의 로컬 번들 연결
self.MonacoEnvironment = {
    getWorker() {
        return new EditorWorker()
    }
}
loader.config({ monaco })

const criterionLabels = {
    PENDING: '대기',
    RUNNING: '채점 중',
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
    const [saveError, setSaveError] = useState('')
    const [referenceOpen, setReferenceOpen] = useState(false)
    const [referenceViewed, setReferenceViewed] = useState(false)
    const [exampleIndex, setExampleIndex] = useState(0)
    const [exercise, setExercise] = useState<ExerciseDetails | null>(null)
    const [contentError, setContentError] = useState('')
    const [referenceCode, setReferenceCode] = useState('')
    const [referenceLoading, setReferenceLoading] = useState(false)
    const [referenceError, setReferenceError] = useState('')
    const [executionError, setExecutionError] = useState('')
    const [gradingError, setGradingError] = useState('')
    const [jobId, setJobId] = useState<string | null>(null)
    const [gradingJobId, setGradingJobId] = useState<string | null>(null)
    const [gradingJob, setGradingJob] = useState<Job | null>(null)
    const [job, setJob] = useState<Job | null>(null)
    const [runBusy, setRunBusy] = useState(false)
    const [requestKind, setRequestKind] = useState<'RUN' | 'GRADING' | null>(null)
    const requests = useRef<AbortController | null>(null)
    const draftKey = exercise ? `algosteady:${exercise.id}:${exercise.version}:c:draft` : ''
    const jobKey = exercise ? `algosteady:${exercise.id}:${exercise.version}:c:last-job` : ''

    const gradingJobKey = exercise ? `algosteady:${exercise.id}:${exercise.version}:c:last-grading-job` : ''

    // 과제 조회 및 저장된 초안·마지막 작업 복구, URL 변경 시 요청 취소
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
                setReferenceOpen(false)
                setReferenceViewed(false)
                setReferenceCode('')
                setReferenceLoading(false)
                setReferenceError('')
                setJob(null)
                setGradingJob(null)
                setExecutionError('')
                setGradingError('')
                setRequestKind(null)
                // 과제·버전별 저장된 초안과 마지막 작업 ID 복구
                let draft = content.starterCode
                let savedJob = null
                let savedGradingJob = null
                try {
                    draft = localStorage.getItem(`algosteady:${content.id}:${content.version}:c:draft`) ?? draft
                    savedJob = localStorage.getItem(`algosteady:${content.id}:${content.version}:c:last-job`)
                    savedGradingJob = localStorage.getItem(`algosteady:${content.id}:${content.version}:c:last-grading-job`)
                } catch {
                    setSaveError('브라우저 저장소를 사용할 수 없습니다.')
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

    // 실행·제출별 독립 조회, 한쪽 실패 시 다른 쪽 폴링 유지
    useEffect(() => {
        if ((!jobId && !gradingJobId) || !exercise || exercise.id !== exerciseId) {
            return
        }
        const controller = new AbortController()
        const ids = new Set([jobId, gradingJobId].filter((id): id is string => Boolean(id)))
        const timers = new Map<string, ReturnType<typeof setTimeout>>()

        async function poll(id: string) {
            try {
                const result = await getJob(id, controller.signal)
                if (controller.signal.aborted) {
                    return
                }
                // 현재 과제의 작업만 해당 결과 영역에 반영
                if (result.exerciseId !== exercise!.id || result.contentVersion !== exercise!.version || result.language !== 'c') {
                    throw new Error('현재 과제와 실행 작업의 정보가 다릅니다.')
                }
                if (id === jobId) {
                    setJob(result)
                    setExecutionError('')
                }
                if (id === gradingJobId) {
                    setGradingJob(result)
                    setGradingError('')
                }
                if (result.status === 'COMPLETED' || result.status === 'FAILED') {
                    if (id === jobId) {
                        setRunBusy(false)
                    }
                    return
                }
                timers.set(id, setTimeout(() => void poll(id), 500))
            } catch (error) {
                if (controller.signal.aborted) {
                    return
                }
                const message = error instanceof Error ? error.message : '작업 결과를 조회하지 못했습니다.'
                if (id === jobId) {
                    setExecutionError(message)
                    setRunBusy(false)
                }
                if (id === gradingJobId) {
                    setGradingError(`제출 결과를 조회하지 못했습니다. ${message}`)
                }

                // 없는 작업의 복구 ID만 제거, 통신·서버 오류의 ID는 보존
                if (error instanceof ApiError && error.status === 404) {
                    if (id === jobId) {
                        setRequestKind(id === gradingJobId ? 'GRADING' : 'RUN')
                        setJobId(null)
                        setJob(null)
                    }
                    if (id === gradingJobId) {
                        setGradingJobId(null)
                        setGradingJob(null)
                    }
                    try {
                        if (id === jobId && localStorage.getItem(jobKey) === id) {
                            localStorage.removeItem(jobKey)
                        }
                        if (id === gradingJobId && localStorage.getItem(gradingJobKey) === id) {
                            localStorage.removeItem(gradingJobKey)
                        }
                    } catch {
                        setSaveError('없는 작업의 복구 ID를 브라우저에서 지우지 못했습니다.')
                    }
                }
            }
        }

        for (const id of ids) {
            void poll(id)
        }

        return () => {
            controller.abort()
            timers.forEach(timer => clearTimeout(timer))
        }
    }, [jobId, gradingJobId, exercise, exerciseId, jobKey, gradingJobKey])

    // 공개 예제 실행 또는 전체 테스트 채점 제출 등록
    async function handleRun(kind: 'RUN' | 'GRADING') {
        if (!exercise || !requests.current || runBusy) {
            return
        }
        const signal = requests.current.signal
        setRunBusy(true)
        setRequestKind(kind)
        setExecutionError('')
        setJob(null)
        setJobId(null)
        if (kind === 'GRADING') {
            setGradingError('')
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
            // 새로고침 후 복구할 마지막 작업 ID 저장
            try {
                localStorage.setItem(jobKey, response.jobId)
                if (kind === 'GRADING') {
                    localStorage.setItem(gradingJobKey, response.jobId)
                }
            } catch {
                setSaveError('마지막 작업 ID를 저장하지 못했습니다. 새로고침 후 자동 복구할 수 없습니다.')
            }
        } catch (error) {
            if (!signal.aborted) {
                setExecutionError(error instanceof Error ? error.message : '실행 요청을 보내지 못했습니다.')
                setRunBusy(false)
            }
        }
    }

    // 작성 코드의 브라우저 초안 저장
    function handleCodeChange(value: string) {
        setCode(value)
        try {
            localStorage.setItem(draftKey, value)
            setSaveError('')
        } catch {
            setSaveError('초안을 저장하지 못했습니다. 코드를 별도로 보관해 주세요.')
        }
    }

    // 기준 코드 조회 및 표시·열람 여부 갱신
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
        setReferenceError('')
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
                setReferenceError(error instanceof Error ? error.message : '기준 코드를 조회하지 못했습니다.')
            }
        } finally {
            if (!signal.aborted) {
                setReferenceLoading(false)
            }
        }
    }

    // 학습·실습 탭 선택을 URL에 반영
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
    const example = publicExamples[exampleIndex]
    const executedExampleIndex = publicExamples.findIndex(item => item.id === job?.exampleId)
    const resultExampleNumber = job?.kind === 'RUN' && executedExampleIndex >= 0 ? executedExampleIndex + 1 : undefined
    const isQueue = exercise.category === 'queue'
    const isDeque = exercise.category === 'deque'
    const isLinkedStack = exercise.category === 'stack' && exercise.contract.storage === 'linked'
    const hasLinkedEnds = (isQueue || isDeque) && exercise.contract.storage === 'linked'
    const isRecursion = exercise.category === 'recursion'
    const presentation = getExercisePresentation(exercise)
    const categoryLabel = presentation.label
    const restoredKind = jobId && jobId === gradingJobId ? 'GRADING' : 'RUN'
    const resultKind = job?.kind ?? requestKind ?? restoredKind

    // 등록된 실습 안내 우선 표시, 안내가 없는 스택·선형 큐만 기본 문구 적용
    let practiceNotes: ReactNode
    if (exercise.learning.notes?.length) {
        practiceNotes = exercise.learning.notes.map((note, index) => (
            <div key={note.title}>
                {index > 0 && <div className="contract-divider" />}
                <h3>{note.title}</h3>
                <p>{note.description}</p>
            </div>
        ))
    } else {
        switch (exercise.category) {
            case 'queue':
                if (!hasLinkedEnds && !exercise.contract.circular) {
                    practiceNotes = (
                        <>
                            <h3>front는 다음 값의 바로 앞</h3>
                            <p>초기 front와 rear는 -1입니다. front와 rear가 같으면 비어 있고, 저장된 값의 개수는 <code>rear - front</code>입니다.</p>
                            <div className="contract-divider" />
                            <h3>앞 공간은 재사용하지 않습니다</h3>
                            <p>rear가 마지막 칸에 도달하면 포화 상태입니다. 포화 상태의 enqueue와 빈 상태의 dequeue는 오류 출력 후 종료합니다.</p>
                        </>
                    )
                }
                break
            case 'stack':
                if (!isLinkedStack) {
                    practiceNotes = (
                        <>
                            <h3>빈 스택의 top은 -1</h3>
                            <p>
                                값 하나가 있으면 0, 가득 차면 {exercise.contract.initialCapacity !== undefined ? 'capacity - 1' : (exercise.contract.capacity ?? 0) - 1}입니다.
                                {' '}저장된 값의 개수는 <code>top + 1</code>이에요.
                            </p>
                            <div className="contract-divider" />
                            <h3>오류도 구현의 일부</h3>
                            <p>
                                {exercise.contract.growthFactor !== undefined
                                    ? `포화 상태의 push는 용량을 ${exercise.contract.growthFactor}배로 늘립니다. 메모리 할당 실패와 빈 상태의 pop·peek는 오류 출력 후 종료합니다.`
                                    : '포화 상태의 push는 값을 유지하고 반환합니다. 빈 상태의 pop·peek는 오류 출력 후 종료합니다.'}
                            </p>
                        </>
                    )
                }
                break
        }
    }

    return (
        <main className="workspace">
            {activeTab === 'learning' && executionError && <p role="alert">{executionError}</p>}
            {saveError && <p role="alert">{saveError}</p>}
            {/* 과제 제목 */}
            <div className="breadcrumb">
                <Link to="/exercises">과제 목록</Link> <span>/</span> {categoryLabel} <span>/</span> <strong>{exercise.title}</strong>
            </div>
            <section className="exercise-heading">
                <div>
                    <div className="eyebrow">{presentation.family}</div>
                    <h1>
                        {exercise.title}
                        <span className="language-badge">C17</span>
                    </h1>
                    <p>{presentation.introduction}</p>
                </div>
                <Link className="primary-button" to={`/exercises/${exerciseId}/submissions`}>제출 기록</Link>
            </section>

            {/* 학습·실습 탭 */}
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
                    02 <span>실습하기</span>
                </button>
                <span className="tab-note">
                    {presentation.note}
                </span>
            </div>

            <div id="exercise-panel" role="tabpanel" aria-labelledby={`${activeTab}-tab`} className="content-grid" data-tab={activeTab}>
                <div className="main-column">
                    {activeTab === 'learning' ? (
                        <>
                            {/* 핵심 개념 */}
                            <section className="panel concept-panel">
                                <div className="section-label">먼저, 원리부터</div>
                                <h2>{exercise.learning.concept.title}</h2>
                                {exercise.learning.concept.paragraphs.map(paragraph => (
                                    <p key={paragraph}>{paragraph}</p>
                                ))}
                                <div className="concept-tags">
                                    <span>{presentation.tag}</span>
                                    <span>{presentation.detail}</span>
                                </div>
                            </section>

                            {/* 연산별 상태 변화 */}
                            <section className="panel simulation-panel">
                                <div className="panel-heading">
                                    <div>
                                        <div className="section-label">{isRecursion ? '기준 코드 호출 예시' : '눈으로 따라가기'}</div>
                                        <h2>{presentation.simulationTitle}</h2>
                                    </div>
                                    <span className="step-counter">{stepIndex + 1} / {exercise.learning.steps.length}</span>
                                </div>
                                <LearningVisualization exercise={exercise} stepIndex={stepIndex} />
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
                                <h2>{exercise.contract.functions.length}개의 함수로 완성하기</h2>
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
                            {/* 실습 안내 */}
                            <section className="panel training-intro">
                                <div className="section-label">이제, 직접 작성해 보세요</div>
                                <h2>선언 아래에 함수 전체를 구현하세요.</h2>
                                <p>{exercise.contract.scope} 입력 처리와 <code>main()</code>은 테스트 호출 코드가 담당합니다.</p>
                            </section>

                            {/* 입출력 예제 */}
                            <section className="panel example-panel">
                                <div className="panel-heading">
                                    <h2>입출력 예제</h2>
                                    <label className="sr-only" htmlFor="example-select">예제 선택</label>
                                    <select id="example-select" value={exampleIndex} onChange={event => setExampleIndex(Number(event.target.value))}>
                                        {publicExamples.map((item, index) => (
                                            <option key={item.id} value={index}>예제 {index + 1}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="example-columns">
                                    <div>
                                        <h3>예제 입력</h3>
                                        <pre>{example.input}</pre>
                                    </div>
                                    <div>
                                        <h3>예제 출력</h3>
                                        <pre>{example.expected.stdout || '(출력 없음)'}</pre>
                                    </div>
                                </div>
                            </section>

                            {/* 코드 작성과 초안 저장 */}
                            <section className="panel editor-panel">
                                <div className="editor-heading">
                                    <div className="editor-file">
                                        <span><span className="file-dot" />submission.c</span>
                                        <span>C17 · 시작 코드 제공</span>
                                    </div>
                                    <div className="execution-actions">
                                        <button className="secondary-button" disabled={runBusy} onClick={() => handleRun('RUN')}>{runBusy && resultKind === 'RUN' ? '실행 중…' : '실행'}</button>
                                        <button className="primary-button" disabled={runBusy} onClick={() => handleRun('GRADING')}>{runBusy && resultKind === 'GRADING' ? '채점 중…' : '제출'}</button>
                                    </div>
                                </div>
                                <div className="code-editor">
                                    <Editor
                                        height="490px"
                                        language="c"
                                        value={code}
                                        onChange={value => handleCodeChange(value ?? '')}
                                        loading="편집기를 불러오는 중입니다."
                                        options={{
                                            ariaLabel: `C ${categoryLabel} 구현 코드`,
                                            fontSize: 13,
                                            fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", monospace',
                                            lineNumbers: 'on',
                                            tabSize: 4,
                                            insertSpaces: true,
                                            detectIndentation: false,
                                            automaticLayout: true,
                                            minimap: { enabled: false },
                                            scrollBeyondLastLine: false,
                                            padding: { top: 20, bottom: 20 },
                                            quickSuggestions: false,
                                            suggestOnTriggerCharacters: false,
                                            wordBasedSuggestions: 'off',
                                            inlineSuggest: { enabled: false }
                                        }}
                                    />
                                </div>
                            </section>

                            {(job || runBusy || executionError) && (
                                <ExecutionResultPanel
                                    variant="practice"
                                    job={job}
                                    kind={requestKind ?? restoredKind}
                                    pending={runBusy}
                                    error={executionError}
                                    currentCode={code}
                                    exampleNumber={resultExampleNumber}
                                />
                            )}
                        </>
                    )}

                    {/* 학습 탭의 기준 코드 열람 */}
                    {activeTab === 'learning' && (
                        <section className="panel reference-panel">
                            <div className="panel-heading">
                                <div>
                                    <h2>기준 코드</h2>
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
                            {referenceError && <p role="alert">{referenceError}</p>}
                            {referenceOpen && (
                                <pre id="reference-code">{referenceCode}</pre>
                            )}
                        </section>
                    )}
                </div>

                {activeTab === 'training' && (
                    <aside className="side-column">
                        {/* 채점 기준 */}
                        <section className="panel criteria-panel">
                            <div className="section-label">실습 체크포인트</div>
                            <h2>채점 기준 <span>{exercise.criteria.length}</span></h2>
                            {gradingError && <p role="alert">{gradingError}</p>}
                            <p>각 기준에 연결된 테스트로<br />구현을 확인합니다.</p>
                            <ol className="criteria-list">
                                {exercise.criteria.map(criterion => {
                                    const result = gradingJob?.criteria.find(item => item.criterionId === criterion.id)
                                    const status = result?.status ?? 'PENDING'
                                    return (
                                        <li key={criterion.id} data-status={status}>
                                            <span className="criterion-circle" />
                                            <div>
                                                <strong>{criterion.title}</strong>
                                                <p>{criterion.description}</p>
                                                {result && <p>{result.passedTests} / {result.totalTests} 테스트 통과</p>}
                                            </div>
                                            <span className="waiting-label">{criterionLabels[status]}</span>
                                        </li>
                                    )
                                })}
                            </ol>
                            <div className="criteria-note">
                                {gradingJob && <p>최근 제출 결과</p>}
                                {gradingJob ? `통과 ${gradingJob.criteria.filter(item => item.status === 'PASSED').length} / ${gradingJob.criteria.length}` : '아직 제출하지 않았습니다.'}
                                {gradingJob && gradingJob.code !== code && <p>수정 전 코드의 결과</p>}
                            </div>
                        </section>

                        {/* 구현 시 주의사항 */}
                        <section className="contract-note">
                            <span className="section-label">기억해 두세요</span>
                            {practiceNotes}
                        </section>
                    </aside>
                )}
            </div>

            <footer className="workspace-footer">
                <span>작은 매일이 쌓여, 단단한 기반이 됩니다.</span>
                <span>Algosteady · C 구현 실습</span>
            </footer>
        </main>
    )
}

export default ExercisePage
