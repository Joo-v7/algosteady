import type { ActivityStep } from '../api/exercises'

// 활동 구간과 순차 검사·선택 결과 표시
export default function ActivityDiagram({ step }: { step: ActivityStep }) {
    const first = Math.min(0, ...step.activities.map(activity => activity.start))
    const last = Math.max(1, ...step.activities.map(activity => activity.end))
    const duration = last - first
    const selected = new Set(step.selected)
    return (
        <div className="activity-diagram">
            <p>초록색은 선택, 빨간색은 겹쳐서 제외한 활동입니다. 보라색 테두리는 마지막 검사 활동입니다.</p>
            <div className="activity-axis"><span>{first}</span><span>시각</span><span>{last}</span></div>
            <ol className="activity-list" aria-label="활동 검사 순서">
                {step.activities.map((activity, index) => {
                    let status = 'pending'
                    let label = '대기'
                    if (selected.has(activity.id)) {
                        status = 'selected'
                        label = '선택'
                    } else if (index < step.processed) {
                        status = 'skipped'
                        label = '겹침으로 제외'
                    }
                    const left = (activity.start - first) / duration * 100
                    const width = (activity.end - activity.start) / duration * 100
                    return (
                        <li key={activity.id} data-id={activity.id} data-status={status} data-active={index === step.processed - 1}>
                            <div className="activity-heading">
                                <strong>활동 {activity.id}</strong>
                                <code>[{activity.start}, {activity.end})</code>
                                <span>{label}</span>
                            </div>
                            <div className="activity-track"><span style={{ left: `${left}%`, width: `${width}%` }} /></div>
                        </li>
                    )
                })}
                {step.activities.length === 0 && <li>활동이 없습니다.</li>}
            </ol>
            <p className="activity-selected">선택 ID: {step.selected.join(' → ') || '없음'}</p>
        </div>
    )
}
