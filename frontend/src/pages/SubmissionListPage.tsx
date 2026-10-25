import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { getExercise } from '../api/exercises'
import type { ExerciseDetails } from '../api/exercises'
import { getSubmissions } from '../api/submissions'
import type { SubmissionPage } from '../api/submissions'

const jobLabels = {
    QUEUED: '대기',
    RUNNING: '채점 중',
    COMPLETED: '채점 완료',
    FAILED: '작업 실패'
}

function SubmissionListPage() {
    const { exerciseId } = useParams()
    const [searchParams, setSearchParams] = useSearchParams()
    const page = searchParams.get('page') ?? '0'
    const [exercise, setExercise] = useState<ExerciseDetails | null>(null)
    const [submissions, setSubmissions] = useState<SubmissionPage | null>(null)
    const [pageState, setPageState] = useState<{
        key: string
        error?: string
    } | null>(null)
    const requestKey = `${exerciseId}:${page}`
    const loading = pageState?.key !== requestKey
    const error = pageState?.key === requestKey ? pageState.error ?? '' : ''

    // 진행 중인 제출 결과 재조회 및 URL 변경 시 중단
    useEffect(() => {
        const controller = new AbortController()
        let timer: ReturnType<typeof setTimeout>
        const key = `${exerciseId}:${page}`

        async function poll() {
            try {
                const result = await getSubmissions(exerciseId ?? '', page, controller.signal)
                if (controller.signal.aborted) {
                    return
                }
                setSubmissions(result)
                setPageState({ key })
                if (result.content.some(item => item.job.status === 'QUEUED' || item.job.status === 'RUNNING')) {
                    timer = setTimeout(poll, 500)
                }
            } catch (error) {
                if (!controller.signal.aborted) {
                    setPageState({
                        key,
                        error: error instanceof Error ? error.message : '제출 기록을 조회하지 못했습니다.'
                    })
                }
            }
        }

        getExercise(exerciseId ?? '', controller.signal)
            .then(content => {
                if (controller.signal.aborted) {
                    return
                }
                setExercise(content)
                void poll()
            })
            .catch(error => {
                if (!controller.signal.aborted) {
                    setPageState({
                        key,
                        error: error.message
                    })
                }
            })

        return () => {
            controller.abort()
            clearTimeout(timer)
        }
    }, [exerciseId, page])

    // 목록 조회 상태에 따른 오류·대기·결과 화면 선택
    let historyContent: ReactNode
    if (error) {
        historyContent = <p role="alert">{error}</p>
    } else if (loading) {
        historyContent = <p role="status">제출 기록을 불러오는 중입니다.</p>
    } else if (submissions) {
        historyContent = (
            <>
                <h2>제출 {submissions.totalElements}건</h2>
                {submissions.content.length === 0 ? <p>제출 기록이 없습니다.</p> : (
                    <div className="history-table-scroll">
                        <table className="history-table">
                            <thead>
                                <tr>
                                    <th>제출 시각</th>
                                    <th>상태</th>
                                    <th>통과 기준</th>
                                    <th>상세</th>
                                </tr>
                            </thead>
                            <tbody>
                                {submissions.content.map(submission => (
                                    <tr key={submission.submissionId}>
                                        <td><time dateTime={submission.submittedAt}>{new Date(submission.submittedAt).toLocaleString('ko-KR')}</time></td>
                                        <td>{jobLabels[submission.job.status]}</td>
                                        <td>{submission.job.passedCriteria} / {submission.job.totalCriteria}</td>
                                        <td><Link to={`/submissions/${submission.submissionId}?page=${submissions.page}`}>상세 보기</Link></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
                <div className="history-pagination">
                    <button className="secondary-button" disabled={submissions.page === 0} onClick={() => setSearchParams({ page: String(submissions.page - 1) })}>이전 페이지</button>
                    <span>{submissions.page + 1} / {Math.max(submissions.totalPages, 1)}</span>
                    <button className="secondary-button" disabled={submissions.page + 1 >= submissions.totalPages} onClick={() => setSearchParams({ page: String(submissions.page + 1) })}>다음 페이지</button>
                </div>
            </>
        )
    }

    return (
        <main className="workspace">
            <div className="breadcrumb">
                <Link to="/exercises">과제 목록</Link> <span>/</span>
                <Link to={`/exercises/${exerciseId}`}>{exercise?.title ?? exerciseId}</Link> <span>/</span> 제출 기록
            </div>
            <section className="exercise-heading">
                <div>
                    <div className="eyebrow">SUBMISSION HISTORY</div>
                    <h1>{exercise?.title ?? '과제'} · 제출 기록</h1>
                    <p>C · 제출 코드와 채점 결과를 확인합니다.</p>
                </div>
                <Link className="primary-button" to={`/exercises/${exerciseId}?tab=training`}>실습하기로</Link>
            </section>
            <section className="panel history-panel" aria-busy={loading}>
                {historyContent}
            </section>
        </main>
    )
}

export default SubmissionListPage
