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
            search: '탐색',
            sort: '정렬',
            graph: '그래프',
            string: '문자열',
            trie: '트라이',
            exhaustive: '완전 탐색',
            backtracking: '백트래킹',
            greedy: '그리디',
            dp: '동적 계획법',
            'two-pointer': '투 포인터',
            'sliding-window': '슬라이딩 윈도'
        }
        const keyword = concepts[exercise.category as keyof typeof concepts]
        if (keyword) {
            assert.ok(presentation.introduction.includes(keyword))
        }
        assert.equal(presentation.family, ['recursion', 'search', 'sort', 'string', 'exhaustive', 'backtracking', 'greedy', 'dp', 'two-pointer', 'sliding-window'].includes(exercise.category) ? 'ALGORITHMS' : 'DATA STRUCTURES')
        if (exercise.category !== 'stack') {
            assert.ok(!presentation.introduction.includes('스택'))
        }
        if (exercise.contract.storage === 'linked' && ['stack', 'queue', 'deque'].includes(exercise.category)) {
            assert.ok(presentation.listTag.includes('연결 리스트'))
            assert.equal(presentation.simulationTitle, '노드는 이렇게 연결돼요')
        }
        if (exercise.contract.problem === 'lcs') {
            assert.equal(presentation.tag, 'Longest Common Subsequence')
            assert.ok(presentation.note.includes('문자열'))
            assert.ok(!presentation.note.includes('메모이제이션'))
        }
        if (exercise.contract.problem === 'knapsack') {
            assert.equal(presentation.tag, '0/1 Knapsack')
            assert.ok(presentation.note.includes('0/1'))
            assert.ok(!presentation.note.includes('피보나치'))
        }
        if (exercise.category === 'graph') {
            assert.ok(presentation.note.startsWith(exercise.contract.directed ? '방향 ·' : '무방향 ·'))
            let representation = '인접 행렬'
            if (exercise.contract.representation === 'edges') {
                representation = '간선 목록'
            } else if (exercise.contract.storage === 'linked') {
                representation = '인접 리스트'
            }
            assert.ok(presentation.listTag.includes(representation))
            assert.ok(presentation.note.includes(exercise.contract.weighted ? '· 가중치 ·' : '· 비가중치 ·'))
        }
    })
}
