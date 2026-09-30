import { useLayoutEffect, useRef, useState } from 'react'
import { prefersReducedMotion } from './reducedMotion'
import './motion.css'

/**
 * Text that rolls upward when `value` changes: the old value slides out over the new one.
 * Meant to sit in a positioned, overflow-hidden box; `className` styles both lines.
 */
export function RollText({ value, className = '' }: { value: string; className?: string }) {
  const [leaving, setLeaving] = useState<string | null>(null)
  const shown = useRef(value)

  useLayoutEffect(() => {
    if (shown.current === value) return
    const old = shown.current
    shown.current = value
    if (prefersReducedMotion()) return
    setLeaving(old)
    const id = window.setTimeout(() => setLeaving(null), 450)
    return () => window.clearTimeout(id)
  }, [value])

  return (
    <>
      {leaving !== null && (
        <p key={`out-${leaving}`} aria-hidden="true" className={`${className} lv-roll-out`}>
          {leaving}
        </p>
      )}
      <p key={value} className={`${className} ${leaving !== null ? 'lv-roll-in' : ''}`}>
        {value}
      </p>
    </>
  )
}
