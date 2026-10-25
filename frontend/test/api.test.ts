import assert from 'node:assert/strict'
import { test } from 'node:test'
import { ApiError, requestApi } from '../src/api/exercises.ts'

test('정상 API 응답의 JSON을 반환한다', async context => {
    context.mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({ jobId: 'job' }), { status: 202 }))
    assert.deepEqual(await requestApi('/api/runs'), { jobId: 'job' })
})

test('HTTP 오류의 상태와 서버 메시지를 함께 보존한다', async context => {
    for (const status of [404, 500]) {
        const mock = context.mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({ message: '조회 실패' }), { status }))
        await assert.rejects(requestApi('/api/jobs/job'), error => {
            assert.ok(error instanceof ApiError)
            assert.equal(error.status, status)
            assert.equal(error.message, '조회 실패')
            return true
        })
        mock.mock.restore()
    }
})

test('JSON이 아닌 오류 응답도 HTTP 상태를 보존한다', async context => {
    context.mock.method(globalThis, 'fetch', async () => new Response('unavailable', { status: 503 }))
    await assert.rejects(requestApi('/api/jobs/job'), error => error instanceof ApiError && error.status === 503)
})

test('통신 실패를 없는 작업의 HTTP 오류로 바꾸지 않는다', async context => {
    const failure = new TypeError('Failed to fetch')
    context.mock.method(globalThis, 'fetch', async () => { throw failure })
    await assert.rejects(requestApi('/api/jobs/job'), error => error === failure && !(error instanceof ApiError))
})
