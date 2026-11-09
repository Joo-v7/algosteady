import { useEffect, useState } from 'react'
import { Link, useOutletContext, useSearchParams } from 'react-router'
import type { Member } from '../api/auth'
import { withdrawMember } from '../api/auth'
import { ApiError } from '../api/exercises'
import { getLearningHistory } from '../api/learning'
import type { LearningHistory } from '../api/learning'
import { getLearningCalendar } from '../learningCalendar'

function MyPage() {
    const member = useOutletContext<Member>()
    const [searchParams, setSearchParams] = useSearchParams()
    const year = searchParams.get('year') ?? ''
    const [history, setHistory] = useState<LearningHistory | null>(null)
    const [withdrawalBusy, setWithdrawalBusy] = useState(false)
    const [withdrawalError, setWithdrawalError] = useState('')
    const [requestState, setRequestState] = useState<{
        key: string
        error?: string
    } | null>(null)
    const requestKey = `${member.id}:${year}`
    const loading = requestState?.key !== requestKey
    const error = requestState?.key === requestKey ? requestState.error : ''

    // 본인 학습 현황 조회 및 연도 변경·화면 이탈 시 요청 중단
    useEffect(() => {
        if (withdrawalBusy) {
            return
        }
        const controller = new AbortController()
        const key = `${member.id}:${year}`
        getLearningHistory(year, controller.signal)
            .then(result => {
                if (!controller.signal.aborted) {
                    setHistory(result)
                    setRequestState({ key })
                }
            })
            .catch(error => {
                if (!controller.signal.aborted) {
                    if (error instanceof ApiError && error.status === 401) {
                        window.location.assign('/login')
                        return
                    }
                    setRequestState({
                        key,
                        error: error instanceof Error ? error.message : '학습 기록을 조회하지 못했습니다.'
                    })
                }
            })
        return () => controller.abort()
    }, [member.id, year, withdrawalBusy])

    const calendar = history ? getLearningCalendar(history.year, history.learningDate) : []
    const completedDays = new Map(history?.days.map(day => [day.learningDate, day]))
    const weeks = calendar.length / 7
    const monthLabels = Array.from({ length: weeks }, (_, week) => {
        const days = calendar.slice(week * 7, week * 7 + 7)
        const firstOfMonth = days.find(day => day?.date.endsWith('-01'))
        return firstOfMonth ? `${Number(firstOfMonth.date.slice(5, 7))}월` : ''
    })

    async function handleWithdraw() {
        if (!window.confirm('회원탈퇴 시 계정 정보, 제출 코드, 채점 결과, 학습 기록이 모두 삭제되며 복구할 수 없습니다. 탈퇴하시겠습니까?')) {
            return
        }
        setWithdrawalBusy(true)
        setWithdrawalError('')
        try {
            await withdrawMember()
            window.location.replace('/exercises')
        } catch (error) {
            if (error instanceof ApiError && error.status === 401) {
                window.location.replace('/exercises')
                return
            }
            setWithdrawalError(error instanceof Error ? error.message : '회원탈퇴를 처리하지 못했습니다.')
            setWithdrawalBusy(false)
        }
    }

    return (
        <main className="workspace mypage-workspace">
            <section className="panel account-panel">
                <h1>마이페이지</h1>
                <p>{member.name ?? '회원'}님</p>
            </section>
            {loading && <p role="status">학습 기록을 불러오는 중입니다.</p>}
            {error && <p role="alert">{error}</p>}
            {!loading && !error && history && (
                <>
                    {/* 학습 현황 */}
                    <section className="panel learning-summary">
                        <h2>학습 현황</h2>
                        <dl className="learning-stats">
                            <div><dt>오늘의 학습</dt><dd>{history.completedToday ? '완료' : '미완료'}</dd></div>
                            <div><dt>연속 완료</dt><dd>{history.currentStreak}<small>일</small></dd></div>
                            <div><dt>누적 완료</dt><dd>{history.totalCompletedDays}<small>일</small></dd></div>
                        </dl>
                        <p className="learning-note">한국 시간 오전 6시 기준 · 오늘의 학습 전체 통과 시 완료</p>
                    </section>
                    {/* 날짜별 완료 기록 */}
                    <section className="panel learning-history">
                        <h2>학습 기록</h2>
                        <div className="learning-year">
                            <button className="secondary-button" aria-label="이전 연도" disabled={history.year <= 1} onClick={() => setSearchParams({ year: String(history.year - 1) })}>←</button>
                            <strong>{history.year}년</strong>
                            <button className="secondary-button" aria-label="다음 연도" disabled={history.year >= Number(history.learningDate.slice(0, 4))} onClick={() => setSearchParams({ year: String(history.year + 1) })}>→</button>
                        </div>
                        <div className="learning-calendar-scroll">
                            <div className="learning-calendar" style={{ gridTemplateColumns: `repeat(${weeks}, 14px)` }}>
                                <div className="learning-months">
                                    {monthLabels.map((label, index) => <span key={index}>{label}</span>)}
                                </div>
                                <div className="learning-days">
                                    {calendar.map((day, index) => {
                                        if (!day) {
                                            return <span key={`padding-${index}`} className="learning-day is-padding" />
                                        }
                                        const completion = completedDays.get(day.date)
                                        const label = `${day.date} ${completion ? '완료' : '미완료'}`
                                        if (completion?.submissionId) {
                                            return <Link key={day.date} className="learning-day is-completed" to={`/submissions/${completion.submissionId}`} title={`${label} · 제출 상세 보기`} aria-label={`${label} · 제출 상세 보기`} />
                                        }
                                        let className = 'learning-day'
                                        if (day.future) {
                                            className += ' is-future'
                                        } else if (completion) {
                                            className += ' is-completed'
                                        }
                                        return <span key={day.date} className={className} title={day.future ? `${day.date} 학습 예정` : label} aria-label={day.future ? `${day.date} 학습 예정` : label} />
                                    })}
                                </div>
                            </div>
                        </div>
                        <p className="learning-note">{history.year}년 {history.days.length}일 완료 · 완료한 날짜를 누르면 제출 코드를 확인할 수 있습니다.</p>
                        <div className="learning-legend"><span className="learning-day" /> 미완료 <span className="learning-day is-completed" /> 완료</div>
                    </section>
                </>
            )}
            <div className="withdrawal-actions">
                {withdrawalError && <p role="alert">{withdrawalError}</p>}
                <button className="withdrawal-button" disabled={withdrawalBusy} onClick={() => void handleWithdraw()}>{withdrawalBusy ? '탈퇴 처리 중…' : '회원탈퇴'}</button>
            </div>
        </main>
    )
}

export default MyPage
