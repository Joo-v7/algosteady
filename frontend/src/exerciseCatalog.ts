import type { ExerciseSummary } from './api/exercises'

// 한국 시간 오전 6시 기준 날짜로 일일 추천 선택
export function getDailyExercise(exercises: ExerciseSummary[], now: Date): ExerciseSummary | undefined {
    if (exercises.length === 0) {
        return undefined
    }
    const learningDate = new Date(now.getTime() + (9 - 6) * 60 * 60 * 1000).toISOString().slice(0, 10)
    let hash = 2166136261
    for (const character of learningDate) {
        hash = Math.imul(hash ^ character.charCodeAt(0), 16777619)
    }
    // 목록 응답 순서와 무관하게 같은 날짜의 추천 유지
    const sorted = [...exercises].sort((left, right) => left.id.localeCompare(right.id, 'en'))
    return sorted[(hash >>> 0) % sorted.length]
}

// 현재 페이지 주변의 번호를 양 끝 범위에 맞춰 선택
export function getPageNumbers(page: number, totalPages: number, visibleCount: number): number[] {
    const count = Math.min(totalPages, visibleCount)
    const start = Math.max(1, Math.min(page - Math.floor(count / 2), totalPages - count + 1))
    return Array.from({ length: count }, (_, index) => start + index)
}
