import type { TrieStep } from '../api/exercises'

// 접두사별 부모·자식 연결과 단어 끝 표시
export default function TrieDiagram({ step }: { step: TrieStep }) {
    const leaves = step.nodes.filter(node => !step.nodes.some(other => other.prefix.length === node.prefix.length + 1 && other.prefix.startsWith(node.prefix)))
    const layout = step.nodes.map(node => {
        const descendants = leaves.filter(leaf => leaf.prefix.startsWith(node.prefix))
        const first = leaves.indexOf(descendants[0])
        const last = leaves.indexOf(descendants[descendants.length - 1])
        const x = 24 + (first + last + 1) * 136 / leaves.length
        return { ...node, x, y: 40 + node.prefix.length * 75 }
    })
    const height = 90 + Math.max(...step.nodes.map(node => node.prefix.length)) * 75
    return (
        <div className="trie-diagram">
            <div className="tree-diagram">
                <svg viewBox={`0 0 320 ${height}`} role="img" aria-label="트라이, 이중 원은 단어 끝">
                    {layout.filter(node => node.prefix !== '').map(node => {
                        const parent = layout.find(other => other.prefix === node.prefix.slice(0, -1))!
                        return <line key={node.prefix} x1={parent.x} y1={parent.y} x2={node.x} y2={node.y} />
                    })}
                    {layout.map(node => (
                        <g className="tree-node" data-state={step.activePrefix === node.prefix ? 'active' : undefined} data-terminal={node.terminal} key={node.prefix}>
                            <title>{`${node.prefix || '루트'}, 단어 끝 ${node.terminal ? '표시' : '없음'}`}</title>
                            <circle cx={node.x} cy={node.y} r="22" />
                            {node.terminal && <circle className="trie-terminal" cx={node.x} cy={node.y} r="17" />}
                            <text className="tree-value" x={node.x} y={node.y + 5}>{node.prefix.slice(-1) || 'ε'}</text>
                        </g>
                    ))}
                </svg>
            </div>
            <p>현재 단어 <code>{JSON.stringify(step.word)}</code> · 이중 원은 단어 끝 · ε는 루트</p>
            <p className="trie-words">저장된 단어: {step.nodes.filter(node => node.terminal).map(node => JSON.stringify(node.prefix)).join(', ') || '없음'}</p>
        </div>
    )
}
