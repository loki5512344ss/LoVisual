import { useEffect, useState } from 'react'
import { prefersReducedMotion } from './reducedMotion'
import './motion.css'

/**
 * Keeps an element mounted for `ms` after `show` turns false so a CSS exit animation
 * (keyed on `data-state="closed"`) can play. Under reduced motion it unmounts at once.
 */
export function usePresence(show: boolean, ms: number): { mounted: boolean; state: 'open' | 'closed' } {
  const [mounted, setMounted] = useState(show)
  if (show && !mounted) setMounted(true)

  useEffect(() => {
    if (show || !mounted) return
    const id = window.setTimeout(() => setMounted(false), ms)
    return () => window.clearTimeout(id)
  }, [show, mounted, ms])

  if (!show && prefersReducedMotion()) return { mounted: false, state: 'closed' }
  return { mounted: show || mounted, state: show ? 'open' : 'closed' }
}
