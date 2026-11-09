export interface LearningCalendarDay {
    date: string
    future: boolean
}

// 브라우저 시간대와 무관한 월요일 시작 연도별 달력
export function getLearningCalendar(year: number, learningDate: string) {
    const firstDay = new Date(0)
    firstDay.setUTCFullYear(year, 0, 1)
    const leadingDays = (firstDay.getUTCDay() + 6) % 7
    const days: (LearningCalendarDay | null)[] = Array.from({ length: leadingDays }, () => null)
    const cursor = new Date(firstDay)
    while (cursor.getUTCFullYear() === year) {
        const date = cursor.toISOString().slice(0, 10)
        days.push({ date, future: date > learningDate })
        cursor.setUTCDate(cursor.getUTCDate() + 1)
    }
    while (days.length % 7 !== 0) {
        days.push(null)
    }
    return days
}
