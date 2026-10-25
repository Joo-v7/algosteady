import type { ReactNode } from 'react'
import type { GraphStep } from '../api/exercises'

// 그래프 연결·저장 방식·순회 상태 표시
export default function GraphDiagram({ step, representation, directed = false }: {
    step: GraphStep
    representation: 'matrix' | 'list' | 'edges'
    directed?: boolean
}) {
    const nodes = step.adjacency.map((neighbors, vertex) => {
        const angle = vertex * 2 * Math.PI / step.adjacency.length - Math.PI / 2
        const x = step.adjacency.length === 1 ? 240 : 240 + 150 * Math.cos(angle)
        const y = step.adjacency.length === 1 ? 150 : 150 + 100 * Math.sin(angle)
        return { vertex, neighbors, x, y }
    })
    const edges = nodes.flatMap(node => node.neighbors
        .filter(neighbor => directed || node.vertex < neighbor)
        .map(neighbor => ({ from: node, to: nodes[neighbor], weight: step.weights?.[node.vertex][neighbor] })))

    // 무방향 간선의 선택·검사 상태 연결
    const selected = new Set((step.selectedEdges ?? []).map(([u, v]) => directed ? `${u}-${v}` : `${Math.min(u, v)}-${Math.max(u, v)}`))
    let active: string | undefined
    if (step.activeEdge) {
        const [u, v] = step.activeEdge
        active = directed ? `${u}-${v}` : `${Math.min(u, v)}-${Math.max(u, v)}`
    }
    let visitLabel = '방문 순서'
    if (step.inDegrees) {
        visitLabel = '위상 정렬 순서'
    } else if (step.distances) {
        visitLabel = '확정 순서'
    }
    let storage: ReactNode
    switch (representation) {
        case 'matrix':
            storage = (
                <div className="graph-matrix-scroll">
                    <table className="graph-matrix" aria-label="인접 행렬">
                        {step.distanceMatrix && <caption>원본 가중치</caption>}
                        <thead>
                            <tr>
                                <th scope="col">정점</th>
                                {nodes.map(node => <th scope="col" key={node.vertex}>{node.vertex}</th>)}
                            </tr>
                        </thead>
                        <tbody>
                            {nodes.map(node => (
                                <tr key={node.vertex} data-active={step.activeNode === node.vertex}>
                                    <th scope="row">{node.vertex}</th>
                                    {nodes.map(other => (
                                        <td data-edge={node.neighbors.includes(other.vertex)} key={other.vertex}>
                                            {step.weights ? (step.weights[node.vertex][other.vertex] ?? '∞') : Number(node.neighbors.includes(other.vertex))}
                                        </td>
                                    ))}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )
            break
        case 'list':
            storage = (
                <ol className="graph-adjacency" aria-label="인접 리스트">
                    {nodes.map(node => (
                        <li key={node.vertex} data-active={step.activeNode === node.vertex}>
                            <strong>{node.vertex}</strong>
                            <span>{`→ ${node.neighbors.map(neighbor => `${neighbor} → `).join('')}NULL`}</span>
                        </li>
                    ))}
                </ol>
            )
            break
        case 'edges': {
            const displayEdges = directed ? edges : [...edges].sort((a, b) => (a.weight ?? 0) - (b.weight ?? 0))
            storage = (
                <div className="graph-matrix-scroll">
                    <table className="graph-matrix" aria-label={directed ? '방향 간선 목록' : '간선 목록, 가중치 오름차순'}>
                        <thead><tr><th>간선</th><th>가중치</th>{!step.distances && <th>선택</th>}</tr></thead>
                        <tbody>
                            {displayEdges.map(edge => {
                                const key = `${edge.from.vertex}-${edge.to.vertex}`
                                return (
                                    <tr key={key} data-selected={selected.has(key)} data-active={active === key}>
                                        <th scope="row">{edge.from.vertex} {directed ? '→' : '—'} {edge.to.vertex}</th>
                                        <td>{edge.weight}</td>
                                        {!step.distances && <td>{selected.has(key) ? '선택' : '—'}</td>}
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>
            )
            break
        }
    }

    return (
        <div className="graph-diagram">
            <div className="tree-diagram graph-picture">
                {nodes.length > 0 ? (
                    <svg viewBox="0 0 480 300" role="img" aria-label={`${directed ? '방향' : '무방향'} 그래프: 정점 ${nodes.length}개, 간선 ${edges.length}개`}>
                        {directed && (
                            <defs>
                                <marker id="graph-direction" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="userSpaceOnUse">
                                    <path d="M 0 0 L 8 4 L 0 8 Z" fill="context-stroke" />
                                </marker>
                            </defs>
                        )}
                        {edges.map(edge => {
                            const key = `${edge.from.vertex}-${edge.to.vertex}`
                            if (!directed && !step.weights) {
                                return <line x1={edge.from.x} y1={edge.from.y} x2={edge.to.x} y2={edge.to.y} key={key} />
                            }
                            // 반대 방향 간선은 곡선으로 분리하고 화살표를 정점 바깥에 배치
                            if (directed) {
                                const dx = edge.to.x - edge.from.x
                                const dy = edge.to.y - edge.from.y
                                const length = Math.hypot(dx, dy)
                                const bend = edge.to.neighbors.includes(edge.from.vertex) ? 38 : 0
                                const controlX = (edge.from.x + edge.to.x) / 2 - dy / length * bend
                                const controlY = (edge.from.y + edge.to.y) / 2 + dx / length * bend
                                const endLength = Math.hypot(edge.to.x - controlX, edge.to.y - controlY)
                                const endX = edge.to.x - (edge.to.x - controlX) / endLength * 25
                                const endY = edge.to.y - (edge.to.y - controlY) / endLength * 25
                                const path = `M ${edge.from.x} ${edge.from.y} Q ${controlX} ${controlY} ${endX} ${endY}`
                                const labelX = edge.from.x * 0.36 + controlX * 0.48 + endX * 0.16
                                const labelY = edge.from.y * 0.36 + controlY * 0.48 + endY * 0.16 - 7
                                return (
                                    <g className="graph-edge" data-active={active === key} key={key}>
                                        <path d={path} markerEnd="url(#graph-direction)" />
                                        {edge.weight != null && <text x={labelX} y={labelY}>{edge.weight}</text>}
                                    </g>
                                )
                            }
                            return (
                                <g className="graph-edge" data-selected={selected.has(key)} data-active={active === key} key={key}>
                                    <line x1={edge.from.x} y1={edge.from.y} x2={edge.to.x} y2={edge.to.y} />
                                    <text x={edge.from.x * 0.6 + edge.to.x * 0.4} y={edge.from.y * 0.6 + edge.to.y * 0.4 - 7}>{edge.weight}</text>
                                </g>
                            )
                        })}
                        {nodes.map(node => {
                            let state: string | undefined
                            if (step.activeNode === node.vertex) {
                                state = 'active'
                            } else if (step.visited.includes(node.vertex)) {
                                state = 'visited'
                            }
                            const visitOrder = step.visited.indexOf(node.vertex)
                            return (
                                <g className="tree-node graph-node" data-state={state} key={node.vertex}>
                                    <title>{`${node.vertex}번 정점, 이웃 ${node.neighbors.join(', ') || '없음'}`}</title>
                                    <circle cx={node.x} cy={node.y} r="22" />
                                    <text className="tree-value" x={node.x} y={node.y + 5}>{node.vertex}</text>
                                    {visitOrder >= 0 && <text className="tree-order" x={node.x + 30} y={node.y - 18}>{visitOrder + 1}</text>}
                                </g>
                            )
                        })}
                    </svg>
                ) : <p>정점이 없습니다.</p>}
            </div>

            {storage}
            {step.inDegrees && (
                <div className="graph-matrix-scroll">
                    <table className="graph-matrix" aria-label="남은 진입 차수">
                        <caption>남은 진입 차수</caption>
                        <thead><tr><th scope="col">정점</th>{nodes.map(node => <th scope="col" key={node.vertex}>{node.vertex}</th>)}</tr></thead>
                        <tbody><tr><th scope="row">진입 차수</th>{step.inDegrees.map((degree, vertex) => <td key={vertex}>{degree}</td>)}</tr></tbody>
                    </table>
                </div>
            )}
            {step.distances && (
                <div className="graph-matrix-scroll">
                    <table className="graph-matrix" aria-label="시작점에서의 거리 배열">
                        <thead><tr><th scope="col">정점</th>{nodes.map(node => <th scope="col" key={node.vertex}>{node.vertex}</th>)}</tr></thead>
                        <tbody><tr><th scope="row">거리</th>{step.distances.map((distance, vertex) => <td key={vertex}>{distance ?? 'INF'}</td>)}</tr></tbody>
                    </table>
                </div>
            )}
            {step.distanceMatrix && (
                <div className="graph-matrix-scroll">
                    <table className="graph-matrix" aria-label="모든 정점 쌍의 거리 행렬">
                        <caption>최단 거리 행렬</caption>
                        <thead>
                            <tr>
                                <th scope="col">출발 ＼ 도착</th>
                                {nodes.map(node => <th scope="col" key={node.vertex}>{node.vertex}</th>)}
                            </tr>
                        </thead>
                        <tbody>
                            {step.distanceMatrix.map((row, from) => (
                                <tr key={from} data-active={step.intermediate === from}>
                                    <th scope="row">{from}</th>
                                    {row.map((distance, to) => <td key={to}>{distance ?? 'INF'}</td>)}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            {step.weights && !step.distances && !step.distanceMatrix && <p className="set-legend">초록색은 선택 간선, 주황색은 현재 검사 간선입니다. ∞는 연결 없음입니다.</p>}

            {step.distances && <p className="set-legend">화살표는 간선 방향, 주황색은 현재 검사 간선입니다. INF·∞는 도달 불가·간선 없음을 뜻합니다.</p>}
            {step.distanceMatrix && <p className="set-legend">상단은 원본 가중치, 하단은 현재 거리 행렬입니다. 보라색 정점은 현재 경유 정점이며 INF는 도달 불가입니다.</p>}

            {step.frontier && (
                <p className="graph-frontier">{step.frontierLabel}: <strong>{step.frontier.join(' → ') || '비어 있음'}</strong></p>
            )}
            {step.visited.length > 0 && <p className="graph-visited">{visitLabel}: {step.visited.join(' → ')}</p>}
        </div>
    )
}
