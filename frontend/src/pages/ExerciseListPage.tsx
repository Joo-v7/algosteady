import { useEffect, useRef, useState } from 'react'
import { useLocation, useSearchParams } from 'react-router'
import { getExercises, getExerciseGroups } from '../api/exercises'
import type { ExerciseSummary, ExerciseGroup } from '../api/exercises'
import ExerciseCard from '../components/ExerciseCard'
import { getPageNumbers } from '../exerciseCatalog'
import { getDailyLearning } from '../api/learning'
import type { DailyLearning } from '../api/learning'

const pageSize = 9

function ExerciseListPage() {
    const [exercises, setExercises] = useState<ExerciseSummary[] | null>(null)
    const [exerciseGroups, setExerciseGroups] = useState<ExerciseGroup[]>([])
    const [error, setError] = useState('')
    const [dailyLearning, setDailyLearning] = useState<DailyLearning | null>(null)
    const [searchParams, setSearchParams] = useSearchParams()
    const location = useLocation()
    const categoryId = searchParams.get('category') ?? 'all'
    const group = exerciseGroups.find(item => item.id === categoryId)
    const keyword = searchParams.get('q') ?? ''
    const [categoryInput, setCategoryInput] = useState(categoryId)
    const [searchInput, setSearchInput] = useState(keyword)
    const composing = useRef(false)
    const listHeading = useRef<HTMLHeadingElement>(null)

    useEffect(() => {
        const controller = new AbortController()

        Promise.all([getExercises(controller.signal), getExerciseGroups(controller.signal), getDailyLearning(controller.signal)])
            .then(([result, groups, daily]) => {
                if (!controller.signal.aborted) {
                    setExercises(result)
                    setExerciseGroups(groups)
                    setDailyLearning(daily)
                }
            })
            .catch(error => {
                if (!controller.signal.aborted) {
                    setError(error.message)
                }
            })

        return () => controller.abort()
    }, [])

    // 분류 링크로 진입하면 목록 로딩 후 검색 영역으로 이동
    useEffect(() => {
        if (exercises !== null && location.hash === '#catalog-heading') {
            listHeading.current?.scrollIntoView({ block: 'start' })
        }
    }, [exercises, location.hash])

    // URL에서 필터·페이지를 읽고 목록과 일일 추천을 별도로 계산
    const normalizedKeyword = keyword.trim().toLowerCase()
    const filtered = (exercises ?? []).filter(exercise => (!group || group.categories.includes(exercise.category))
        && exercise.title.toLowerCase().includes(normalizedKeyword))
    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize))
    const requestedPage = Number(searchParams.get('page') ?? '1')
    const validPage = Number.isInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1
    const page = Math.min(validPage, totalPages)
    const visibleExercises = filtered.slice((page - 1) * pageSize, page * pageSize)
    const dailyExercise = exercises?.find(exercise => exercise.id === dailyLearning?.exerciseId)
    const desktopPages = getPageNumbers(page, totalPages, 5)
    const mobilePages = getPageNumbers(page, totalPages, 3)

    // 조회로 확정된 조건 또는 뒤로가기의 URL 값 복원
    useEffect(() => {
        setCategoryInput(categoryId)
        setSearchInput(keyword)
    }, [categoryId, keyword])

    // 직접 입력한 페이지가 범위를 벗어나면 URL도 현재 페이지로 보정
    useEffect(() => {
        if (!exercises || error || !searchParams.has('page') || searchParams.get('page') === String(page)) {
            return
        }
        const params = new URLSearchParams(searchParams)
        params.set('page', String(page))
        setSearchParams(params, { replace: true })
    }, [exercises, error, page, searchParams, setSearchParams])

    // 입력 중에는 목록을 유지하고 조회 시 조건·첫 페이지 반영
    function handleSearch() {
        const params = new URLSearchParams(searchParams)
        const query = searchInput.trim()
        if (categoryInput === 'all') {
            params.delete('category')
        } else {
            params.set('category', categoryInput)
        }
        if (query === '') {
            params.delete('q')
        } else {
            params.set('q', query)
        }
        params.delete('page')
        setSearchInput(query)
        setSearchParams(params)
    }

    function handlePageChange(nextPage: number) {
        if (nextPage === page) {
            return
        }
        const params = new URLSearchParams(searchParams)
        params.set('page', String(nextPage))
        setSearchParams(params)
        listHeading.current?.scrollIntoView({ block: 'start' })
    }

    return (
        <main className="workspace home-workspace">
            {/* 로그인 여부와 무관한 오늘의 학습 추천 */}
            <section className="daily-learning" aria-labelledby="daily-learning-heading">
                <h1 id="daily-learning-heading" className="home-section-title">오늘의 학습</h1>
                {error && <p className="home-message" role="alert">{error}</p>}
                {!error && exercises === null && <p className="home-message" role="status">학습 목록을 불러오는 중입니다.</p>}
                {dailyExercise && <ExerciseCard exercise={dailyExercise} featured dailyCompleted={dailyLearning?.completed} />}
                {!error && exercises?.length === 0 && <p className="home-message">등록된 학습이 없습니다.</p>}
            </section>

            {/* 분류·검색·페이지별 학습 목록 */}
            <section className="catalog-section" aria-labelledby="catalog-heading">
                <h2 id="catalog-heading" className="home-section-title" ref={listHeading}>학습 목록</h2>
                <form
                    className="catalog-filters"
                    role="search"
                    aria-label="학습 검색"
                    onSubmit={event => {
                        event.preventDefault()
                        if (!composing.current) {
                            handleSearch()
                        }
                    }}
                >
                    <label className="catalog-category">
                        <span>분류</span>
                        <span className="catalog-select">
                            <select value={categoryInput} onChange={event => setCategoryInput(event.target.value)}>
                                <option value="all">전체</option>
                                {exerciseGroups.map(item => <option value={item.id} key={item.id}>{item.label}</option>)}
                            </select>
                            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7 10 5 5 5-5" /></svg>
                        </span>
                    </label>
                    <label className="catalog-search">
                        <span className="sr-only">학습 이름 검색</span>
                        <input
                            type="search"
                            placeholder="학습 이름 검색"
                            value={searchInput}
                            onChange={event => setSearchInput(event.target.value)}
                            onCompositionStart={() => {
                                composing.current = true
                            }}
                            onCompositionEnd={event => {
                                composing.current = false
                                setSearchInput(event.currentTarget.value)
                            }}
                            onKeyDown={event => {
                                if (event.key === 'Enter' && (composing.current || event.nativeEvent.isComposing)) {
                                    event.preventDefault()
                                }
                            }}
                        />
                    </label>
                    <button className="primary-button catalog-query" type="submit">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4.5 4.5" /></svg>
                        조회
                    </button>
                </form>
                {!error && exercises !== null && filtered.length === 0 && <p className="home-message" role="status">검색 결과가 없습니다.</p>}
                <div className="exercise-list" aria-label="학습 카드">
                    {visibleExercises.map(exercise => <ExerciseCard exercise={exercise} key={exercise.id} />)}
                </div>
                {!error && totalPages > 1 && (
                    <nav className="catalog-pagination" aria-label="학습 목록 페이지">
                        <button aria-label="처음 페이지" disabled={page === 1} onClick={() => handlePageChange(1)}>«</button>
                        <button aria-label="이전 페이지" disabled={page === 1} onClick={() => handlePageChange(page - 1)}>‹</button>
                        <div className="pagination-pages pagination-pages-desktop">
                            {desktopPages.map(number => (
                                <button key={number} aria-label={`${number}페이지`} aria-current={number === page ? 'page' : undefined} onClick={() => handlePageChange(number)}>{number}</button>
                            ))}
                        </div>
                        <div className="pagination-pages pagination-pages-mobile">
                            {mobilePages.map(number => (
                                <button key={number} aria-label={`${number}페이지`} aria-current={number === page ? 'page' : undefined} onClick={() => handlePageChange(number)}>{number}</button>
                            ))}
                        </div>
                        <button aria-label="다음 페이지" disabled={page === totalPages} onClick={() => handlePageChange(page + 1)}>›</button>
                        <button aria-label="마지막 페이지" disabled={page === totalPages} onClick={() => handlePageChange(totalPages)}>»</button>
                    </nav>
                )}
            </section>

            <footer className="workspace-footer">
                <span>매일이 쌓여, 단단한 기본기가 됩니다.</span>
                <span>Algosteady · C 구현 실습</span>
            </footer>
        </main>
    )
}

export default ExerciseListPage
