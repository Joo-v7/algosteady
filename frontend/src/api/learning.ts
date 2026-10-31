import { requestApi } from './exercises'

export interface DailyLearning {
    learningDate: string
    exerciseId: string
    completed: boolean
}

export function getDailyLearning(signal: AbortSignal) {
    return requestApi<DailyLearning>('/api/learning/today', { signal })
}
