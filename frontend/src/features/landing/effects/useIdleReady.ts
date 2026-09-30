import { useEffect, useState } from 'react'

/**
 * Flips to true once the browser is idle (or after `timeout` ms at the latest), so heavy
 * decorative work stays out of the first paint's main-thread budget. Without
 * requestIdleCallback (Safari) a short timeout stands in.
 */
export function useIdleReady(timeout = 600): boolean {
  const [ready, setReady] = useState(false)

  useEffect(() => {
    if (typeof requestIdleCallback === 'function') {
      const id = requestIdleCallback(() => setReady(true), { timeout })
      return () => cancelIdleCallback(id)
    }
    const id = setTimeout(() => setReady(true), 200)
    return () => clearTimeout(id)
  }, [timeout])

  return ready
}
