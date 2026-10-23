import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import { test } from 'node:test'
import { getExercisePresentation } from '../src/exercisePresentation.ts'

const directory = new URL('../../content/', import.meta.url)
for (const entry of readdirSync(directory, { withFileTypes: true }).filter(entry => entry.isDirectory())) {
    test(`${entry.name}: 목록·상세 분류와 저장 방식 문구를 제공한다`, () => {
        const exercise = JSON.parse(readFileSync(new URL(`${entry.name}/exercise.json`, directory), 'utf8'))
        const presentation = getExercisePresentation(exercise)
        for (const value of Object.values(presentation)) {
            assert.ok(value.length > 0)
            assert.ok(!value.includes('undefined'))
        }
        const concepts = {
            heap: '힙',
            hash: '버킷',
            set: '대표',
            search: '탐색'
        }
        const keyword = concepts[exercise.category as keyof typeof concepts]
        if (keyword) {
            assert.ok(presentation.introduction.includes(keyword))
        }
        assert.equal(presentation.family, ['recursion', 'search'].includes(exercise.category) ? 'ALGORITHMS' : 'DATA STRUCTURES')
        if (exercise.category !== 'stack') {
            assert.ok(!presentation.introduction.includes('스택'))
        }
        if (exercise.contract.storage === 'linked' && ['stack', 'queue', 'deque'].includes(exercise.category)) {
            assert.ok(presentation.listTag.includes('연결 리스트'))
            assert.equal(presentation.simulationTitle, '노드는 이렇게 연결돼요')
        }
    })
}
