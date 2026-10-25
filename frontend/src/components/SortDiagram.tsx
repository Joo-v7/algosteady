import type { SortStep } from '../api/exercises'

// 정렬 단계의 배열·비교 위치·정렬된 구간 표시
export default function SortDiagram({ step }: {
    step: SortStep
}) {
    return (
        <div className="sort-diagram">
            <div className="sort-array" aria-label={`정렬 중인 배열: ${step.values.join(', ') || '원소 없음'}`}>
                {step.values.map((value, index) => (
                    <div className="sort-slot" data-active={step.active.includes(index)} data-sorted={step.sorted.includes(index)} key={index}>
                        <small>[{index}]</small>
                        <strong>{value}</strong>
                    </div>
                ))}
            </div>
            <p className="sort-legend">강조 테두리: 현재 이동·비교 위치 · 초록색: 현재 정렬된 구간</p>
        </div>
    )
}
