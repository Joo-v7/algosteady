import type { ReactNode } from 'react'
import type { ExerciseDetails, SequenceStep, TreeStep, SearchStep, SetStep, HashStep } from '../api/exercises'
import { getExercisePresentation } from '../exercisePresentation'

// 동일한 상태 표현을 사용하는 과제들이 시각화를 공유한다.
function SearchDiagram({ step }: { step: SearchStep }) {
    const stepValues = step.values
    return (
        <div className="search-array" aria-label={`정렬된 배열, 찾는 키 ${step.target}, 범위 ${step.low}~${step.high}`}>
            {stepValues.map((value, index) => (
                <div className="search-slot" data-in-range={index >= step.low && index <= step.high} data-middle={step.activeNode === index} data-found={step.result === index} key={index}>
                    <small>[{index}]</small>
                    <strong>{value}</strong>
                    <span>{[step.low === index ? 'low' : '', step.activeNode === index ? 'mid' : '', step.high === index ? 'high' : ''].filter(Boolean).join(' · ') || ' '}</span>
                </div>
            ))}
        </div>
    )
}

function SetDiagram({ step }: { step: SetStep }) {
    const setParents = step.parents
    const setRoots = setParents.flatMap((parent, index) => parent === -1 ? [index] : [])
    const setNodes = setParents.map((parent, index) => {
        let root = index
        let depth = 0
        while (setParents[root] !== -1) {
            root = setParents[root]
            depth++
        }
        return { index, parent, root, depth }
    })
    const setLayout = setNodes.map(node => {
        const peers = setNodes.filter(other => other.root === node.root && other.depth === node.depth)
        const group = setRoots.indexOf(node.root)
        return { ...node, x: (group + (peers.indexOf(node) + 0.5) / peers.length) * 640 / setRoots.length, y: 40 + node.depth * 80 }
    })
    const setHeight = setNodes.length ? (Math.max(...setNodes.map(node => node.depth)) + 1) * 80 + 30 : 0

    return (
        <>
            <div className="tree-diagram set-diagram">
                {setNodes.length ? (
                    <svg viewBox={`0 0 640 ${setHeight}`} role="img" aria-label={`서로소 집합 ${setRoots.length}개, 화살표는 원소에서 부모 방향`}>
                        <defs>
                            <marker id="set-parent-arrow" markerWidth="6" markerHeight="6" refX="6" refY="3" orient="auto">
                                <path d="M 0 0 L 6 3 L 0 6 Z" />
                            </marker>
                        </defs>
                        {setLayout.filter(node => node.parent !== -1).map(node => {
                            const parent = setLayout[node.parent]
                            return <line key={node.index} x1={node.x} y1={node.y - 19} x2={parent.x} y2={parent.y + 19} markerEnd="url(#set-parent-arrow)" />
                        })}
                        {setLayout.map(node => (
                            <g className="tree-node set-node" data-state={step.activeNode === node.index ? 'active' : step.visited?.includes(node.index) ? 'visited' : undefined} key={node.index}>
                                <title>{`원소 ${node.index}, parent는 ${node.parent}, 대표는 ${node.root}`}</title>
                                <circle cx={node.x} cy={node.y} r="19" />
                                <text className="tree-value" x={node.x} y={node.y + 5}>{node.index}</text>
                                {node.parent === -1 && <text x={node.x} y={node.y - 26}>대표</text>}
                            </g>
                        ))}
                    </svg>
                ) : <span className="list-pointer">원소가 없습니다.</span>}
            </div>
            <div className="parent-array" aria-label="부모 배열, -1은 대표 원소">
                {setParents.map((parent, index) => (
                    <span className="parent-slot" data-active={step.activeNode === index} key={index}>
                        <small>[{index}]</small>
                        <strong>{parent}</strong>
                    </span>
                ))}
            </div>
            <p className="set-legend">화살표는 원소 → 부모 방향입니다. -1은 대표, 초록색은 찾은 경로입니다.</p>
        </>
    )
}

function HashDiagram({ step }: { step: HashStep }) {
    return (
        <div className="hash-table" aria-label="버킷별 연결 리스트">
            {step.buckets.map((keys, bucket) => (
                <div className="hash-bucket" data-active={step.activeBucket === bucket} key={bucket}>
                    <strong className="hash-bucket-index">[{bucket}]</strong>
                    <div className="hash-chain" aria-label={`${bucket}번 버킷: ${keys.length ? keys.join(' → ') + ' → ' : ''}NULL`}>
                        <span className="list-pointer">→</span>
                        {keys.map(key => (
                            <div className="linked-node-group" key={key}>
                                <div className="linked-node">
                                    <strong>{key}</strong><span>link</span>
                                </div>
                                <span className="list-pointer">→</span>
                            </div>
                        ))}
                        <span className="list-pointer">NULL</span>
                    </div>
                </div>
            ))}
        </div>
    )
}

