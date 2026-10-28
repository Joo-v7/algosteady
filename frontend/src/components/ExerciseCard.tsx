import { Link } from 'react-router'
import type { ExerciseSummary } from '../api/exercises'
import { getExercisePresentation } from '../exercisePresentation'

// 오늘의 추천과 학습 목록에서 같은 콘텐츠 정보·이동 버튼 사용
export default function ExerciseCard({ exercise, featured = false }: { exercise: ExerciseSummary; featured?: boolean }) {
    const presentation = getExercisePresentation(exercise)
    const Heading = featured ? 'h2' : 'h3'
    return (
        <article className={`panel exercise-card${featured ? ' featured-exercise-card' : ''}`}>
            <div className="exercise-card-content">
                <div className="section-label">{presentation.label} · 기본 구현</div>
                <Heading>{exercise.title}</Heading>
                <p className="exercise-card-description">{exercise.description}</p>
                <div className="concept-tags">
                    <span>{exercise.languages.join(' · ').toUpperCase()}</span>
                    <span>{presentation.listTag}</span>
                    <span>채점 기준 {exercise.criteria.length}개</span>
                </div>
            </div>
            <div className="exercise-card-actions">
                <Link className="secondary-button" to={`/exercises/${exercise.id}`}>학습하기</Link>
                <Link className="primary-button" to={`/exercises/${exercise.id}?tab=training`}>실습하기</Link>
            </div>
        </article>
    )
}
