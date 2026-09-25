import { useEffect, useRef } from 'react'
import { prefersReducedMotion } from '../../../shared/motion/reducedMotion'

const MAX_PARTICLES = 150
const MAX_DPR = 2
const SPRITE = 64

type Particle = { x: number; y: number; z: number; vx: number; vy: number; r: number; phase: number; speed: number }

/** A soft glowing dot tinted with `color`, drawn once and stamped for every particle (cheap "bloom"). */
function makeSprite(color: string): HTMLCanvasElement {
  const c = document.createElement('canvas')
  c.width = c.height = SPRITE
  const g = c.getContext('2d')
  if (!g) return c
  const h = SPRITE / 2
  const grad = g.createRadialGradient(h, h, 0, h, h, h)
  grad.addColorStop(0, 'rgba(255,255,255,1)')
  grad.addColorStop(0.12, color)
  grad.addColorStop(0.4, color.replace(/rgb\((.+)\)/, 'rgba($1, 0.25)'))
  grad.addColorStop(1, 'rgba(0,0,0,0)')
  g.fillStyle = grad
  g.fillRect(0, 0, SPRITE, SPRITE)
  return c
}

function spawn(w: number, h: number, anywhere: boolean): Particle {
  const z = 0.35 + Math.random() * 0.65
  return {
    x: Math.random() * w,
    y: anywhere ? Math.random() * h : h + 20,
    z,
    vx: (Math.random() - 0.5) * 0.12,
    vy: -(0.08 + Math.random() * 0.3) * z,
    r: (2 + Math.random() * 7) * z,
    phase: Math.random() * Math.PI * 2,
    speed: 0.6 + Math.random() * 1.4,
  }
}

/**
 * Canvas of drifting glowing particles (the mod's AmbientParticles + bloom look).
 * `color` is an opaque `rgb(r, g, b)`. Pauses off-screen; a single static frame under reduced motion.
 */
export function ParticleField({ color, className = '' }: { color: string; className?: string }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const spriteRef = useRef<HTMLCanvasElement | null>(null)

  useEffect(() => {
    spriteRef.current = makeSprite(color)
  }, [color])

  useEffect(() => {
    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!canvas || !ctx) return
    const reduced = prefersReducedMotion()
    const dpr = Math.min(window.devicePixelRatio || 1, MAX_DPR)
    let w = 0
    let h = 0
    let particles: Particle[] = []
    let frame = 0
    let visible = true
    let time = 0
    const pointer = { x: 0, y: 0, tx: 0, ty: 0 }

    const resize = () => {
      w = canvas.clientWidth
      h = canvas.clientHeight
      canvas.width = Math.round(w * dpr)
      canvas.height = Math.round(h * dpr)
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      const count = Math.min(MAX_PARTICLES, Math.round((w * h) / 8000))
      particles = Array.from({ length: count }, () => spawn(w, h, true))
    }

    const draw = () => {
      ctx.clearRect(0, 0, w, h)
      ctx.globalCompositeOperation = 'lighter'
      const sprite = spriteRef.current
      if (!sprite) return
      for (const p of particles) {
        const twinkle = 0.55 + 0.45 * Math.sin(time * 0.002 * p.speed + p.phase)
        const size = p.r * 4
        ctx.globalAlpha = twinkle * (0.35 + p.z * 0.65)
        ctx.drawImage(sprite, p.x + pointer.x * p.z - size / 2, p.y + pointer.y * p.z - size / 2, size, size)
      }
      ctx.globalAlpha = 1
    }

    const step = (now: number) => {
      const dt = Math.min(48, time ? now - time : 16) / 16
      time = now
      pointer.x += (pointer.tx - pointer.x) * 0.04
      pointer.y += (pointer.ty - pointer.y) * 0.04
      for (let i = 0; i < particles.length; i++) {
        const p = particles[i]
        p.x += (p.vx + Math.sin(now * 0.0004 + p.phase) * 0.08) * dt
        p.y += p.vy * dt
        if (p.y < -30 || p.x < -30 || p.x > w + 30) particles[i] = spawn(w, h, false)
      }
      draw()
      frame = visible ? requestAnimationFrame(step) : 0
    }

    resize()
    if (reduced) {
      draw()
      return
    }

    const onPointer = (e: PointerEvent) => {
      pointer.tx = (e.clientX / window.innerWidth - 0.5) * -30
      pointer.ty = (e.clientY / window.innerHeight - 0.5) * -20
    }
    const ro = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(resize)
    ro?.observe(canvas)
    const io =
      typeof IntersectionObserver === 'undefined'
        ? null
        : new IntersectionObserver(([entry]) => {
            visible = entry.isIntersecting && !document.hidden
            if (visible && !frame) frame = requestAnimationFrame(step)
          })
    io?.observe(canvas)
    window.addEventListener('pointermove', onPointer, { passive: true })
    frame = requestAnimationFrame(step)

    return () => {
      cancelAnimationFrame(frame)
      ro?.disconnect()
      io?.disconnect()
      window.removeEventListener('pointermove', onPointer)
    }
  }, [])

  return <canvas ref={canvasRef} aria-hidden="true" data-testid="particle-field" className={`pointer-events-none absolute inset-0 h-full w-full ${className}`} />
}
