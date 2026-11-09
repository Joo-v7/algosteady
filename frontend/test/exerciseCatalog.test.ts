import assert from 'node:assert/strict'
import { test } from 'node:test'
import { getPageNumbers } from '../src/exerciseCatalog.ts'

test('페이지 번호는 처음·중간·마지막에서 유효 범위를 유지한다', () => {
    assert.deepEqual(getPageNumbers(1, 7, 5), [1, 2, 3, 4, 5])
    assert.deepEqual(getPageNumbers(4, 7, 5), [2, 3, 4, 5, 6])
    assert.deepEqual(getPageNumbers(7, 7, 5), [3, 4, 5, 6, 7])
    assert.deepEqual(getPageNumbers(4, 7, 3), [3, 4, 5])
    assert.deepEqual(getPageNumbers(7, 7, 3), [5, 6, 7])
    assert.deepEqual(getPageNumbers(1, 1, 5), [1])
    assert.deepEqual(getPageNumbers(1, 0, 5), [])
})
