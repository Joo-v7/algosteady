import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import { test } from 'node:test'
import type { ExerciseSummary } from '../src/api/exercises.ts'
import { getDailyExercise, getPageNumbers } from '../src/exerciseCatalog.ts'

const directory = new URL('../../content/', import.meta.url)
const exercises: ExerciseSummary[] = readdirSync(directory, { withFileTypes: true })
    .filter(entry => entry.isDirectory())
    .map(entry => JSON.parse(readFileSync(new URL(`${entry.name}/exercise.json`, directory), 'utf8')))

test('오전 6시 전에는 전날 추천을 유지하고 정확히 6시부터 새 날짜를 적용한다', () => {
    const previousDay = getDailyExercise(exercises, new Date('2026-10-24T06:00:00+09:00'))
    assert.equal(getDailyExercise(exercises, new Date('2026-10-25T00:00:00+09:00'))?.id, previousDay?.id)
    assert.equal(getDailyExercise(exercises, new Date('2026-10-25T05:59:59.999+09:00'))?.id, previousDay?.id)
    const today = getDailyExercise(exercises, new Date('2026-10-25T06:00:00+09:00'))
    assert.equal(getDailyExercise(exercises, new Date('2026-10-26T05:59:59.999+09:00'))?.id, today?.id)
    assert.equal(getDailyExercise(exercises, new Date('2026-10-24T21:00:00Z'))?.id, today?.id)
    assert.notEqual(previousDay?.id, today?.id)
})

test('월말·연말의 오전 6시 경계를 처리한다', () => {
    for (const [before, previous] of [
        ['2026-11-01T05:59:59+09:00', '2026-10-31T06:00:00+09:00'],
        ['2027-01-01T05:59:59+09:00', '2026-12-31T06:00:00+09:00']
    ]) {
        assert.equal(getDailyExercise(exercises, new Date(before))?.id, getDailyExercise(exercises, new Date(previous))?.id)
    }
})

test('목록 순서·기기 시간대 표현이 달라도 같은 추천을 고르고 원본 순서를 보존한다', () => {
    const original = exercises.map(exercise => exercise.id)
    const now = new Date('2026-10-25T08:00:00+09:00')
    const reversed = [...exercises].reverse()
    assert.equal(getDailyExercise(exercises, now)?.id, getDailyExercise(reversed, new Date('2026-10-24T19:00:00-04:00'))?.id)
    assert.deepEqual(exercises.map(exercise => exercise.id), original)
    assert.equal(getDailyExercise([], now), undefined)
    assert.equal(getDailyExercise([exercises[0]], now), exercises[0])
})

test('페이지 번호는 처음·중간·마지막에서 유효 범위를 유지한다', () => {
    assert.deepEqual(getPageNumbers(1, 7, 5), [1, 2, 3, 4, 5])
    assert.deepEqual(getPageNumbers(4, 7, 5), [2, 3, 4, 5, 6])
    assert.deepEqual(getPageNumbers(7, 7, 5), [3, 4, 5, 6, 7])
    assert.deepEqual(getPageNumbers(4, 7, 3), [3, 4, 5])
    assert.deepEqual(getPageNumbers(7, 7, 3), [5, 6, 7])
    assert.deepEqual(getPageNumbers(1, 1, 5), [1])
    assert.deepEqual(getPageNumbers(1, 0, 5), [])
})