function TreeDiagram({ step, isHeap }: {
    step: TreeStep
    isHeap: boolean
}) {
    const treeNodes = step.tree.flatMap((value, index) => {
        if (value === null) {
            return []
        }
        const depth = Math.floor(Math.log2(index + 1))
        return [{ value, index, depth, x: (index - (2 ** depth - 1) + 0.5) * 640 / 2 ** depth, y: 40 + depth * 80 }]
    })
    const treeHeight = treeNodes.length ? (Math.max(...treeNodes.map(node => node.depth)) + 1) * 80 + 70 : 0

    return (
        <div className="tree-diagram">
            {treeNodes.length ? (
                <svg viewBox={`0 0 640 ${treeHeight}`} role="img" aria-label={isHeap ? `힙, 루트는 ${step.tree?.[0]}, 노드 ${treeNodes.length}개` : `이진 트리, root는 ${step.tree?.[0]}, 노드 ${treeNodes.length}개`}>
                    {treeNodes.map(node => (
                        <g className="tree-node" data-state={step.activeNode === node.index ? 'active' : step.visited?.includes(node.index) ? 'visited' : undefined} key={node.index}>
                            <title>{isHeap ? `heap[${node.index + 1}] = ${node.value}` : `${node.value} 노드, left는 ${step.tree?.[node.index * 2 + 1] ?? 'NULL'}, right는 ${step.tree?.[node.index * 2 + 2] ?? 'NULL'}`}</title>
                            {[0, 1].map(side => {
                                const childIndex = node.index * 2 + 1 + side
                                const childDepth = node.depth + 1
                                const childX = (childIndex - (2 ** childDepth - 1) + 0.5) * 640 / 2 ** childDepth
                                const childY = node.y + 80
                                const childValue = step.tree?.[childIndex]
                                if (isHeap && childValue == null) {
                                    return null
                                }
                                return (
                                    <g className="tree-edge" key={side}>
                                        <line x1={node.x} y1={node.y + 19} x2={childX} y2={childY - 21} />
                                        <text x={(node.x + childX) / 2} y={node.y + 40}>{isHeap ? childIndex + 1 : side === 0 ? 'left' : 'right'}</text>
                                        {childValue == null && <text className="tree-null" x={childX} y={childY + 4}>NULL</text>}
                                    </g>
                                )
                            })}
                            <circle cx={node.x} cy={node.y} r="19" />
                            <text className="tree-value" x={node.x} y={node.y + 5}>{node.value}</text>
                            {step.visited?.includes(node.index) && <text className="tree-order" x={node.x + 27} y={node.y - 14}>{step.visited.indexOf(node.index) + 1}</text>}
                            {isHeap ? <text className="heap-index" x={node.x} y={node.y - 26}>[{node.index + 1}]</text> : node.index === 0 && <text className="tree-root" x={node.x} y={node.y - 26}>root</text>}
                        </g>
                    ))}
                </svg>
            ) : <span className="list-pointer">{isHeap ? '빈 힙 · heap_size = 0' : 'root → NULL'}</span>}
        </div>
    )
}

function SequenceDiagram({ exercise, step }: {
    exercise: ExerciseDetails
    step: SequenceStep
}) {
    const isQueue = exercise.category === 'queue'
    const isDeque = exercise.category === 'deque'
    const isLinkedList = exercise.category === 'linked-list'
    const isLinkedStack = exercise.category === 'stack' && exercise.contract.storage === 'linked'
    const hasLinkedEnds = (isQueue || isDeque) && exercise.contract.storage === 'linked'
    const isDoublyList = isLinkedList && Boolean(exercise.contract.doubly)
    const isDoublyLinked = isDoublyList || (isDeque && hasLinkedEnds)
    const isCircularList = isLinkedList && Boolean(exercise.contract.circular) && !isDoublyList
    const categoryLabel = getExercisePresentation(exercise).label
    const stepValues = step.values
    return isLinkedList || isLinkedStack || hasLinkedEnds ? (
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
    )
}

