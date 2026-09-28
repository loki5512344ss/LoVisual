export class ApiError extends Error {
  readonly status: number
  readonly retryAfter: number | null

  constructor(status: number, message: string, retryAfter: number | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.retryAfter = retryAfter
  }
}

export type RequestOptions = { method?: string; json?: unknown; body?: BodyInit; signal?: AbortSignal }

export type ApiClient = {
  request<T>(path: string, options?: RequestOptions): Promise<T>
  upload(path: string, body: FormData, onProgress?: (pct: number) => void): Promise<unknown>
  refresh(): Promise<boolean>
  setAccessToken(token: string | null): void
  hasAccessToken(): boolean
}

async function toError(res: Response): Promise<ApiError> {
  let message = `HTTP ${res.status}`
  try {
    const body: unknown = await res.json()
    if (body && typeof body === 'object' && 'error' in body && typeof body.error === 'string') message = body.error
  } catch {
    // non-JSON error body: keep the status text
  }
  const retry = Number(res.headers.get('retry-after'))
  return new ApiError(res.status, message, Number.isFinite(retry) && retry > 0 ? retry : null)
}

export function createApiClient(
  baseUrl = '/api',
  // Resolved at call time so tests can stub the global fetch.
  fetchImpl: typeof fetch = (input, init) => fetch(input, init),
): ApiClient {
  let accessToken: string | null = null
  let refreshing: Promise<boolean> | null = null

  async function refreshOnce(): Promise<boolean> {
    try {
      const res = await fetchImpl(`${baseUrl}/auth/refresh`, { method: 'POST', credentials: 'include' })
      if (!res.ok) {
        accessToken = null
        return false
      }
      const body = (await res.json()) as { access_token: string }
      // Store the renewed token: callers (session restore) rely on refresh()
      // alone, and requests after a reload would otherwise go out without a bearer.
      accessToken = body.access_token
      return true
    } catch {
      return false
    }
  }

  function refresh(): Promise<boolean> {
    refreshing ??= refreshOnce().finally(() => {
      refreshing = null
    })
    return refreshing
  }

  function send(path: string, options: RequestOptions): Promise<Response> {
    const headers = new Headers()
    let body = options.body
    if (options.json !== undefined) {
      headers.set('content-type', 'application/json')
      body = JSON.stringify(options.json)
    }
    if (accessToken && !path.startsWith('/auth/')) headers.set('authorization', `Bearer ${accessToken}`)
    return fetchImpl(`${baseUrl}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body,
      credentials: 'include',
      signal: options.signal,
    })
  }

  async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    let res = await send(path, options)
    if (res.status === 401 && !path.startsWith('/auth/') && (await refresh())) {
      res = await send(path, options)
    }
    if (!res.ok) throw await toError(res)
    const text = await res.text()
    return (text ? JSON.parse(text) : undefined) as T
  }

  /**
   * Sends `body` via XMLHttpRequest so byte-level upload progress is observable —
   * fetch exposes none. Same auth/refresh/error semantics as `request`.
   */
  function upload(path: string, body: FormData, onProgress?: (pct: number) => void): Promise<unknown> {
    return new Promise((resolve, reject) => {
      const attempt = (retried: boolean) => {
        const xhr = new XMLHttpRequest()
        xhr.withCredentials = true
        xhr.upload.onprogress = (e) => {
          if (e.lengthComputable) onProgress?.(Math.round((e.loaded / e.total) * 100))
        }
        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            resolve(xhr.response ? JSON.parse(xhr.response) : undefined)
            return
          }
          if (xhr.status === 401 && !retried) {
            void refresh().then((ok) => (ok ? attempt(true) : reject(errorFrom(xhr))))
            return
          }
          reject(errorFrom(xhr))
        }
        xhr.onerror = () => reject(new ApiError(0, 'Network error'))
        xhr.open('POST', `${baseUrl}${path}`)
        if (accessToken) xhr.setRequestHeader('authorization', `Bearer ${accessToken}`)
        xhr.send(body)
      }
      attempt(false)
    })
  }

  function errorFrom(xhr: XMLHttpRequest): ApiError {
    let message = `HTTP ${xhr.status}`
    try {
      const body: unknown = JSON.parse(xhr.response)
      if (body && typeof body === 'object' && 'error' in body && typeof body.error === 'string') message = body.error
    } catch {
      // non-JSON error body: keep the status text
    }
    return new ApiError(xhr.status, message)
  }

  return {
    request,
    upload,
    refresh,
    setAccessToken: (token) => {
      accessToken = token
    },
    hasAccessToken: () => accessToken !== null,
  }
}

export const api = createApiClient()
