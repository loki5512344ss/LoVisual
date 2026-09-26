import type { CSSProperties } from 'react'
import { argbToCss, type GradientSpec, parseArgb, type ThemeEntry, withAlpha } from '../themes/types'

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
  Object.assign(vars, derivedVars(entry))
  for (const [slot, key, flat] of GRADIENT_SLOTS) {
    const spec = entry[key]
    vars[`--gui-${slot}-gradient`] = gradientToCss(spec, t[flat])
    vars[`--gui-${slot}-from`] = argbToCss(spec.enabled ? spec.start : t[flat])
    vars[`--gui-${slot}-to`] = argbToCss(spec.enabled ? spec.end : t[flat])
    vars[`--gui-${slot}-angle`] = `${cssAngle(spec.angle)}deg`
  }
  return vars
}

type Argb = { r: number; g: number; b: number; a: number }

/** `ClickGuiDrawHelpers.mixColor`: per-channel lerp of two packed ARGB colors, alpha included. */
function mix(from: Argb, to: Argb, t: number): Argb {
  const l = (x: number, y: number) => Math.round(x * (1 - t) + y * t)
  return { r: l(from.r, to.r), g: l(from.g, to.g), b: l(from.b, to.b), a: l(from.a, to.a) }
}

const opaque = (c: Argb): Argb => ({ ...c, a: 255 })
const css = (c: Argb, alpha = c.a) => `rgba(${c.r}, ${c.g}, ${c.b}, ${Number((alpha / 255).toFixed(3))})`

/**
 * Colors the in-game renderers derive from the palette at draw time — `ModulesMenuStyle.syncTheme`
 * (category panels) and `ClickGuiTabBar.drawTabs` (the Modules/Settings pill) — so the replica
 * gets the very same tints instead of approximating them in CSS.
 */
function derivedVars(entry: ThemeEntry): CssVars {
  const p = Object.fromEntries(Object.entries(entry.theme).map(([k, v]) => [k, parseArgb(v)])) as Record<
    keyof ThemeEntry['theme'],
    Argb
  >
  const accent = opaque(p.accent)
  return {
    '--gui-panel-tint': css(mix(opaque(p.surface), accent, 0.07)),
    '--gui-panel-stroke': css(mix(opaque(p.strokeSoft), accent, 0.155), 76),
    '--gui-split': css(mix(opaque(p.strokeSoft), accent, 0.12), 34),
    '--gui-row-hover': css(mix(opaque(p.surfaceHover), accent, 0.125), 34),
    '--gui-row-glow': css(mix(opaque(p.accentSoft), accent, 0.42), 92),
    '--gui-row-text': css(opaque(p.textMuted), 210),
    '--gui-tab-left': css(mix(p.surface, p.windowBg, 0.34)),
    '--gui-tab-right': css(mix(p.surfaceHover, p.windowBg, 0.22)),
    '--gui-tab-active-left': css(mix(p.accentSoft, p.accent, 0.18)),
    '--gui-tab-active-right': css(mix(p.accent, p.textPrimary, 0.08)),
    '--gui-menu-left': css(mix(p.windowBg, p.surface, 0.18), 215),
    '--gui-menu-right': css(mix(p.windowBg, p.windowHeader, 0.3), 215),
  }
}
