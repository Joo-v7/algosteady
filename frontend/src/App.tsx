import { Link, Navigate, Route, Routes } from 'react-router'
import ExerciseListPage from './pages/ExerciseListPage'
import ExercisePage from './pages/ExercisePage'
import SubmissionListPage from './pages/SubmissionListPage'
import SubmissionDetailsPage from './pages/SubmissionDetailsPage'

function App() {
    return (
        <div className="app">
            {/* 모든 화면에서 사용하는 헤더 */}
            <header className="site-header">
                <Link className="brand" to="/exercises" aria-label="Algosteady 홈">
                    <span className="brand-mark">AS</span>
                    algosteady
                </Link>
                <span className="header-description">매일 꾸준히 구현하는 자료구조</span>
                <Link className="header-nav" to="/exercises">과제 목록</Link>
            </header>

            {/* URL별 화면 연결 */}
            <Routes>
                <Route path="/" element={<Navigate to="/exercises" replace />} />
                <Route path="/exercises" element={<ExerciseListPage />} />
                <Route path="/exercises/:exerciseId/submissions" element={<SubmissionListPage />} />
                <Route path="/exercises/:exerciseId" element={<ExercisePage />} />
                <Route path="/submissions/:submissionId" element={<SubmissionDetailsPage />} />
                <Route path="*" element={
                    <main className="workspace">
                        <section className="panel unavailable-page">
                            <h1>페이지를 찾을 수 없습니다.</h1>
                            <p>주소를 확인하거나 과제 목록으로 돌아가 주세요.</p>
                            <Link className="primary-button" to="/exercises">과제 목록으로</Link>
                        </section>
                    </main>
                } />
            </Routes>
        </div>
    )
}

export default App
