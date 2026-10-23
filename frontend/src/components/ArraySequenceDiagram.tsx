import type { ExerciseDetails, SequenceStep } from '../api/exercises'
import { getExercisePresentation } from '../exercisePresentation'

// 배열 칸과 저장 위치·용량 표시
export default function ArraySequenceDiagram({ exercise, step }: {
    exercise: ExerciseDetails
    step: SequenceStep
}) {
    const isQueue = exercise.category === 'queue'
    const isDeque = exercise.category === 'deque'
    const categoryLabel = getExercisePresentation(exercise).label
    const stepValues = step.values

    // 배열 큐의 front·rear와 저장 위치 표시
    if (isQueue || isDeque) {
        return (
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
        )
    }

    // 스택의 남은 공간과 확장 여부 표시
    let spaceLabel = '다음 값이 들어올 자리'
    if (stepValues.length === (step.capacity ?? exercise.contract.capacity)) {
        spaceLabel = exercise.contract.growthFactor ? '다음 삽입 시 용량 확장' : '현재 공간이 가득 찼습니다'
    }
    return (
        <div className="stack-diagram" aria-label={`스택 값 ${stepValues.join(', ') || '없음'}, top ${step.top}`}>
            <div className="stack-cell empty-cell">
                {spaceLabel}
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
