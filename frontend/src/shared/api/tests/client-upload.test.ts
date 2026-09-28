import { beforeEach, expect, test, vi } from 'vitest'
import { createApiClient } from '../client'
import { json, mockFetch } from '@test/fetch'

/** Minimal XMLHttpRequest stand-in: `send` hands the instance to the test to fire events on. */
class XhrStub {
  static instances: XhrStub[] = []
  method = ''
  url = ''
  headers: Record<string, string> = {}
  withCredentials = false
  status = 200
  response = ''
  upload: { onprogress?: (e: { lengthComputable: boolean; loaded: number; total: number }) => void } = {}
  onload?: () => void
  onerror?: () => void
  open(method: string, url: string) {
    this.method = method
    this.url = url
  }
  setRequestHeader(name: string, value: string) {
    this.headers[name.toLowerCase()] = value
  }
  send() {
    XhrStub.instances.push(this)
  }
}

beforeEach(() => {
  XhrStub.instances = []
  vi.stubGlobal('XMLHttpRequest', XhrStub)
})

test('upload sends bearer token, cookies and reports progress', async () => {
  mockFetch({})
  const api = createApiClient()
  api.setAccessToken('tok')
  const progress: number[] = []
  const promise = api.upload('/avatars', new FormData(), (pct) => progress.push(pct))
  const xhr = XhrStub.instances[0]
  xhr.upload.onprogress?.({ lengthComputable: true, loaded: 50, total: 100 })
  xhr.upload.onprogress?.({ lengthComputable: false, loaded: 0, total: 0 })
  xhr.status = 200
  xhr.response = JSON.stringify({ avatar_url: 'x' })
  xhr.onload?.()

  await expect(promise).resolves.toEqual({ avatar_url: 'x' })
  expect(progress).toEqual([50])
  expect(xhr.url).toBe('/api/avatars')
  expect(xhr.headers.authorization).toBe('Bearer tok')
  expect(xhr.withCredentials).toBe(true)
})

test('upload retries once after a successful refresh on 401', async () => {
  const fetch = mockFetch({ 'POST /auth/refresh': () => json({ access_token: 'new' }) })
  const api = createApiClient()
  const promise = api.upload('/avatars', new FormData())
  const first = XhrStub.instances[0]
  expect(first.headers.authorization).toBeUndefined()
  first.status = 401
  first.response = JSON.stringify({ error: 'unauthorized' })
  first.onload?.()

  const second = await vi.waitFor(() => {
    expect(XhrStub.instances).toHaveLength(2)
    return XhrStub.instances[1]
  })
  expect(second.headers.authorization).toBe('Bearer new')
  second.status = 200
  second.response = ''
  second.onload?.()

  await expect(promise).resolves.toBeUndefined()
  expect(fetch.mock.calls.some(([url]) => String(url).includes('/auth/refresh'))).toBe(true)
})

test('upload rejects with the server error message when refresh fails', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({ error: 'unauthorized' }, 401) })
  const api = createApiClient()
  const promise = api.upload('/avatars', new FormData())
  const xhr = XhrStub.instances[0]
  xhr.status = 413
  xhr.response = JSON.stringify({ error: 'too large' })
  xhr.onload?.()

  await expect(promise).rejects.toMatchObject({ status: 413, message: 'too large' })
})
