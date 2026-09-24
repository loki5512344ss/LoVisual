import { vi } from 'vitest'

type Handler = (init: RequestInit) => Response | Promise<Response>

export function json(body: unknown, status = 200, headers: Record<string, string> = {}): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json', ...headers },
  })
}

export function mockFetch(routes: Record<string, Handler>) {
  const fn = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const url = new URL(String(input), 'http://localhost')
    const key = `${(init.method ?? 'GET').toUpperCase()} ${url.pathname.replace(/^\/api/, '')}`
    const handler = routes[key]
    return handler ? handler(init) : json({ error: `no mock for ${key}` }, 404)
  })
  vi.stubGlobal('fetch', fn)
  return fn
}
