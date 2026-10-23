import type { ExerciseDetails, SequenceStep } from '../api/exercises'

// 연결 노드와 시작·끝 포인터 표시
export default function LinkedSequenceDiagram({ exercise, step }: {
    exercise: ExerciseDetails
    step: SequenceStep
}) {
    const isLinkedList = exercise.category === 'linked-list'
    const isDeque = exercise.category === 'deque'
    const isLinkedStack = exercise.category === 'stack'
    const hasLinkedEnds = exercise.category === 'queue' || isDeque
    const isDoublyList = isLinkedList && Boolean(exercise.contract.doubly)
    const isDoublyLinked = isDoublyList || isDeque
    const isCircularList = isLinkedList && Boolean(exercise.contract.circular) && !isDoublyList
    const stepValues = step.values

    // 연결 방식에 따른 접근성 설명과 시작·끝 포인터 결정
    const hasValues = stepValues.length > 0
    const firstNode = hasValues ? `${stepValues[0]} 노드` : 'NULL'
    const lastNode = hasValues ? `${stepValues[stepValues.length - 1]} 노드` : 'NULL'
    let diagramLabel: string
    let startPointer = 'head →'
    let endPointer = 'NULL'

    if (isDoublyList) {
        const chain = hasValues ? `${stepValues.join(' ⇄ ')} ⇄ ` : ''
        diagramLabel = `헤드 노드 ⇄ ${chain}헤드 노드, 데이터가 없는 head로 돌아오는 양방향 고리`
        endPointer = '↩ head'
    } else if (isDeque && hasValues) {
        diagramLabel = `NULL ⇄ ${stepValues.join(' ⇄ ')} ⇄ NULL, front는 ${firstNode}, rear는 ${lastNode}`
        startPointer = 'NULL ⇄'
    } else if (isCircularList && hasValues) {
        diagramLabel = `첫 노드 → ${stepValues.join(' → ')} → 첫 노드, head는 마지막 노드`
        startPointer = '첫 노드 →'
        endPointer = '↩ 첫 노드'
    } else {
        let pointer = 'head'
        if (hasLinkedEnds) {
            pointer = 'front'
        } else if (isLinkedStack) {
            pointer = 'top'
        }
        const chain = hasValues ? `${stepValues.join(' → ')} → ` : ''
        diagramLabel = `${pointer} → ${chain}NULL`
        startPointer = `${pointer} →`
        if (hasLinkedEnds) {
            diagramLabel += `, rear는 ${lastNode}`
        }
    }
    return (
        <div className="linked-list-diagram" data-doubly={isDoublyLinked} aria-label={diagramLabel}>
            <div className="linked-list-nodes">
                {isDoublyList ? (
                    <>
                        <div className="linked-node linked-sentinel"><span>llink</span><strong>head</strong><span>rlink</span></div>
                        <span className="list-pointer">⇄</span>
                    </>
                ) : <span className="list-pointer">{startPointer}</span>}
                {stepValues.map((value, index) => (
                    <div className="linked-node-group" key={index}>
                        <div className="linked-node">
                            {isDoublyLinked && <span>llink</span>}
                            <strong>{value}</strong><span>{isDoublyLinked ? 'rlink' : 'link'}</span>
                            {isCircularList && index === stepValues.length - 1 && <small className="linked-head-label">head</small>}
                            {isDeque && index === 0 && <small className="linked-head-label">front</small>}
                            {hasLinkedEnds && index === stepValues.length - 1 && <small className="linked-head-label">rear</small>}
                        </div>
                        <span className="list-pointer">{isDoublyLinked ? '⇄' : '→'}</span>
                    </div>
                ))}
                <span className="list-pointer">{endPointer}</span>
                {hasLinkedEnds && stepValues.length === 0 && <span className="list-pointer">· rear → NULL</span>}
            </div>
        </div>
    )
}
