import { useEffect, useRef, useState } from 'react'
import Editor, { loader } from '@monaco-editor/react'
import * as monaco from 'monaco-editor'
import EditorWorker from 'monaco-editor/editor/editor.worker?worker'
import { Link, useParams, useSearchParams } from 'react-router'
import { getExercise, getReference } from '../api/exercises'
import type { ExerciseDetails } from '../api/exercises'
import { createRun, getJob } from '../api/runs'
import type { Job } from '../api/runs'
import { createSubmission } from '../api/submissions'

// 편집기와 작업자를 로컬 번들에서 불러온다.
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
    const [executionError, setExecutionError] = useState('')
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
                setReferenceOpen(false)
                setReferenceViewed(false)
                setReferenceCode('')
                setReferenceLoading(false)
                setJob(null)
                setGradingJob(null)
                setExecutionError('')
                setRequestKind(null)
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
        setRequestKind(kind)
        setExecutionError('')
        setJob(null)
        setJobId(null)
        if (kind === 'GRADING') {
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
                setSaveError('마지막 작업 ID를 저장하지 못했습니다. 새로고침 후 자동 복구할 수 없습니다.')
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
            setSaveError('')
        } catch {
            setSaveError('초안을 저장하지 못했습니다. 코드를 별도로 보관해 주세요.')
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
    const isQueue = exercise.category === 'queue'
    const isDeque = exercise.category === 'deque'
    const isLinkedList = exercise.category === 'linked-list'
    const isLinkedStack = exercise.category === 'stack' && exercise.contract.storage === 'linked'
    const hasLinkedEnds = (isQueue || isDeque) && exercise.contract.storage === 'linked'
    const isDoublyList = isLinkedList && Boolean(exercise.contract.doubly)
    const isDoublyLinked = isDoublyList || (isDeque && hasLinkedEnds)
    const isCircularList = isLinkedList && Boolean(exercise.contract.circular) && !isDoublyList
    const isRecursion = exercise.category === 'recursion'
    const isTree = exercise.category === 'tree'
    const categoryLabel = isTree ? '트리' : isRecursion ? '재귀' : isLinkedList ? '연결 리스트' : isDeque ? '덱' : isQueue ? '큐' : '스택'
    const stepValues = step.values ?? []
    const treeNodes = (step.tree ?? []).flatMap((value, index) => {
        if (value === null) {
            return []
        }
        const depth = Math.floor(Math.log2(index + 1))
        return [{ value, index, depth, x: (index - (2 ** depth - 1) + 0.5) * 640 / 2 ** depth, y: 40 + depth * 80 }]
    })
    const treeHeight = treeNodes.length ? (Math.max(...treeNodes.map(node => node.depth)) + 1) * 80 + 70 : 0

    const resultKind = job?.kind ?? requestKind ?? (jobId && jobId === gradingJobId ? 'GRADING' : 'RUN')
    const resultPending = runBusy && !executionError
    const resultTests = job?.tests.filter(test => test.actual.status !== 'NOT_EXECUTED') ?? []
    const preparation = job?.preparation
    const compilationFailed = preparation != null && preparation.status !== 'SUCCEEDED'
    const resultError = Boolean(executionError || job?.status === 'FAILED' || compilationFailed
        || job?.criteria.some(criterion => criterion.status === 'UNAVAILABLE')
        || resultTests.some(test => test.actual.status !== 'COMPLETED'))
    const resultFailed = Boolean(job?.criteria.some(criterion => criterion.status === 'FAILED') || resultTests.some(test => !test.passed))
    const resultStatus = resultPending ? 'pending' : resultError || resultFailed ? 'failed' : 'passed'
    let resultLabel = resultPending ? (resultKind === 'GRADING' ? '채점 중…' : '실행 중…')
        : resultError ? '오류' : resultFailed ? (resultKind === 'GRADING' ? '실패' : '불일치')
            : resultKind === 'GRADING' ? '통과' : '정상'

    if (resultKind === 'GRADING' && !resultPending) {
        if (executionError || job?.status === 'FAILED' || preparation?.status === 'ENVIRONMENT_ERROR') {
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
                    <div className="eyebrow">{isRecursion ? 'ALGORITHMS' : 'DATA STRUCTURES'}</div>
                    <h1>
                        {exercise.title}
                        <span className="language-badge">C17</span>
                    </h1>
                    <p>{isTree ? '노드와 자식을 연결하며 트리의 구조를 익혀보세요.' : isRecursion ? '종료 조건과 호출·복귀를 익히고 직접 구현해 보세요.' : isLinkedList ? '노드를 연결하고 끊으며 포인터와 메모리 관리를 익혀보세요.' : isDeque ? '앞과 뒤 양쪽에서 넣고 꺼내며 덱의 원리를 익혀보세요.' : isQueue ? '뒤에 넣고, 앞에서 꺼내며 큐의 원리를 익혀보세요.' : '쌓고, 꺼내고, 다시 쌓으며 스택의 원리를 익혀보세요.'}</p>
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
                    {isTree ? '루트 · 왼쪽 자식 · 오른쪽 자식' : isRecursion ? '종료 조건 · 재귀 호출' : hasLinkedEnds ? `노드 · front와 rear 포인터 · ${isDeque ? '양방향 연결' : 'FIFO'}` : isLinkedStack ? '노드 · top 포인터 · 동적 메모리' : isLinkedList ? isDoublyList ? '헤드 노드 · 양방향 연결' : isCircularList ? '순환 연결 · head는 마지막 노드' : '노드 · 포인터 · 동적 메모리' : exercise.contract.circular
                        ? `원형 배열 · ${isDeque ? '양쪽 삽입·삭제' : 'FIFO'} · 저장 ${exercise.contract.usableCapacity}개` : exercise.contract.initialCapacity !== undefined
                        ? `동적 배열 · LIFO · 초기 용량 ${exercise.contract.initialCapacity}`
                        : `배열 · ${isQueue ? 'FIFO' : 'LIFO'} · 용량 ${exercise.contract.capacity}`}
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
                                    <span>{isTree ? 'Binary Tree' : isRecursion ? 'Recursion' : isLinkedList ? isDoublyList ? 'Doubly Linked List' : isCircularList ? 'Circular Linked List' : 'Singly Linked List' : isDeque ? 'Double-ended Queue' : isQueue ? 'First In, First Out' : 'Last In, First Out'}</span>
                                    <span>{isTree ? '노드마다 최대 두 자식' : isRecursion ? '종료 조건과 작은 문제' : hasLinkedEnds ? isDeque ? '양쪽 끝에서 삽입 · 삭제 · 조회' : 'front에서 꺼내고 rear 뒤에 연결' : isLinkedStack ? 'top에서 link를 따라 순회' : isLinkedList ? isDoublyList ? 'llink와 rlink로 앞뒤 이동' : isCircularList ? 'head->link에서 한 바퀴 순회' : 'head에서 link를 따라 순회' : isDeque ? '양쪽 끝에서 삽입 · 삭제 · 조회' : isQueue ? '뒤에서 삽입 · 앞에서 삭제' : '삽입과 삭제는 맨 위에서'}</span>
                                </div>
                            </section>

                            {/* 연산별 상태 변화 */}
                            <section className="panel simulation-panel">
                                <div className="panel-heading">
                                    <div>
                                        <div className="section-label">{isRecursion ? '기준 코드 호출 예시' : '눈으로 따라가기'}</div>
                                        <h2>{isTree ? '부모와 자식은 이렇게 연결돼요' : isRecursion ? '호출과 복귀를 따라가 보세요' : isLinkedList || isLinkedStack || hasLinkedEnds ? '노드는 이렇게 연결돼요' : isDeque ? '덱은 이렇게 움직여요' : isQueue ? '큐는 이렇게 움직여요' : '스택은 이렇게 움직여요'}</h2>
                                    </div>
                                    <span className="step-counter">{stepIndex + 1} / {exercise.learning.steps.length}</span>
                                </div>
                                <div className="simulation" data-category={isLinkedStack || hasLinkedEnds ? 'linked-list' : exercise.category} data-circular={isCircularList || isDoublyList} data-doubly={isDoublyLinked}>
                                    {isTree ? (
                                        <div className="tree-diagram">
                                            {treeNodes.length ? (
                                                <svg viewBox={`0 0 640 ${treeHeight}`} role="img" aria-label={`이진 트리, root는 ${step.tree?.[0]}, 노드 ${treeNodes.length}개`}>
                                                    {treeNodes.map(node => (
                                                        <g className="tree-node" data-state={step.activeNode === node.index ? 'active' : step.visited?.includes(node.index) ? 'visited' : undefined} key={node.index}>
                                                            <title>{`${node.value} 노드, left는 ${step.tree?.[node.index * 2 + 1] ?? 'NULL'}, right는 ${step.tree?.[node.index * 2 + 2] ?? 'NULL'}`}</title>
                                                            {[0, 1].map(side => {
                                                                const childIndex = node.index * 2 + 1 + side
                                                                const childDepth = node.depth + 1
                                                                const childX = (childIndex - (2 ** childDepth - 1) + 0.5) * 640 / 2 ** childDepth
                                                                const childY = node.y + 80
                                                                const childValue = step.tree?.[childIndex]
                                                                return (
                                                                    <g className="tree-edge" key={side}>
                                                                        <line x1={node.x} y1={node.y + 19} x2={childX} y2={childY - 21} />
                                                                        <text x={(node.x + childX) / 2} y={node.y + 40}>{side === 0 ? 'left' : 'right'}</text>
                                                                        {childValue == null && <text className="tree-null" x={childX} y={childY + 4}>NULL</text>}
                                                                    </g>
                                                                )
                                                            })}
                                                            <circle cx={node.x} cy={node.y} r="19" />
                                                            <text className="tree-value" x={node.x} y={node.y + 5}>{node.value}</text>
                                                            {step.visited?.includes(node.index) && <text className="tree-order" x={node.x + 27} y={node.y - 14}>{step.visited.indexOf(node.index) + 1}</text>}
                                                            {node.index === 0 && <text className="tree-root" x={node.x} y={node.y - 26}>root</text>}
                                                        </g>
                                                    ))}
                                                </svg>
                                            ) : <span className="list-pointer">root → NULL</span>}
                                        </div>
                                    ) : isRecursion ? (
                                        <pre className="recursion-trace">{step.trace?.join('\n')}</pre>
                                    ) : isLinkedList || isLinkedStack || hasLinkedEnds ? (
                                        <div className="linked-list-diagram" data-doubly={isDoublyLinked} aria-label={isDoublyList ? `헤드 노드 ⇄ ${stepValues.length ? `${stepValues.join(' ⇄ ')} ⇄ ` : ''}헤드 노드, 데이터가 없는 head로 돌아오는 양방향 고리` : isDeque && hasLinkedEnds && stepValues.length ? `NULL ⇄ ${stepValues.join(' ⇄ ')} ⇄ NULL, front는 ${stepValues[0]} 노드, rear는 ${stepValues[stepValues.length - 1]} 노드` : isCircularList && stepValues.length ? `첫 노드 → ${stepValues.join(' → ')} → 첫 노드, head는 마지막 노드` : `${hasLinkedEnds ? 'front' : isLinkedStack ? 'top' : 'head'} → ${stepValues.join(' → ')}${stepValues.length ? ' → ' : ''}NULL${hasLinkedEnds ? `, rear는 ${stepValues.length ? `${stepValues[stepValues.length - 1]} 노드` : 'NULL'}` : ''}`}>
                                            <div className="linked-list-nodes">
                                                {isDoublyList ? (
                                                    <>
                                                        <div className="linked-node linked-sentinel"><span>llink</span><strong>head</strong><span>rlink</span></div>
                                                        <span className="list-pointer">⇄</span>
                                                    </>
                                                ) : <span className="list-pointer">{hasLinkedEnds ? isDeque && stepValues.length ? 'NULL ⇄' : 'front →' : isLinkedStack ? 'top →' : isCircularList && stepValues.length ? '첫 노드 →' : 'head →'}</span>}
                                                {stepValues.map((value, index) => (
                                                    <div className="linked-node-group" key={index}>
                                                        <div className="linked-node">
                                                            {isDoublyLinked && <span>llink</span>}
                                                            <strong>{value}</strong><span>{isDoublyLinked ? 'rlink' : 'link'}</span>
                                                            {isCircularList && index === stepValues.length - 1 && <small className="linked-head-label">head</small>}
                                                            {isDeque && hasLinkedEnds && index === 0 && <small className="linked-head-label">front</small>}
                                                            {hasLinkedEnds && index === stepValues.length - 1 && <small className="linked-head-label">rear</small>}
                                                        </div>
                                                        <span className="list-pointer">{isDoublyLinked ? '⇄' : '→'}</span>
                                                    </div>
                                                ))}
                                                <span className="list-pointer">{isDoublyList ? '↩ head' : isCircularList && stepValues.length ? '↩ 첫 노드' : 'NULL'}</span>
                                                {hasLinkedEnds && stepValues.length === 0 && <span className="list-pointer">· rear → NULL</span>}
                                            </div>
                                        </div>
                                    ) : isQueue || isDeque ? (
                                        <div className="queue-diagram" aria-label={`${categoryLabel} 값 ${stepValues.join(', ') || '없음'}, front ${step.front}, rear ${step.rear}`}>
                                            <div className="queue-cells">
                                                {Array.from({ length: exercise.contract.capacity ?? 0 }, (_, index) => {
                                                    let offset = index - (step.front ?? -1) - 1
                                                    if (exercise.contract.circular) {
                                                        const capacity = exercise.contract.capacity ?? 0
                                                        offset = (offset + capacity) % capacity
                                                    }
                                                    const active = offset >= 0 && offset < stepValues.length
                                                    return (
                                                        <div className="queue-cell" key={index} data-active={active}>
                                                            <span className="queue-index">[{index}]</span>
                                                            <strong>{active ? stepValues[offset] : '—'}</strong>
                                                            <span className="queue-marker">{[step.front === index ? 'front' : '', step.rear === index ? 'rear' : ''].filter(Boolean).join(' · ')}</span>
                                                        </div>
                                                    )
                                                })}
                                            </div>
                                            <div className="stack-base">
                                                data[{exercise.contract.capacity}] · {exercise.contract.circular ? '한 칸을 비우고 순환합니다' : '빈 앞 공간은 재사용하지 않습니다'}
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="stack-diagram" aria-label={`스택 값 ${stepValues.join(', ') || '없음'}, top ${step.top}`}>
                                            <div className="stack-cell empty-cell">
                                                {stepValues.length === (step.capacity ?? exercise.contract.capacity)
                                                    ? exercise.contract.growthFactor ? '다음 삽입 시 용량 확장' : '현재 공간이 가득 찼습니다'
                                                    : '다음 값이 들어올 자리'}
                                            </div>
                                            {[...stepValues].reverse().map((value, index) => (
                                                <div className={`stack-cell ${index === 0 ? 'top-cell' : ''}`} key={stepValues.length - index}>
                                                    <span className="cell-index">[{stepValues.length - index - 1}]</span>
                                                    {value}
                                                    {index === 0 && (
                                                        <span className="top-marker">← top</span>
                                                    )}
                                                </div>
                                            ))}
                                            {stepValues.length === 0 && (
                                                <div className="empty-stack">비어 있는 스택</div>
                                            )}
                                            <div className="stack-base">data[{step.capacity ?? exercise.contract.capacity}]</div>
                                        </div>
                                    )}
                                    {isTree && step.trace && <pre className="tree-output" aria-label="누적 출력">{step.trace.join('\n')}</pre>}
                                    <div className="operation-info">
                                        <span className="operation-caption">현재 연산</span>
                                        <code>{isTree && step.operation === 'root' ? 'root = NULL' : `${step.operation}(${step.argument ?? ''})`}</code>
                                        <div className="operation-values">
                                            {isQueue || isDeque ? (
                                                <>
                                                    <span>front <strong>{hasLinkedEnds ? stepValues.length ? `${stepValues[0]} 노드` : 'NULL' : step.front}</strong></span>
                                                    <span>rear <strong>{hasLinkedEnds ? stepValues.length ? `${stepValues[stepValues.length - 1]} 노드` : 'NULL' : step.rear}</strong></span>
                                                </>
                                            ) : isTree ? <span>root <strong>{step.tree?.[0] == null ? 'NULL' : `${step.tree[0]} 노드`}</strong></span>
                                                : !isRecursion && !isLinkedList && <span>top <strong>{isLinkedStack ? stepValues.length ? `${stepValues[0]} 노드` : 'NULL' : step.top}</strong></span>}
                                            <span>반환값 <strong>{step.result ?? '—'}</strong></span>
                                        </div>
                                        <p>{step.description ?? exercise.learning.operations.find(operation => operation.function === step.operation)?.description}</p>
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
                                <section className="panel execution-result practice-result" aria-live="polite" aria-busy={resultPending}>
                                    <div className="result-heading">
                                        <h2>
                                            {resultKind === 'GRADING' ? '제출 결과' : '실행 결과'}
                                            <span className="result-status" data-status={resultStatus}>{resultLabel}</span>
                                        </h2>
                                        <span className="result-time" title={resultKind === 'GRADING' ? '테스트 실행 시간 합계' : '코드 실행 시간'}>{executionSeconds == null ? '— s' : `${executionSeconds.toFixed(3)} s`}</span>
                                    </div>
                                    {job && job.code !== code && <p>수정 전 코드의 결과</p>}
                                    {resultPending && (
                                        <p>{resultKind === 'GRADING' ? '전체 테스트를 채점하고 있습니다.' : '선택한 예제를 실행하고 있습니다.'}</p>
                                    )}
                                    {executionError && <pre role="alert">{executionError}</pre>}
                                    {job?.message && <pre>{job.message}</pre>}
                                    {compilationFailed && preparation && (
                                        <pre>{preparation.stderr || preparation.stdout || preparation.message || '컴파일하지 못했습니다.'}</pre>
                                    )}
                                    {job?.kind === 'RUN' && resultTests.map(test => (
                                        <pre key={test.testId}>
                                            {[test.actual.stdout, test.actual.stderr, test.actual.message
                                                || (test.actual.status !== 'COMPLETED' ? '프로그램이 정상적으로 실행되지 않았습니다.' : '')].filter(Boolean).join('\n') || '(출력 없음)'}
                                        </pre>
                                    ))}
                                    {job?.kind === 'GRADING' && !resultPending && !compilationFailed && failedTest && (
                                        <pre>{failedTest.actual.stderr || failedTest.actual.message || '일부 테스트를 통과하지 못했습니다. 우측 채점 기준을 확인해 주세요.'}</pre>
                                    )}
                                </section>
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
                            {isTree || isRecursion || isLinkedList || isLinkedStack || hasLinkedEnds || exercise.contract.circular ? exercise.learning.notes?.map((note, index) => (
                                <div key={note.title}>
                                    {index > 0 && <div className="contract-divider" />}
                                    <h3>{note.title}</h3>
                                    <p>{note.description}</p>
                                </div>
                            )) : isQueue ? (
                                <>
                                    <h3>front는 다음 값의 바로 앞</h3>
                                    <p>초기 front와 rear는 -1입니다. front와 rear가 같으면 비어 있고, 저장된 값의 개수는 <code>rear - front</code>입니다.</p>
                                    <div className="contract-divider" />
                                    <h3>앞 공간은 재사용하지 않습니다</h3>
                                    <p>rear가 마지막 칸에 도달하면 포화 상태입니다. 포화 상태의 enqueue와 빈 상태의 dequeue는 오류 출력 후 종료합니다.</p>
                                </>
                            ) : (
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
                            )}
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
