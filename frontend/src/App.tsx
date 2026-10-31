import { useEffect, useState } from 'react'
import { Link, Navigate, Outlet, Route, Routes, useNavigate } from 'react-router'
import { getCurrentMember, logout } from './api/auth'
import type { Member } from './api/auth'
import ExerciseListPage from './pages/ExerciseListPage'
import ExercisePage from './pages/ExercisePage'
import SubmissionListPage from './pages/SubmissionListPage'
import SubmissionDetailsPage from './pages/SubmissionDetailsPage'
import LoginPage from './pages/LoginPage'
import MyPage from './pages/MyPage'

function App() {
    const navigate = useNavigate()
    const [member, setMember] = useState<Member | null>(null)
    const [authLoading, setAuthLoading] = useState(true)
    const [authError, setAuthError] = useState('')
    const [logoutBusy, setLogoutBusy] = useState(false)

    useEffect(() => {
        const controller = new AbortController()

        getCurrentMember(controller.signal)
            .then(result => {
                if (!controller.signal.aborted) {
                    setMember(result)
                    setAuthLoading(false)
                }
            })
            .catch(error => {
                if (!controller.signal.aborted) {
                    setAuthError(error.message)
                    setAuthLoading(false)
                }
            })

        return () => controller.abort()
    }, [])

    async function handleLogout() {
        setLogoutBusy(true)
        setAuthError('')
        try {
            await logout()
            setMember(null)
            navigate('/exercises')
        } catch (error) {
            setAuthError(error instanceof Error ? error.message : '로그아웃하지 못했습니다.')
        } finally {
            setLogoutBusy(false)
        }
    }

    const loadingPage = <main className="workspace"><p role="status">로그인 상태를 확인하는 중입니다.</p></main>

    // 인증 상태를 확인한 뒤 개인 화면·제출 기록 진입 여부 결정
    let privatePages
    if (authLoading) {
        privatePages = loadingPage
    } else if (authError) {
        privatePages = <main className="workspace"><p role="alert">{authError}</p></main>
    } else if (member) {
        privatePages = <Outlet key={member.id} context={member} />
    } else {
        privatePages = <Navigate to="/login" replace />
    }

    return (
        <div className="app">
            {/* 모든 화면에서 사용하는 헤더 */}
            <header className="site-header">
                <Link className="brand" to="/exercises" aria-label="Algosteady 홈">
                    <span className="brand-mark">AS</span>
                    algosteady
                </Link>
                <span className="header-description">매일 꾸준히 구현하는 자료구조</span>
                <nav className="header-links" aria-label="주 메뉴">
                    <Link className="header-nav" to="/exercises">홈</Link>
                    {authLoading && <span className="header-nav" role="status">로그인 확인 중</span>}
                    {!authLoading && !member && <Link className="header-nav" to="/login">로그인</Link>}
                    {member && (
                        <>
                            <Link className="header-nav" to="/mypage">마이페이지</Link>
                            <button className="header-nav" disabled={logoutBusy} onClick={() => void handleLogout()}>로그아웃</button>
                        </>
                    )}
                </nav>
            </header>
            {authError && <p className="account-error" role="alert">{authError}</p>}

            {/* URL별 화면 연결 */}
            <Routes>
                <Route path="/" element={<Navigate to="/exercises" replace />} />
                <Route path="/exercises" element={<ExerciseListPage key={member?.id ?? 'anonymous'} />} />
                <Route path="/login" element={member ? <Navigate to="/exercises" replace /> : <LoginPage />} />
                <Route element={privatePages}>
                    <Route path="/mypage" element={<MyPage />} />
                    <Route path="/exercises/:exerciseId/submissions" element={<SubmissionListPage />} />
                    <Route path="/submissions/:submissionId" element={<SubmissionDetailsPage />} />
                </Route>
                <Route path="/exercises/:exerciseId" element={authLoading ? loadingPage : <ExercisePage key={member?.id ?? 'anonymous'} member={member} />} />
                <Route path="*" element={
                    <main className="workspace">
                        <section className="panel unavailable-page">
                            <h1>페이지를 찾을 수 없습니다.</h1>
                            <p>주소를 확인하거나 학습 목록으로 돌아가 주세요.</p>
                            <Link className="primary-button" to="/exercises">학습 목록으로</Link>
                        </section>
                    </main>
                } />
            </Routes>
        </div>
    )
}

export default App
