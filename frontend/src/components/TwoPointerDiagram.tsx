import type { TwoPointerStep } from '../api/exercises'

const decisionLabels = {
    'move-left': '합이 작음 → left 증가',
    'move-right': '합이 큼 → right 감소',
    found: '목표 합 발견',
    absent: '탐색 종료 · 목표 합 없음'
}

// 두 포인터의 후보 범위와 현재 비교 위치 표시
export default function TwoPointerDiagram({ step }: { step: TwoPointerStep }) {
    const comparing = step.left < step.right
    return (
        <div className="two-pointer-diagram">
            <div className="two-pointer-array" aria-label={`정렬된 배열, 목표 합 ${step.target}`}>
                {step.values.map((value, index) => {
                    const atLeft = index === step.left
                    const atRight = index === step.right
                    const active = comparing && (atLeft || atRight)
                    let pointerLabel = ' '
                    if (atLeft && atRight) {
                        pointerLabel = 'left · right'
                    } else if (atLeft) {
                        pointerLabel = 'left'
                    } else if (atRight) {
                        pointerLabel = 'right'
                    }
                    return (
                        <div className="two-pointer-slot" data-in-range={comparing && index >= step.left && index <= step.right} data-active={active} data-found={active && step.decision === 'found'} key={index}>
                            <small>[{index}]</small>
                            <strong>{value}</strong>
                            <span>{pointerLabel}</span>
                        </div>
                    )
                })}
                {step.values.length === 0 && <p>빈 배열</p>}
            </div>
            <p className="two-pointer-equation">
                {comparing && <span>{step.values[step.left]} + {step.values[step.right]} = <strong>{step.sum}</strong> · 목표 {step.target}</span>}
                {!comparing && <span>서로 다른 두 인덱스의 후보가 없습니다.</span>}
            </p>
            <p className="two-pointer-decision" data-decision={step.decision}>{decisionLabels[step.decision]}</p>
            {step.decision === 'found' && <p>반환 인덱스: {'{'}{step.left}, {step.right}{'}'}</p>}
            {step.decision === 'absent' && <p>반환 인덱스: {'{-1, -1}'}</p>}
            <p>보라색은 비교 중인 두 위치, 초록색은 찾은 쌍입니다. 흐린 칸은 제외한 후보입니다.</p>
        </div>
    )
}
