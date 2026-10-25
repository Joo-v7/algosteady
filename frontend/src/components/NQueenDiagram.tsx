import type { NQueenStep } from '../api/exercises'

// 행별 배치와 현재 후보의 충돌 위치 표시
export default function NQueenDiagram({ step }: { step: NQueenStep }) {
    const columns = Array.from({ length: step.size }, (_, column) => column)
    const conflicts = new Set<number>()
    if (step.candidateColumn != null && !step.safe) {
        for (let row = 0; row < step.row; row++) {
            const distance = Math.abs(step.queens[row] - step.candidateColumn)
            if (distance === 0 || distance === step.row - row) {
                conflicts.add(row)
            }
        }
    }
    let candidateLabel = '다음 행의 후보를 검사합니다.'
    if (step.candidateColumn != null) {
        candidateLabel = step.safe ? '충돌 없음 · 다음 행 탐색 가능' : '충돌 발생 · 이 분기 제외'
    } else if (step.row === step.size) {
        candidateLabel = '모든 행 배치 완료 · 해 하나 발견'
    } else if (step.result != null) {
        candidateLabel = `탐색 완료 · 해 ${step.result}개`
    }
    return (
        <div className="nqueen-diagram">
            <div className="nqueen-scroll">
                <table className="nqueen-board" aria-label={`${step.size}행 ${step.size}열 N-Queen 체스판`}>
                    <thead><tr><th scope="col">행 ＼ 열</th>{columns.map(column => <th scope="col" key={column}>{column}</th>)}</tr></thead>
                    <tbody>
                        {step.queens.map((queenColumn, row) => (
                            <tr key={row}>
                                <th scope="row">{row}</th>
                                {columns.map(column => {
                                    const queen = queenColumn === column
                                    const candidate = row === step.row && column === step.candidateColumn
                                    const conflict = queen && conflicts.has(row)
                                    let marker = ''
                                    if (queen) {
                                        marker = '♛'
                                    } else if (candidate) {
                                        marker = step.safe ? '○' : '×'
                                    }
                                    return (
                                        <td key={column} data-dark={(row + column) % 2 === 1} data-queen={queen} data-candidate={candidate} data-conflict={conflict} data-safe={candidate ? step.safe : undefined} aria-label={`${row}행 ${column}열${queen ? ', 퀸' : ''}${candidate ? ', 검사 후보' : ''}${conflict ? ', 충돌' : ''}`}>
                                            {marker}
                                        </td>
                                    )
                                })}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <p className="nqueen-status">{candidateLabel}</p>
            <p>초록색은 배치한 퀸, 보라색은 안전한 후보, 빨간색은 충돌입니다.</p>
            <p className="nqueen-columns">columns = {'{' + step.queens.join(', ') + '}'}</p>
        </div>
    )
}
