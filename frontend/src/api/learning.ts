import { requestApi } from './exercises'

export interface DailyLearning {
    learningDate: string
    exerciseId: string
    completed: boolean
    attempted: boolean
}

export function getDailyLearning(signal: AbortSignal) {
    return requestApi<DailyLearning>('/api/learning/today', { signal })
}

export interface LearningHistoryDay {
    learningDate: string
    submissionId: string | null
}

export interface LearningHistory {
    learningDate: string
    completedToday: boolean
    currentStreak: number
    totalCompletedDays: number
    year: number
    days: LearningHistoryDay[]
}

export function getLearningHistory(year: string, signal: AbortSignal) {
    const query = year ? `?year=${encodeURIComponent(year)}` : ''
    return requestApi<LearningHistory>(`/api/learning/history${query}`, { signal })
}
