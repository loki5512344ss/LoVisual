import { type RefObject, useEffect, useState } from 'react'

/**
 * Tracks whether `ref` intersects the viewport. Without IntersectionObserver (old browsers,
 * jsdom) the element counts as visible so content never stays hidden.
 */
export function useInView<T extends Element>(ref: RefObject<T | null>, options: { once?: boolean; rootMargin?: string } = {}): boolean {
  const [inView, setInView] = useState(() => typeof IntersectionObserver === 'undefined')
  const { once = false, rootMargin = '0px' } = options

  useEffect(() => {
    const el = ref.current
    if (!el || typeof IntersectionObserver === 'undefined') return
    const observer = new IntersectionObserver(
      ([entry]) => {
        setInView(entry.isIntersecting)
        if (entry.isIntersecting && once) observer.disconnect()
      },
      { rootMargin },
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [ref, once, rootMargin])

  return inView
}
