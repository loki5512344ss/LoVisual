import { expect, test } from 'vitest'
import { ApiError, createApiClient } from '../client'
import { describeError } from '../errors'
import { json, mockFetch } from '../../../test/fetch'

test('sends json with bearer token and cookies', async () => {
  const fetch = mockFetch({ 'PUT /configs/1': () => json({ ok: true }) })
  const api = createApiClient()
  api.setAccessToken('tok')
  await api.request('/configs/1', { method: 'PUT', json: { a: 1 } })
  const [, init] = fetch.mock.calls[0]
  const headers = new Headers(init?.headers)
  expect(headers.get('authorization')).toBe('Bearer tok')
  expect(headers.get('content-type')).toBe('application/json')
  expect(init?.credentials).toBe('include')
  expect(init?.body).toBe('{"a":1}')
})

test('401 triggers one refresh and one retry', async () => {
  let calls = 0
  const fetch = mockFetch({
    'GET /me': () => (++calls === 1 ? json({ error: 'unauthorized' }, 401) : json({ id: 'x' })),
    'POST /auth/refresh': () => json({ access_token: 'new' }),
  })
  const api = createApiClient()
  await expect(api.request('/me')).resolves.toEqual({ id: 'x' })
  expect(fetch.mock.calls.filter(([u]) => String(u).endsWith('/auth/refresh'))).toHaveLength(1)
  expect(new Headers(fetch.mock.calls[2][1]?.headers).get('authorization')).toBe('Bearer new')
})

test('concurrent 401s share a single refresh', async () => {
  let refreshes = 0
  mockFetch({
    'GET /a': (init) => (new Headers(init.headers).get('authorization') ? json(1) : json({}, 401)),
    'GET /b': (init) => (new Headers(init.headers).get('authorization') ? json(2) : json({}, 401)),
    'POST /auth/refresh': () => {
      refreshes++
      return json({ access_token: 't' })
    },
  })
  const api = createApiClient()
  await Promise.all([api.request('/a'), api.request('/b')])
  expect(refreshes).toBe(1)
})

test('failed refresh surfaces the original 401 and clears the token', async () => {
  mockFetch({
    'GET /me': () => json({ error: 'unauthorized' }, 401),
    'POST /auth/refresh': () => json({ error: 'unauthorized' }, 401),
  })
  const api = createApiClient()
  api.setAccessToken('stale')
  await expect(api.request('/me')).rejects.toMatchObject({ status: 401 })
  expect(api.hasAccessToken()).toBe(false)
})

test('auth endpoints never trigger refresh', async () => {
  const fetch = mockFetch({ 'POST /auth/login': () => json({ error: 'unauthorized' }, 401) })
  await expect(createApiClient().request('/auth/login', { method: 'POST', json: {} })).rejects.toBeInstanceOf(
    ApiError,
  )
  expect(fetch).toHaveBeenCalledTimes(1)
})

test('429 carries retry-after; 204 resolves undefined', async () => {
  mockFetch({
    'POST /auth/login': () => json({ error: 'too many requests' }, 429, { 'retry-after': '42' }),
    'DELETE /device/links/1': () => new Response(null, { status: 204 }),
  })
  const api = createApiClient()
  await expect(api.request('/auth/login', { method: 'POST' })).rejects.toMatchObject({ status: 429, retryAfter: 42 })
  await expect(api.request('/device/links/1', { method: 'DELETE' })).resolves.toBeUndefined()
})

test('describeError covers 429, network failure and status defaults', () => {
  expect(describeError(new ApiError(429, 'x', 42))).toBe('Слишком много попыток. Подожди 42 с.')
  expect(describeError(new ApiError(429, 'x', null))).toBe('Слишком много попыток. Подожди немного.')
  expect(describeError(new TypeError('Failed to fetch'))).toBe(
    'Нет связи с сервером. Проверь интернет и попробуй ещё раз.',
  )
  expect(describeError(new ApiError(404, 'x'))).toBe('Не найдено.')
})
