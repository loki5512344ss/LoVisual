import { useLayoutEffect, useRef } from 'react'
import { prefersReducedMotion } from './reducedMotion'

const lastRect = new Map<string, DOMRect>()

/**
 * A "shared layout" thumb without a library: each tab renders its own thumb while active;
 * when one mounts it slides in from where the previous thumb with the same `id` sat (FLIP).
 */
export function useSharedThumb<T extends HTMLElement>(id: string, duration = 380) {
  const ref = useRef<T>(null)

  useLayoutEffect(() => {
    const el = ref.current
    if (!el) return
    const now = el.getBoundingClientRect()
    const prev = lastRect.get(id)
    lastRect.set(id, now)
    if (!prev || !now.width || !now.height || prefersReducedMotion() || typeof el.animate !== 'function') return
    const from = `translate(${prev.left - now.left}px, ${prev.top - now.top}px) scale(${prev.width / now.width}, ${prev.height / now.height})`
    const anim = el.animate([{ transform: from, transformOrigin: '0 0' }, { transform: 'none', transformOrigin: '0 0' }], {
      duration,
      easing: 'cubic-bezier(0.22, 1, 0.36, 1)',
    })
    return () => anim.cancel()
  }, [id, duration])

  return ref
}
