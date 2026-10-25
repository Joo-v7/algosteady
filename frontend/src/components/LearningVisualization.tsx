import type { ReactNode } from 'react'
import type { ExerciseDetails, TreeStep, SearchStep, SetStep, HashStep } from '../api/exercises'
import ArraySequenceDiagram from './ArraySequenceDiagram'
import LinkedSequenceDiagram from './LinkedSequenceDiagram'
import SortDiagram from './SortDiagram'
import GraphDiagram from './GraphDiagram'
import StringDiagram from './StringDiagram'
import TrieDiagram from './TrieDiagram'
import ExhaustiveDiagram from './ExhaustiveDiagram'
import NQueenDiagram from './NQueenDiagram'
import ActivityDiagram from './ActivityDiagram'
import FibonacciDiagram from './FibonacciDiagram'
import KnapsackDiagram from './KnapsackDiagram'
import LcsDiagram from './LcsDiagram'
import TwoPointerDiagram from './TwoPointerDiagram'
import SlidingWindowDiagram from './SlidingWindowDiagram'

// 과제별 학습 상태 시각화
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
    // 부모 배열을 따라 대표 원소와 깊이 확인
    const setRoots = setParents.flatMap((parent, index) => parent === -1 ? [index] : [])
    const setNodes = setParents.map((parent, index) => {
        let root = index
        let depth = 0
        while (setParents[root] !== -1) {
            root = setParents[root]
            depth++
        }
        return {
            index,
            parent,
            root,
            depth
        }
    })
    // 같은 집합·깊이의 노드를 나란히 배치
    const setLayout = setNodes.map(node => {
        const peers = setNodes.filter(other => other.root === node.root && other.depth === node.depth)
        const group = setRoots.indexOf(node.root)
        const x = (group + (peers.indexOf(node) + 0.5) / peers.length) * 640 / setRoots.length
        const y = 40 + node.depth * 80
        return { ...node, x, y }
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
                        {setLayout.map(node => {
                            let nodeState: string | undefined
                            if (step.activeNode === node.index) {
                                nodeState = 'active'
                            } else if (step.visited?.includes(node.index)) {
                                nodeState = 'visited'
                            }
                            return (
                                <g className="tree-node set-node" data-state={nodeState} key={node.index}>
                                    <title>{`원소 ${node.index}, parent는 ${node.parent}, 대표는 ${node.root}`}</title>
                                    <circle cx={node.x} cy={node.y} r="19" />
                                    <text className="tree-value" x={node.x} y={node.y + 5}>{node.index}</text>
                                    {node.parent === -1 && <text x={node.x} y={node.y - 26}>대표</text>}
                                </g>
                            )
                        })}
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
    // 배열 인덱스로 노드 깊이와 좌표 계산
    const treeNodes = step.tree.flatMap((value, index) => {
        if (value === null) {
            return []
        }
        const depth = Math.floor(Math.log2(index + 1))
        const x = (index - (2 ** depth - 1) + 0.5) * 640 / 2 ** depth
        const y = 40 + depth * 80
        return [{ value, index, depth, x, y }]
    })
    const treeHeight = treeNodes.length ? (Math.max(...treeNodes.map(node => node.depth)) + 1) * 80 + 70 : 0

    return (
        <div className="tree-diagram">
            {treeNodes.length ? (
                <svg viewBox={`0 0 640 ${treeHeight}`} role="img" aria-label={isHeap ? `힙, 루트는 ${step.tree?.[0]}, 노드 ${treeNodes.length}개` : `이진 트리, root는 ${step.tree?.[0]}, 노드 ${treeNodes.length}개`}>
                    {treeNodes.map(node => {
                        let nodeState: string | undefined
                        if (step.activeNode === node.index) {
                            nodeState = 'active'
                        } else if (step.visited?.includes(node.index)) {
                            nodeState = 'visited'
                        }

                        let nodeTitle: string
                        if (isHeap) {
                            nodeTitle = `heap[${node.index + 1}] = ${node.value}`
                        } else {
                            const leftValue = step.tree[node.index * 2 + 1] ?? 'NULL'
                            const rightValue = step.tree[node.index * 2 + 2] ?? 'NULL'
                            nodeTitle = `${node.value} 노드, left는 ${leftValue}, right는 ${rightValue}`
                        }
                        return (
                            <g className="tree-node" data-state={nodeState} key={node.index}>
                                <title>{nodeTitle}</title>
                                {[0, 1].map(side => {
                                    const childIndex = node.index * 2 + 1 + side
                                    const childDepth = node.depth + 1
                                    const childX = (childIndex - (2 ** childDepth - 1) + 0.5) * 640 / 2 ** childDepth
                                    const childY = node.y + 80
                                    const childValue = step.tree[childIndex]
                                    let edgeLabel: string | number
                                    if (isHeap) {
                                        edgeLabel = childIndex + 1
                                    } else {
                                        edgeLabel = side === 0 ? 'left' : 'right'
                                    }
                                    if (isHeap && childValue == null) {
                                        return null
                                    }
                                    return (
                                        <g className="tree-edge" key={side}>
                                            <line x1={node.x} y1={node.y + 19} x2={childX} y2={childY - 21} />
                                            <text x={(node.x + childX) / 2} y={node.y + 40}>{edgeLabel}</text>
                                            {childValue == null && <text className="tree-null" x={childX} y={childY + 4}>NULL</text>}
                                        </g>
                                    )
                                })}
                                <circle cx={node.x} cy={node.y} r="19" />
                                <text className="tree-value" x={node.x} y={node.y + 5}>{node.value}</text>
                                {step.visited?.includes(node.index) && <text className="tree-order" x={node.x + 27} y={node.y - 14}>{step.visited.indexOf(node.index) + 1}</text>}
                                {isHeap && <text className="heap-index" x={node.x} y={node.y - 26}>[{node.index + 1}]</text>}
                                {!isHeap && node.index === 0 && <text className="tree-root" x={node.x} y={node.y - 26}>root</text>}
                            </g>
                        )
                    })}
                </svg>
            ) : <span className="list-pointer">{isHeap ? '빈 힙 · heap_size = 0' : 'root → NULL'}</span>}
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

    // 과제 분류에 맞는 그림·현재 값·실행 과정 선택
    switch (exercise.category) {
        case 'dp': {
            const step = exercise.learning.steps[stepIndex]
            switch (step.problem) {
                case 'lcs':
                    diagram = <LcsDiagram step={step} />
                    details = (
                        <>
                            <span>a 길이 <strong>{step.a.length}</strong></span>
                            <span>b 길이 <strong>{step.b.length}</strong></span>
                        </>
                    )
                    break
                case 'knapsack':
                    diagram = <KnapsackDiagram step={step} />
                    details = (
                        <>
                            <span>물건 수 <strong>{step.items.length}</strong></span>
                            <span>배낭 용량 <strong>{step.capacity}</strong></span>
                        </>
                    )
                    break
                default:
                    diagram = <FibonacciDiagram step={step} />
                    details = (
                        <>
                            <span>목표 n <strong>{step.target}</strong></span>
                            <span>현재 항 <strong>{step.activeIndex ?? '—'}</strong></span>
                        </>
                    )
            }
            trace = step.trace
            traceLabel = '동적 계획법 계산 과정'
            break
        }
        case 'greedy': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <ActivityDiagram step={step} />
            details = (
                <>
                    <span>검사 <strong>{step.processed} / {step.activities.length}</strong></span>
                    <span>선택 <strong>{step.selected.length}</strong></span>
                    <span>마지막 종료 <strong>{step.lastEnd ?? '—'}</strong></span>
                </>
            )
            trace = step.trace
            traceLabel = '그리디 선택 과정'
            break
        }
        case 'backtracking': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <NQueenDiagram step={step} />
            details = (
                <>
                    <span>다음 행 <strong>{step.row === step.size ? '완성' : step.row}</strong></span>
                    <span>찾은 해 <strong>{step.solutions}</strong></span>
                </>
            )
            trace = step.trace
            traceLabel = '백트래킹 과정'
            break
        }
        case 'exhaustive': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <ExhaustiveDiagram step={step} />
            details = (
                <>
                    <span>탐색 깊이 <strong>{step.depth}</strong></span>
                    <span>선택 개수 <strong>{step.selected.length}</strong></span>
                </>
            )
            trace = step.trace
            traceLabel = '완전 탐색 과정'
            break
        }
        case 'string': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <StringDiagram step={step} />
            details = <span>본문 길이 <strong>{step.text.length}</strong></span>
            trace = step.trace
            traceLabel = '문자열 처리 과정'
            break
        }
        case 'trie': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <TrieDiagram step={step} />
            details = (
                <>
                    <span>노드 <strong>{step.nodes.length}</strong></span>
                    <span>단어 <strong>{step.nodes.filter(node => node.terminal).length}</strong></span>
                </>
            )
            trace = step.trace
            traceLabel = '트라이 연산 과정'
            break
        }
        case 'graph': {
            const step = exercise.learning.steps[stepIndex]
            const directed = Boolean(exercise.contract.directed)
            const edgeCount = step.adjacency.reduce((count, neighbors) => count + neighbors.length, 0) / (directed ? 1 : 2)
            const representation = exercise.contract.representation ?? (exercise.contract.storage === 'linked' ? 'list' : 'matrix')
            diagram = <GraphDiagram step={step} representation={representation} directed={directed} />
            details = (
                <>
                    <span>정점 <strong>{step.adjacency.length}</strong></span>
                    <span>간선 <strong>{edgeCount}</strong></span>
                    {step.totalWeight != null && <span>선택 비용 <strong>{step.totalWeight}</strong></span>}
                    {step.source != null && <span>시작점 <strong>{step.source}</strong></span>}
                    {step.intermediate != null && <span>경유 정점 <strong>{step.intermediate}</strong></span>}
                </>
            )
            trace = step.trace
            traceLabel = '그래프 연산 과정'
            break
        }
        case 'sort': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <SortDiagram step={step} />
            details = <span>원소 수 <strong>{step.values.length}</strong></span>
            trace = step.trace
            traceLabel = '정렬 과정'
            break
        }
        case 'sliding-window': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <SlidingWindowDiagram step={step} />
            details = (
                <>
                    <span>구간 길이 K <strong>{step.k}</strong></span>
                    <span>현재 범위 <strong>{step.start}~{step.end}</strong></span>
                    <span>현재 합 <strong>{step.sum}</strong></span>
                    <span>최대 합 <strong>{step.best ?? '—'}</strong></span>
                </>
            )
            trace = step.trace
            traceLabel = '슬라이딩 윈도 계산 과정'
            break
        }
        case 'two-pointer': {
            const step = exercise.learning.steps[stepIndex]
            diagram = <TwoPointerDiagram step={step} />
            details = (
                <>
                    <span>목표 합 <strong>{step.target}</strong></span>
                    <span>현재 합 <strong>{step.sum ?? '—'}</strong></span>
                    <span>left <strong>{step.left}</strong></span>
                    <span>right <strong>{step.right}</strong></span>
                </>
            )
            trace = step.trace
            traceLabel = '투 포인터 탐색 과정'
            break
        }
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
            let frontLabel: string | number | undefined = step.front
            let rearLabel: string | number | undefined = step.rear
            let topLabel: string | number | undefined = step.top
            if (linked) {
                const firstNode = step.values.length ? `${step.values[0]} 노드` : 'NULL'
                const lastNode = step.values.length ? `${step.values[step.values.length - 1]} 노드` : 'NULL'
                frontLabel = firstNode
                rearLabel = lastNode
                topLabel = firstNode
            }
            if (exercise.category === 'linked-list' || linked) {
                diagram = <LinkedSequenceDiagram exercise={exercise} step={step} />
            } else {
                diagram = <ArraySequenceDiagram exercise={exercise} step={step} />
            }
            if (exercise.category === 'queue' || exercise.category === 'deque') {
                details = (
                    <>
                        <span>front <strong>{frontLabel}</strong></span>
                        <span>rear <strong>{rearLabel}</strong></span>
                    </>
                )
            } else if (exercise.category === 'stack') {
                details = <span>top <strong>{topLabel}</strong></span>
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
