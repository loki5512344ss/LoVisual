import { useEffect, useRef } from 'react'
import { prefersReducedMotion } from '../../../shared/motion/reducedMotion'

/** Deterministic PRNG so the landscape looks the same on every visit. */
function mulberry32(seed: number) {
  let a = seed
  return () => {
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

type Layer = {
  seed: number
  block: number
  /** Surface baseline as a fraction of height, and hill amplitude in blocks. */
  base: number
  amp: number
  top: string
  side: string
  deep: string
  trees: number
  torches: number
  /** Scroll parallax factor (fraction of scroll the layer lags behind). */
  lag: number
}

const LAYERS: Layer[] = [
  { seed: 7, block: 10, base: 0.5, amp: 9, top: '#4a3563', side: '#3a2a52', deep: '#302445', trees: 0, torches: 0, lag: 0.42 },
  { seed: 21, block: 16, base: 0.6, amp: 6, top: '#35304f', side: '#241f38', deep: '#1c1830', trees: 0.08, torches: 0, lag: 0.28 },
  { seed: 43, block: 26, base: 0.74, amp: 4, top: '#3d6b45', side: '#1c2a26', deep: '#141c1c', trees: 0.12, torches: 0.1, lag: 0.12 },
]

function shade(hex: string, k: number): string {
  const n = Number.parseInt(hex.slice(1), 16)
  const f = (v: number) => Math.max(0, Math.min(255, Math.round(v * k)))
  return `rgb(${f((n >> 16) & 255)}, ${f((n >> 8) & 255)}, ${f(n & 255)})`
}

function drawBlock(ctx: CanvasRenderingContext2D, x: number, y: number, s: number, color: string, k: number) {
  ctx.fillStyle = shade(color, k)
  ctx.fillRect(x, y, s, s)
  ctx.fillStyle = 'rgba(0,0,0,0.18)'
  ctx.fillRect(x, y + s - 2, s, 2)
  ctx.fillRect(x + s - 1, y, 1, s)
}

function drawLayer(ctx: CanvasRenderingContext2D, w: number, h: number, layer: Layer, accent: string) {
  const rnd = mulberry32(layer.seed)
  const s = layer.block
  const cols = Math.ceil(w / s) + 1
  const p1 = rnd() * 10
  const p2 = rnd() * 10
  const heights = Array.from({ length: cols }, (_, i) => {
    const n = Math.sin(i * 0.11 + p1) * 0.6 + Math.sin(i * 0.037 + p2) * 0.9 + Math.sin(i * 0.29 + p1 * 2) * 0.25
    return Math.round(h * layer.base / s - n * layer.amp)
  })
  const glow: [number, number][] = []
  heights.forEach((row, i) => {
    const x = i * s
    for (let r = row; r * s < h; r++) {
      const depth = r - row
      const color = depth === 0 ? layer.top : depth < 3 ? layer.side : layer.deep
      drawBlock(ctx, x, r * s, s, color, 0.9 + rnd() * 0.2)
    }
    // A thin accent rim on the sunward edge of the surface, like the mod's bloom catching the light.
    ctx.fillStyle = accent
    ctx.globalAlpha = 0.28
    ctx.fillRect(x, row * s, s, Math.max(1, s * 0.12))
    ctx.globalAlpha = 1
    const flat = heights[i + 1] === row && heights[i - 1] === row
    if (flat && rnd() < layer.trees) drawTree(ctx, x, row * s, s, rnd)
    else if (rnd() < layer.torches) {
      ctx.fillStyle = '#6b4a2b'
      ctx.fillRect(x + s * 0.42, row * s - s * 0.7, s * 0.16, s * 0.7)
      ctx.fillStyle = '#ffd27a'
      ctx.fillRect(x + s * 0.38, row * s - s * 0.9, s * 0.24, s * 0.24)
      glow.push([x + s / 2, row * s - s * 0.8])
    }
  })
  for (const [gx, gy] of glow) {
    const g = ctx.createRadialGradient(gx, gy, 0, gx, gy, s * 3)
    g.addColorStop(0, 'rgba(255, 190, 110, 0.55)')
    g.addColorStop(1, 'rgba(255, 190, 110, 0)')
    ctx.fillStyle = g
    ctx.fillRect(gx - s * 3, gy - s * 3, s * 6, s * 6)
  }
}

function drawTree(ctx: CanvasRenderingContext2D, x: number, groundY: number, s: number, rnd: () => number) {
  const trunk = 2 + Math.floor(rnd() * 2)
  for (let i = 1; i <= trunk; i++) drawBlock(ctx, x, groundY - i * s, s, '#3b2a22', 0.9)
  const top = groundY - (trunk + 1) * s
  for (let dy = 0; dy < 2; dy++) {
    for (let dx = -1; dx <= 1; dx++) drawBlock(ctx, x + dx * s, top - dy * s, s, '#1f3b2e', 0.85 + rnd() * 0.3)
  }
  drawBlock(ctx, x, top - 2 * s, s, '#244536', 1)
}

function drawSky(ctx: CanvasRenderingContext2D, w: number, h: number) {
  const rnd = mulberry32(99)
  for (let i = 0; i < 90; i++) {
    const size = rnd() < 0.85 ? 2 : 3
    ctx.fillStyle = `rgba(255, 245, 230, ${0.25 + rnd() * 0.6})`
    ctx.fillRect(Math.floor(rnd() * w), Math.floor(rnd() * h * 0.45), size, size)
  }
  // Square setting sun with a warm halo.
  const sx = w * 0.72
  const sy = h * 0.46
  const halo = ctx.createRadialGradient(sx, sy, 0, sx, sy, h * 0.4)
  halo.addColorStop(0, 'rgba(255, 170, 110, 0.55)')
  halo.addColorStop(1, 'rgba(255, 120, 90, 0)')
  ctx.fillStyle = halo
  ctx.fillRect(0, 0, w, h)
  const sun = Math.max(48, h * 0.1)
  ctx.fillStyle = '#ffcf8a'
  ctx.fillRect(sx - sun / 2, sy - sun / 2, sun, sun)
  ctx.fillStyle = '#ffe7b8'
  ctx.fillRect(sx - sun / 4, sy - sun / 4, sun / 2, sun / 2)
  // Blocky clouds.
  for (let c = 0; c < 7; c++) {
    const cx = rnd() * w
    const cy = h * (0.12 + rnd() * 0.3)
    const u = 14
    ctx.fillStyle = `rgba(${200 + rnd() * 40}, ${120 + rnd() * 40}, ${170 + rnd() * 40}, 0.2)`
    const len = 4 + Math.floor(rnd() * 7)
    for (let i = 0; i < len; i++) ctx.fillRect(cx + i * u, cy + (i % 3 === 0 ? -u : 0), u, u * (i % 3 === 0 ? 2 : 1))
  }
}

/** Procedural blocky dusk landscape with layered scroll parallax. Purely decorative. */
export function VoxelScene({ accent, className = '' }: { accent: string; className?: string }) {
  const rootRef = useRef<HTMLDivElement>(null)
  const canvases = useRef<(HTMLCanvasElement | null)[]>([])

  useEffect(() => {
    const root = rootRef.current
    if (!root) return
    const paint = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      const w = root.clientWidth
      const h = root.clientHeight * 1.25
      canvases.current.forEach((canvas, i) => {
        const ctx = canvas?.getContext('2d')
        if (!canvas || !ctx) return
        canvas.width = Math.round(w * dpr)
        canvas.height = Math.round(h * dpr)
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
        ctx.clearRect(0, 0, w, h)
        if (i === 0) drawSky(ctx, w, h)
        else drawLayer(ctx, w, h, LAYERS[i - 1], accent)
      })
    }
    paint()
    const ro = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(paint)
    ro?.observe(root)
    return () => ro?.disconnect()
  }, [accent])

  useEffect(() => {
    if (prefersReducedMotion()) return
    let frame = 0
    const lags = [0.5, ...LAYERS.map((l) => l.lag)]
    const onScroll = () => {
      if (frame) return
      frame = requestAnimationFrame(() => {
        frame = 0
        const y = Math.min(window.scrollY, 1200)
        canvases.current.forEach((c, i) => {
          if (c) c.style.transform = `translate3d(0, ${(y * lags[i]).toFixed(1)}px, 0)`
        })
      })
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => {
      window.removeEventListener('scroll', onScroll)
      cancelAnimationFrame(frame)
    }
  }, [])

  return (
    <div ref={rootRef} aria-hidden="true" className={`lv-voxel pointer-events-none absolute inset-0 overflow-hidden ${className}`}>
      {[0, 1, 2, 3].map((i) => (
        <canvas
          key={i}
          ref={(el) => {
            canvases.current[i] = el
          }}
          className="absolute inset-x-0 top-0 h-[125%] w-full will-change-transform"
        />
      ))}
    </div>
  )
}
