import type { LcsStep } from '../api/exercises'

// 두 접두사의 문자 비교와 DP 표 참조 위치 표시
export default function LcsDiagram({ step }: { step: LcsStep }) {
    const [activeRow, activeColumn] = step.activeCell ?? [-1, -1]
    const comparing = activeRow > 0 && activeColumn > 0
    const matching = comparing && step.a[activeRow - 1] === step.b[activeColumn - 1]
    let comparison = '빈 접두사의 0행·0열에서 시작합니다.'
    if (comparing) {
        comparison = matching ? '문자 일치 · 대각선 + 1' : '문자 불일치 · 위·왼쪽 중 큰 값'
    }

    return (
        <div className="lcs-diagram">
            <div className="lcs-strings">
                <div aria-label="첫 번째 문자열">
                    <strong>a</strong>
                    {[...step.a].map((character, index) => (
                        <span data-active={index + 1 === activeRow} key={index}>{character}</span>
                    ))}
                    {step.a.length === 0 && <span>빈 문자열</span>}
                </div>
                <div aria-label="두 번째 문자열">
                    <strong>b</strong>
                    {[...step.b].map((character, index) => (
                        <span data-active={index + 1 === activeColumn} key={index}>{character}</span>
                    ))}
                    {step.b.length === 0 && <span>빈 문자열</span>}
                </div>
            </div>
            <div className="lcs-scroll">
                <table className="lcs-table">
                    <caption>dp[i][j] · 행: a의 앞 i글자 / 열: b의 앞 j글자</caption>
                    <thead>
                        <tr>
                            <th scope="col">a ＼ b</th>
                            <th scope="col">0 · ∅</th>
                            {[...step.b].map((character, index) => <th scope="col" key={index}>{index + 1} · {character}</th>)}
                        </tr>
                    </thead>
                    <tbody>
                        {step.table.map((row, rowIndex) => (
                            <tr key={rowIndex}>
                                <th scope="row">{rowIndex} · {step.a[rowIndex - 1] ?? '∅'}</th>
                                {row.map((value, column) => {
                                    let reference = false
                                    if (comparing) {
                                        if (matching) {
                                            reference = rowIndex === activeRow - 1 && column === activeColumn - 1
                                        } else {
                                            const above = rowIndex === activeRow - 1 && column === activeColumn
                                            const left = rowIndex === activeRow && column === activeColumn - 1
                                            reference = above || left
                                        }
                                    }
                                    return (
                                        <td data-active={rowIndex === activeRow && column === activeColumn} data-reference={reference} key={column}>
                                            {value ?? '—'}
                                        </td>
                                    )
                                })}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <p>{comparison}</p>
            <p>보라색은 현재 문자·칸, 초록색은 참조 칸입니다. ∅는 빈 접두사, —는 미계산입니다.</p>
        </div>
    )
}
