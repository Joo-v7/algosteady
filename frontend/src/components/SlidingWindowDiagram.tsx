import type { SlidingWindowStep } from '../api/exercises'

const phaseLabels = {
    initial: '첫 K개 원소의 합 계산',
    slide: '한 칸 이동 · 합 갱신',
    done: '모든 길이 K 구간 비교 완료'
}

// 고정 길이 구간과 빠지는 값·들어오는 값 표시
export default function SlidingWindowDiagram({ step }: { step: SlidingWindowStep }) {
    const sliding = step.phase === 'slide'
    return (
        <div className="window-diagram">
            <div className="window-array" aria-label={`길이 ${step.k}의 연속 구간, 현재 범위 ${step.start}~${step.end}`}>
                {step.values.map((value, index) => {
                    let positionLabel = ' '
                    if (sliding && index === step.start - 1) {
                        positionLabel = '빠짐'
                    } else if (sliding && index === step.end) {
                        positionLabel = '들어옴'
                    } else if (index === step.start && index === step.end) {
                        positionLabel = 'start · end'
                    } else if (index === step.start) {
                        positionLabel = 'start'
                    } else if (index === step.end) {
                        positionLabel = 'end'
                    }
                    return (
                        <div className="window-slot" data-window={index >= step.start && index <= step.end} data-outgoing={sliding && index === step.start - 1} data-incoming={sliding && index === step.end} key={index}>
                            <small>[{index}]</small>
                            <strong>{value}</strong>
                            <span>{positionLabel}</span>
                        </div>
                    )
                })}
            </div>
            <p className="window-phase">{phaseLabels[step.phase]}</p>
            {sliding && <p className="window-equation">{step.previousSum} − ({step.values[step.start - 1]}) + ({step.values[step.end]}) = <strong>{step.sum}</strong></p>}
            <div className="window-summary">
                <span>현재 합 <strong>{step.sum}</strong></span>
                <span>지금까지 최대 합 <strong>{step.best ?? '첫 구간 계산 중'}</strong></span>
            </div>
            <p>보라색은 현재 구간, 빨간색은 빠지는 원소, 초록색은 들어오는 원소입니다.</p>
        </div>
    )
}
