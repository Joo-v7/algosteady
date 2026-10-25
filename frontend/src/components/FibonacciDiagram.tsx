import type { FibonacciStep } from '../api/exercises'

// 두 계산 방식의 저장 배열·현재 항·호출 스택 표시
export default function FibonacciDiagram({ step }: { step: FibonacciStep }) {
    const methodLabel = step.method === 'memoization' ? '메모이제이션 · 하향식' : '타뷸레이션 · 상향식'
    return (
        <div className="fibonacci-diagram" data-method={step.method}>
            <h3>{methodLabel}</h3>
            <div className="fibonacci-values" aria-label="피보나치 저장 배열">
                {step.values.map((value, index) => (
                    <div className="fibonacci-cell" data-known={value != null} data-active={step.activeIndex === index} data-reused={step.reused && step.activeIndex === index} key={index}>
                        <small>F({index})</small>
                        <strong>{value ?? '미계산'}</strong>
                    </div>
                ))}
            </div>
            <p>보라색은 현재 항, 초록색은 저장값 재사용입니다. 0도 계산된 값입니다.</p>
            {step.pending.length > 0 && <p className="fibonacci-stack">호출 스택: {step.pending.map(index => `F(${index})`).join(' → ')}</p>}
            {step.method === 'tabulation' && <p>재귀 호출 없이 작은 인덱스부터 채웁니다.</p>}
        </div>
    )
}
