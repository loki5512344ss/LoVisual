import { memo } from 'react'

/**
 * The world behind the ClickGui: a night sky over a frozen lake, drawn as one small SVG
 * (no images). Horizon at ~46%; the lake starts as dark as the sky and brightens towards the
 * viewer, with a mist band over the shoreline, so there is no hard edge behind the panels.
 * A real screenshot can replace it: set `--gui-backdrop-image` (or ClickGuiWindow's `backdrop`),
 * and `.lv-gui-photo` covers this scene.
 */

/** Deterministic pseudo-random sequence, so the sky is identical on every render and in SSR. */
function rng(seed: number) {
  let s = seed
  return () => {
    s = (s * 16807) % 2147483647
    return (s - 1) / 2147483646
  }
}

const W = 560
const HORIZON = 150

const rand = rng(7)
const STARS = Array.from({ length: 80 }, () => ({
  x: rand() * W,
  y: rand() * (HORIZON - 30),
  r: rand() < 0.12 ? 0.9 : 0.45 + rand() * 0.35,
  o: 0.35 + rand() * 0.6,
  twinkle: rand() < 0.2,
}))
const FLAKES = Array.from({ length: 26 }, () => ({ x: rand() * W, y: rand() * 300, s: 0.8 + rand() * 1.1, d: 9 + rand() * 9 }))

/** A Minecraft spruce: a trunk and stacked needle tiers that alternate wide/narrow, snow on each tier. */
function Spruce({ x, base, h }: { x: number; base: number; h: number }) {
  const u = h / 14
  const tiers = [5, 3.5, 4.5, 3, 3.5, 2, 2.5, 1, 1]
  return (
    <g>
      <rect x={x - u / 2} y={base - 2 * u} width={u} height={2 * u} fill="#1a1210" />
      {tiers.map((half, i) => {
        const y = base - (i + 2) * u - u * 0.35
        return (
          <g key={i}>
            <rect x={x - half * u} y={y} width={half * 2 * u} height={u * 1.05} fill={i % 2 ? '#0c1b1d' : '#0f2224'} />
            {i % 2 === 0 && <rect x={x - half * u + u * 0.6} y={y} width={half * 2 * u - u * 1.6} height={u * 0.4} fill="#d4dcf3" opacity={0.55} />}
          </g>
        )
      })}
    </g>
  )
}

/** Stepped (voxel) ridge: one block column every `step` units. */
function ridge(from: number, to: number, step: number, base: number, heights: number[]) {
  let d = `M${from} ${HORIZON + 6}`
  for (let x = from, i = 0; x < to; x += step, i++) d += ` V${base - heights[i % heights.length]} H${Math.min(x + step, to)}`
  return `${d} V${HORIZON + 6} Z`
}

const TREES = [
  { x: 14, base: 142, h: 62 }, { x: 44, base: 140, h: 48 }, { x: 70, base: 144, h: 36 }, { x: 100, base: 147, h: 22 },
  { x: 268, base: 148, h: 14 }, { x: 281, base: 148, h: 10 },
  { x: 440, base: 147, h: 22 }, { x: 488, base: 141, h: 44 }, { x: 516, base: 139, h: 58 }, { x: 548, base: 142, h: 50 },
]

export const Backdrop = memo(function Backdrop() {
  return (
    <svg className="lv-gui-backdrop" viewBox={`0 0 ${W} 324`} preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <defs>
        <linearGradient id="lvgb-sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#020309" />
          <stop offset="0.55" stopColor="#060b1b" />
          <stop offset="1" stopColor="#141d3d" />
        </linearGradient>
        <linearGradient id="lvgb-lake" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#121a36" />
          <stop offset="0.3" stopColor="#262f5a" />
          <stop offset="0.7" stopColor="#434c84" />
          <stop offset="1" stopColor="#5d67a3" />
        </linearGradient>
        <linearGradient id="lvgb-mist" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#7d8ad0" stopOpacity="0" />
          <stop offset="0.5" stopColor="#7d8ad0" stopOpacity="0.22" />
          <stop offset="1" stopColor="#7d8ad0" stopOpacity="0" />
        </linearGradient>
        <radialGradient id="lvgb-moon" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0" stopColor="#dfe6ff" stopOpacity="0.28" />
          <stop offset="1" stopColor="#dfe6ff" stopOpacity="0" />
        </radialGradient>
        <linearGradient id="lvgb-glint" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#c9d3ff" stopOpacity="0.16" />
          <stop offset="1" stopColor="#c9d3ff" stopOpacity="0" />
        </linearGradient>
      </defs>

      <rect width={W} height={HORIZON + 6} fill="url(#lvgb-sky)" />
      {STARS.map((s, i) => (
        <circle key={i} cx={s.x} cy={s.y} r={s.r} fill="#fff" opacity={s.o} className={s.twinkle ? 'lv-gui-twinkle' : undefined} style={{ animationDelay: `${(i % 7) * 0.6}s` }} />
      ))}
      <circle cx={452} cy={44} r={46} fill="url(#lvgb-moon)" />
      <rect x={445} y={37} width={14} height={14} fill="#eef2ff" opacity={0.92} />
      <rect x={448} y={40} width={4} height={3} fill="#c9d0ea" opacity={0.7} />

      <path d={ridge(0, W, 14, HORIZON, [18, 24, 30, 26, 20, 14, 10, 8, 12, 16, 22, 28, 34, 30, 24, 18, 12, 10, 14, 20, 26, 30, 24, 20, 16, 22, 28, 34, 38, 32, 26, 20, 16, 12, 10, 14, 18, 24, 28, 22])} fill="#1a2349" />
      <path d={ridge(0, W, 14, HORIZON, [18, 24, 30, 26, 20, 14, 10, 8, 12, 16, 22, 28, 34, 30, 24, 18, 12, 10, 14, 20, 26, 30, 24, 20, 16, 22, 28, 34, 38, 32, 26, 20, 16, 12, 10, 14, 18, 24, 28, 22])} fill="none" stroke="#9aa6d8" strokeWidth="0.6" opacity="0.35" />
      <path d={ridge(0, 120, 8, HORIZON, [10, 12, 12, 9, 7, 5, 5, 4, 3, 3, 2, 2, 1, 1, 0])} fill="#0d1530" />
      <path d={ridge(380, W, 8, HORIZON, [0, 1, 2, 3, 4, 6, 6, 8, 9, 10, 10, 12, 12, 11, 13, 12, 12, 11, 12, 13, 12, 12, 11])} fill="#0d1530" />
      {TREES.map((t, i) => (
        <Spruce key={i} {...t} />
      ))}

      <rect y={HORIZON} width={W} height={324 - HORIZON} fill="url(#lvgb-lake)" />
      <g stroke="#dfe6ff" strokeWidth="0.5">
        {[6, 14, 25, 40, 60, 86, 118, 156].map((dy, i) => (
          <line key={dy} x1="0" x2={W} y1={HORIZON + dy} y2={HORIZON + dy} opacity={0.02 + i * 0.008} />
        ))}
      </g>
      <rect x={430} y={HORIZON} width={44} height={120} fill="url(#lvgb-glint)" />
      <rect y={HORIZON - 22} width={W} height={44} fill="url(#lvgb-mist)" />

      <g fill="#fff">
        {FLAKES.map((f, i) => (
          <rect key={i} className="lv-gui-flake" x={f.x} y={f.y} width={f.s} height={f.s} opacity={0.55} style={{ animationDuration: `${f.d}s`, animationDelay: `${-i * 1.3}s` }} />
        ))}
      </g>
    </svg>
  )
})
