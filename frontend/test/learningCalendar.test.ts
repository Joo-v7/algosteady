import assert from 'node:assert/strict'
import { test } from 'node:test'
import { getLearningCalendar } from '../src/learningCalendar.ts'

test('평년·윤년의 모든 날짜를 중복 없이 월요일 기준으로 배치한다', () => {
    for (const [year, length, offset] of [[2026, 365, 3], [2028, 366, 5]]) {
        const calendar = getLearningCalendar(year, `${year}-12-31`)
        const dates = calendar.filter(day => day !== null)
        assert.equal(dates.length, length)
        assert.equal(new Set(dates.map(day => day.date)).size, length)
        assert.equal(calendar.length % 7, 0)
        assert.ok(calendar.slice(0, offset).every(day => day === null))
        assert.equal(dates[0].date, `${year}-01-01`)
        assert.equal(dates.at(-1)?.date, `${year}-12-31`)
        assert.ok(dates.every(day => !day.future))
    }
    assert.ok(getLearningCalendar(2028, '2028-12-31').some(day => day?.date === '2028-02-29'))
    assert.ok(!getLearningCalendar(2026, '2026-12-31').some(day => day?.date === '2026-02-29'))
})

test('서버의 학습 날짜까지 활성화하고 이후 날짜는 미래로 구분한다', () => {
    const calendar = getLearningCalendar(2028, '2028-01-02')
    assert.equal(calendar.find(day => day?.date === '2028-01-02')?.future, false)
    assert.equal(calendar.find(day => day?.date === '2028-01-03')?.future, true)
    assert.ok(getLearningCalendar(2027, '2028-01-02').filter(day => day !== null).every(day => !day.future))
})
