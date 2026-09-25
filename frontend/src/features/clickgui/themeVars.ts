import type { CSSProperties } from 'react'
import { argbToCss, type GradientSpec, type ThemeEntry, withAlpha } from '../themes/types'

type CssVars = CSSProperties & Record<`--${string}`, string>

/**
 * The mod measures gradient angles from the +X axis with 90° pointing down (its header
 * gradient is "top-lit" at 90°); CSS measures from "to top" clockwise, so 90° there is
 * left→right. Mod angle + 90 gives the CSS angle.
 */
export const cssAngle = (modAngle: number) => (((modAngle + 90) % 360) + 360) % 360

/** `linear-gradient(...)` when the slot's gradient is enabled, else the flat fallback color. */
export function gradientToCss(spec: GradientSpec, flat: string): string {
  if (!spec.enabled) return argbToCss(flat)
  return `linear-gradient(${cssAngle(spec.angle)}deg, ${argbToCss(spec.start)}, ${argbToCss(spec.end)})`
}

const GRADIENT_SLOTS = [
  ['window', 'windowGradient', 'windowBg'],
  ['header', 'headerGradient', 'windowHeader'],
  ['surface', 'surfaceGradient', 'surface'],
  ['card', 'cardGradient', 'cardEnabled'],
  ['stroke', 'strokeGradient', 'windowStroke'],
] as const

/**
 * Maps a mod theme to the CSS custom properties the ClickGui replica reads.
 * Besides the full `--gui-<slot>-gradient` strings it emits `-from`/`-to`/`-angle` parts:
 * those are registered as typed `@property`s so a theme switch cross-fades smoothly.
 */
export function themeToCssVars(entry: ThemeEntry): CssVars {
  const t = entry.theme
  const vars: CssVars = {
    '--gui-window-bg': argbToCss(t.windowBg),
    '--gui-header': argbToCss(t.windowHeader),
    '--gui-stroke': argbToCss(t.windowStroke),
    '--gui-surface': argbToCss(t.surface),
    '--gui-surface-hover': argbToCss(t.surfaceHover),
    '--gui-card-on': argbToCss(t.cardEnabled),
    '--gui-card-off': argbToCss(t.cardDisabled),
    '--gui-text': argbToCss(t.textPrimary),
    '--gui-text-muted': argbToCss(t.textMuted),
    '--gui-accent': argbToCss(t.accent),
    '--gui-accent-soft': argbToCss(t.accentSoft),
    '--gui-stroke-soft': argbToCss(t.strokeSoft),
    '--gui-accent-glow': withAlpha(t.accent, 0.45),
  }
  for (const [slot, key, flat] of GRADIENT_SLOTS) {
    const spec = entry[key]
    vars[`--gui-${slot}-gradient`] = gradientToCss(spec, t[flat])
    vars[`--gui-${slot}-from`] = argbToCss(spec.enabled ? spec.start : t[flat])
    vars[`--gui-${slot}-to`] = argbToCss(spec.enabled ? spec.end : t[flat])
    vars[`--gui-${slot}-angle`] = `${cssAngle(spec.angle)}deg`
  }
  return vars
}
