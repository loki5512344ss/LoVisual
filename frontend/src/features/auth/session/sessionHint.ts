const KEY = 'lv_session'

/**
 * A guest has no refresh cookie, so asking /auth/refresh only produces a 401 (a console error
 * and a wasted round trip on every first visit). This flag records "this browser signed in";
 * without it the restore step is skipped. Storage may be unavailable, so it never throws.
 */
export function hasSessionHint(): boolean {
  try {
    return localStorage.getItem(KEY) === '1'
  } catch {
    return false
  }
}

export function setSessionHint(on: boolean): void {
  try {
    if (on) localStorage.setItem(KEY, '1')
    else localStorage.removeItem(KEY)
  } catch {
    // Private mode or blocked storage: the hint is only an optimisation.
  }
}
