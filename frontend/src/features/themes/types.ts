/** The mod's 12 palette slots (`Themes.Theme` record), each as `#AARRGGBB`. */
export type ThemeColors = {
  windowBg: string
  windowHeader: string
  windowStroke: string
  surface: string
  surfaceHover: string
  cardEnabled: string
  cardDisabled: string
  textPrimary: string
  textMuted: string
  accent: string
  accentSoft: string
  strokeSoft: string
}

/** Mirrors `Themes.GradientSpec`: when disabled the slot renders flat with `start`. */
export type GradientSpec = { enabled: boolean; start: string; end: string; angle: number }

/** Mirrors `Themes.ThemeEntry`. */
export type ThemeEntry = {
  id: string
  name: string
  builtin: boolean
  theme: ThemeColors
  windowGradient: GradientSpec
  headerGradient: GradientSpec
  surfaceGradient: GradientSpec
  cardGradient: GradientSpec
  strokeGradient: GradientSpec
}

type Rgba = { r: number; g: number; b: number; a: number }

/** Parses `#AARRGGBB` (the mod's packed ARGB) or `#RRGGBB` (opaque). */
export function parseArgb(hex: string): Rgba {
  const clean = hex.replace(/^#/, '')
  const full = clean.length === 6 ? `FF${clean}` : clean.padStart(8, '0')
  const n = Number.parseInt(full, 16) >>> 0
  return { a: (n >>> 24) & 0xff, r: (n >>> 16) & 0xff, g: (n >>> 8) & 0xff, b: n & 0xff }
}

const alpha = (a: number) => String(Number((a / 255).toFixed(3)))

/** `#F012191B` → `rgba(18, 25, 27, 0.941)`. */
export function argbToCss(hex: string): string {
  const { r, g, b, a } = parseArgb(hex)
  return `rgba(${r}, ${g}, ${b}, ${alpha(a)})`
}

/** Same color with its alpha replaced (0–1) — for glows built from a palette slot. */
export function withAlpha(hex: string, a: number): string {
  const { r, g, b } = parseArgb(hex)
  return `rgba(${r}, ${g}, ${b}, ${Number(a.toFixed(3))})`
}