export default function LearningVisualization({ exercise, stepIndex }: {
    exercise: ExerciseDetails
    stepIndex: number
}) {
    let diagram: ReactNode
    let details: ReactNode
    let trace: string[] | undefined
    let traceLabel = '누적 출력'
    const step = exercise.learning.steps[stepIndex]

    switch (exercise.category) {
        case 'search': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <SearchDiagram step={step} />
            details = (
                <>
                    <span>key <strong>{step.target}</strong></span>
                    <span>low <strong>{step.low}</strong></span>
                    <span>middle <strong>{step.activeNode ?? '—'}</strong></span>
                    <span>high <strong>{step.high}</strong></span>
                </>
            )
            trace = step.trace
            traceLabel = '탐색 과정'
            break
        }
        case 'set': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <SetDiagram step={step} />
            details = (
                <>
                    <span>원소 수 <strong>{step.parents.length}</strong></span>
                    <span>집합 수 <strong>{step.parents.filter(parent => parent === -1).length}</strong></span>
                </>
            )
            trace = step.trace
            traceLabel = '집합 연산 과정'
            break
        }
        case 'hash': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <HashDiagram step={step} />
            details = (
                <>
                    <span>버킷 <strong>{step.activeBucket ?? '—'}</strong></span>
                    <span>노드 수 <strong>{step.buckets.reduce((count, keys) => count + keys.length, 0)}</strong></span>
                </>
            )
            trace = step.trace
            traceLabel = '해싱 연산 과정'
            break
        }
        case 'heap': {
            const step = exercise.learning.steps[stepIndex]
            diagram = (
                <>
                    <TreeDiagram step={step} isHeap={true} />
                    <div className="heap-array" aria-label="힙 배열, 0번 칸 미사용">
                        <span className="heap-slot" data-unused="true"><small>[0]</small><strong>미사용</strong></span>
                        {step.values.map((value, index) => (
                            <span className="heap-slot" data-active={step.activeNode === index} key={index}>
                                <small>[{index + 1}]</small>
                                <strong>{value}</strong>
                            </span>
                        ))}
                    </div>
                </>
            )
            details = <span>heap_size <strong>{step.values.length}</strong></span>
            trace = step.trace
            traceLabel = '힙 연산 과정'
            break
        }
        case 'tree': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <TreeDiagram step={step} isHeap={false} />
            details = <span>root <strong>{step.tree[0] == null ? 'NULL' : `${step.tree[0]} 노드`}</strong></span>
            trace = step.trace
            break
        }
        case 'recursion': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <pre className="recursion-trace">{step.trace.join('\n')}</pre>
            break
        }
        default: {
            const step = exercise.learning.steps[stepIndex]
            const linked = exercise.contract.storage === 'linked'
            diagram = <SequenceDiagram exercise={exercise} step={step} />
            if (exercise.category === 'queue' || exercise.category === 'deque') {
                details = (
                    <>
                        <span>front <strong>{linked ? step.values.length ? `${step.values[0]} 노드` : 'NULL' : step.front}</strong></span>
                        <span>rear <strong>{linked ? step.values.length ? `${step.values[step.values.length - 1]} 노드` : 'NULL' : step.rear}</strong></span>
                    </>
                )
            } else if (exercise.category === 'stack') {
                details = <span>top <strong>{linked ? step.values.length ? `${step.values[0]} 노드` : 'NULL' : step.top}</strong></span>
            }
        }
    }

    const linked = exercise.category === 'linked-list' || (['queue', 'deque', 'stack'].includes(exercise.category) && exercise.contract.storage === 'linked')
    return (
        <div className="simulation" data-category={linked ? 'linked-list' : exercise.category} data-circular={exercise.category === 'linked-list' && Boolean(exercise.contract.circular)} data-doubly={linked && Boolean(exercise.contract.doubly)}>
            {diagram}
            {trace && <pre className="tree-output" aria-label={traceLabel}>{trace.join('\n')}</pre>}
            <div className="operation-info">
                <span className="operation-caption">현재 연산</span>
                <code>{exercise.category === 'tree' && step.operation === 'root' ? 'root = NULL' : `${step.operation}(${step.argument ?? ''})`}</code>
                <div className="operation-values">
                    {details}
                    <span>반환값 <strong>{step.result ?? '—'}</strong></span>
                </div>
                <p>{step.description ?? exercise.learning.operations.find(operation => operation.function === step.operation)?.description}</p>
            </div>
        </div>
    )
}
