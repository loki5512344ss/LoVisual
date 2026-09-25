import { motion, useMotionValue, useSpring } from 'motion/react'
import { type ReactNode, useEffect, useRef } from 'react'
import { prefersReducedMotion } from '../../../shared/motion/reducedMotion'

const MAX_DEG = 8
const REST = { x: 4, y: -6 }
const SPRING = { stiffness: 120, damping: 16, mass: 0.6 }

const clamp = (v: number) => Math.max(-MAX_DEG, Math.min(MAX_DEG, v))

/** Pointer-driven 3D tilt (max 8°) on a spring; rests at a slight angle for depth. */
export function Tilt({ children, className = '' }: { children: ReactNode; className?: string }) {
  const ref = useRef<HTMLDivElement>(null)
  const rx = useMotionValue(REST.x)
  const ry = useMotionValue(REST.y)
  const rotateX = useSpring(rx, SPRING)
  const rotateY = useSpring(ry, SPRING)

  useEffect(() => {
    if (prefersReducedMotion()) return
    const onMove = (e: PointerEvent) => {
      const el = ref.current
      if (!el || e.pointerType !== 'mouse') return
      const r = el.getBoundingClientRect()
      const nx = (e.clientX - (r.left + r.width / 2)) / (window.innerWidth / 2)
      const ny = (e.clientY - (r.top + r.height / 2)) / (window.innerHeight / 2)
      ry.set(clamp(nx * MAX_DEG))
      rx.set(clamp(-ny * MAX_DEG))
    }
    const onLeave = () => {
      rx.set(REST.x)
      ry.set(REST.y)
    }
    window.addEventListener('pointermove', onMove, { passive: true })
    document.documentElement.addEventListener('pointerleave', onLeave)
    return () => {
      window.removeEventListener('pointermove', onMove)
      document.documentElement.removeEventListener('pointerleave', onLeave)
    }
  }, [rx, ry])

  return (
    <div ref={ref} className={className} style={{ perspective: 1400 }}>
      <motion.div style={{ rotateX, rotateY, transformStyle: 'preserve-3d' }}>{children}</motion.div>
    </div>
  )
}
