import type { ExhaustiveStep } from '../api/exercises'

// 전체 후보와 현재 선택 경로 표시
export default function ExhaustiveDiagram({ step }: { step: ExhaustiveStep }) {
    return (
        <div className="exhaustive-diagram">
            <p>전체 후보 · 보라색은 현재 확인한 원소, 초록색은 선택한 원소</p>
            <div className="exhaustive-values" aria-label="전체 후보">
                {step.values.map(value => (
                    <span className="exhaustive-value" data-selected={step.selected.includes(value)} data-active={step.activeValue === value} key={value}>{value}</span>
                ))}
                {step.values.length === 0 && <span>후보 없음</span>}
            </div>
            <p>현재 선택 경로</p>
            <ol className="exhaustive-path" aria-label="선택 경로">
                {step.selected.map((value, index) => <li key={index}><small>[{index}]</small><strong>{value}</strong></li>)}
                {step.selected.length === 0 && <li className="exhaustive-empty">빈 선택 {'{}'}</li>}
            </ol>
        </div>
    )
}
