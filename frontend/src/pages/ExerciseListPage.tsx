import { Link } from 'react-router'
import { useEffect, useState } from 'react'
import { getExercises } from '../api/exercises'
import type { ExerciseSummary } from '../api/exercises'
import { getExercisePresentation } from '../exercisePresentation'

function ExerciseListPage() {
    const [exercises, setExercises] = useState<ExerciseSummary[]>([])
    const [error, setError] = useState('')

    useEffect(() => {
        const controller = new AbortController()

        getExercises(controller.signal)
            .then(setExercises)
            .catch(error => {
                if (!controller.signal.aborted) {
                    setError(error.message)
                }
            })

        return () => controller.abort()
    }, [])

    return (
        <main className="workspace">
            <section className="exercise-heading">
                <div>
                    <div className="eyebrow">DATA STRUCTURES &amp; ALGORITHMS</div>
                    <h1>자료구조·알고리즘 구현</h1>
                    <p>원리를 배우고, 직접 구현하면서 기본기를 쌓아보세요.</p>
                </div>
                <span className="language-badge">C17</span>
            </section>

            {/* 현재 제공하는 과제 */}
            <section className="exercise-list" aria-label="과제 목록">
                {error && <p role="alert">{error}</p>}
                {exercises.map(exercise => {
                    const presentation = getExercisePresentation(exercise)
                    return (
                        <article className="panel exercise-card" key={exercise.id}>
                            <div className="section-label">{presentation.label} · 기본 구현</div>
                            <h2>{exercise.title}</h2>
                            <p>{exercise.description}</p>
                            <div className="concept-tags">
                                <span>{exercise.languages.join(' · ').toUpperCase()}</span>
                                <span>
                                    {presentation.listTag}
                                </span>
                                <span>채점 기준 {exercise.criteria.length}개</span>
                            </div>
                            <div className="exercise-card-actions">
                                <Link className="secondary-button" to={`/exercises/${exercise.id}`}>학습</Link>
                                <Link className="primary-button" to={`/exercises/${exercise.id}?tab=training`}>실습</Link>
                            </div>
                        </article>
                    )
                })}
            </section>

            <footer className="workspace-footer">
                <span>매일이 쌓여, 단단한 기본기가 됩니다.</span>
                <span>Algosteady · C 구현 실습</span>
            </footer>
        </main>
    )
}

export default ExerciseListPage
