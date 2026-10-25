import type { KnapsackStep } from '../api/exercises'

// 물건별 선택과 이전 행을 참조하는 DP 표 표시
export default function KnapsackDiagram({ step }: { step: KnapsackStep }) {
    const [activeRow, activeColumn] = step.activeCell ?? [-1, -1]
    const activeItem = step.items[activeRow - 1]
    return (
        <div className="knapsack-diagram">
            <div className="knapsack-items" aria-label="물건의 무게와 가치">
                {step.items.map((item, index) => (
                    <div data-active={index + 1 === activeRow} key={index}>
                        <strong>물건 {index + 1}</strong>
                        <span>무게 {item.weight} · 가치 {item.value}</span>
                    </div>
                ))}
            </div>
            <div className="knapsack-scroll">
                <table className="knapsack-table">
                    <caption>dp[i][w] · 앞의 i개 물건 / 용량 w</caption>
                    <thead>
                        <tr>
                            <th scope="col">i ＼ w</th>
                            {step.table[0].map((_, column) => <th scope="col" key={column}>{column}</th>)}
                        </tr>
                    </thead>
                    <tbody>
                        {step.table.map((row, rowIndex) => (
                            <tr key={rowIndex}>
                                <th scope="row">{rowIndex}</th>
                                {row.map((value, column) => {
                                    const isReference = activeItem && rowIndex === activeRow - 1
                                        && (column === activeColumn || column === activeColumn - activeItem.weight)
                                    return (
                                        <td data-active={rowIndex === activeRow && column === activeColumn} data-reference={Boolean(isReference)} key={column}>
                                            {value ?? '—'}
                                        </td>
                                    )
                                })}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <p>보라색은 현재 칸, 초록색은 이전 행의 참조 칸입니다. —는 미계산입니다.</p>
            {step.comparison && (
                <div className="knapsack-comparison">
                    <span>제외 <strong>{step.comparison.excluded}</strong></span>
                    <span>포함 <strong>{step.comparison.included ?? '무게 초과'}</strong></span>
                </div>
            )}
        </div>
    )
}
