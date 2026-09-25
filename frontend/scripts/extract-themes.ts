// Generates src/features/themes/presets.generated.ts from the mod's built-in theme presets.
// Usage: bun run gen:themes
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const THEME_DIR = resolve(here, '../../mod/src/main/java/dev/loki/lovisual/features/theme')
const PRESET_FILES = [1, 2, 3].map((n) => resolve(THEME_DIR, `presets/ThemePresets${n}.java`))
const OUT = resolve(here, '../src/features/themes/presets.generated.ts')

const COLOR_KEYS = [
  'windowBg', 'windowHeader', 'windowStroke', 'surface', 'surfaceHover', 'cardEnabled',
  'cardDisabled', 'textPrimary', 'textMuted', 'accent', 'accentSoft', 'strokeSoft',
] as const
const GRADIENT_KEYS = ['windowGradient', 'headerGradient', 'surfaceGradient', 'cardGradient', 'strokeGradient'] as const

type Colors = Record<(typeof COLOR_KEYS)[number], number>
type Gradient = { enabled: boolean; start: number; end: number; angle: number }

const stripComments = (src: string) => src.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '')

/** Splits a comma-separated argument list, respecting nested parentheses. */
function splitArgs(args: string): string[] {
  const out: string[] = []
  let depth = 0
  let cur = ''
  for (const ch of args) {
    if (ch === '(') depth++
    if (ch === ')') depth--
    if (ch === ',' && depth === 0) {
      out.push(cur.trim())
      cur = ''
    } else cur += ch
  }
  if (cur.trim()) out.push(cur.trim())
  return out
}

/** Finds `static final <type> NAME = new <type>(...)` declarations and returns their raw argument lists. */
function declarations(src: string, type: string): Map<string, string[]> {
  const found = new Map<string, string[]>()
  const re = new RegExp(`static\\s+final\\s+${type}\\s+(\\w+)\\s*=\\s*new\\s+${type}\\s*\\(`, 'g')
  for (let m = re.exec(src); m; m = re.exec(src)) {
    let depth = 1
    let i = re.lastIndex
    for (; depth > 0; i++) {
      if (src[i] === '(') depth++
      if (src[i] === ')') depth--
    }
    found.set(m[1], splitArgs(src.slice(re.lastIndex, i - 1)))
  }
  return found
}

const parseInt32 = (lit: string) => Number.parseInt(lit.replace(/^0x/i, ''), 16) >>> 0

// Port of ThemesBlending.adjustBrightness (Java Math.round(float) == floor(x + 0.5)).
function adjustBrightness(argb: number, delta: number): number {
  const ch = (v: number) => {
    const moved = v + Math.floor((delta >= 0 ? 255 - v : v) * delta + 0.5)
    return Math.min(255, Math.max(0, moved))
  }
  const a = (argb >>> 24) & 0xff
  return ((a << 24) | (ch((argb >>> 16) & 0xff) << 16) | (ch((argb >>> 8) & 0xff) << 8) | ch(argb & 0xff)) >>> 0
}

function resolveColor(expr: string, themes: Map<string, Colors>): number {
  if (/^0x[0-9a-f]+$/i.test(expr)) return parseInt32(expr)
  const ref = /^(\w+)\.(\w+)\(\)$/.exec(expr)
  const colors = ref ? themes.get(ref[1]) : undefined
  if (!ref || !colors || !(ref[2] in colors)) throw new Error(`Cannot resolve color: ${expr}`)
  return colors[ref[2] as keyof Colors]
}

function parseGradient(expr: string, themes: Map<string, Colors>): Gradient {
  const call = /^(?:Themes\.)?(flatGradient|subtleHeaderGradient)\((.+)\)$/.exec(expr)
  if (call) {
    const color = resolveColor(call[2].trim(), themes)
    if (call[1] === 'flatGradient') return { enabled: false, start: color, end: color, angle: 90 }
    return { enabled: true, start: adjustBrightness(color, 0.08), end: adjustBrightness(color, -0.06), angle: 90 }
  }
  const spec = /^new\s+GradientSpec\((.+)\)$/.exec(expr)
  if (!spec) throw new Error(`Unknown gradient expression: ${expr}`)
  const [enabled, start, end, angle] = splitArgs(spec[1])
  return {
    enabled: enabled === 'true',
    start: resolveColor(start, themes),
    end: resolveColor(end, themes),
    angle: Number.parseFloat(angle.replace(/f$/i, '')),
  }
}

const hex = (argb: number) => `#${argb.toString(16).toUpperCase().padStart(8, '0')}`

export type ParsedEntry = { const: string; id: string; name: string; builtin: boolean; colors: Colors; gradients: Gradient[] }

/** Parses the preset sources (and Themes.java for id constants and display order). */
export function parsePresets(presetSources: string[], themesSource: string): ParsedEntry[] {
  const themesJava = stripComments(themesSource)
  const ids = new Map<string, string>()
  for (const m of themesJava.matchAll(/static\s+final\s+String\s+(THEME_\w+)\s*=\s*"([^"]+)"/g)) ids.set(m[1], m[2])

  const entries: ParsedEntry[] = []
  for (const raw of presetSources) {
    const src = stripComments(raw)
    const themes = new Map<string, Colors>()
    for (const [name, args] of declarations(src, 'Theme')) {
      if (args.length !== COLOR_KEYS.length) throw new Error(`Theme ${name}: expected 12 colors, got ${args.length}`)
      themes.set(name, Object.fromEntries(COLOR_KEYS.map((k, i) => [k, parseInt32(args[i])])) as Colors)
    }
    for (const [constName, args] of declarations(src, 'ThemeEntry')) {
      const [idExpr, nameExpr, builtin, themeRef, ...grads] = args
      const idConst = idExpr.replace(/^Themes\./, '')
      const colors = themes.get(themeRef)
      if (!colors) throw new Error(`ThemeEntry ${constName}: unknown theme ${themeRef}`)
      entries.push({
        const: constName,
        id: ids.get(idConst) ?? idExpr.replace(/^"|"$/g, ''),
        name: nameExpr.replace(/^"|"$/g, ''),
        builtin: builtin === 'true',
        colors,
        gradients: grads.map((g) => parseGradient(g, themes)),
      })
    }
  }

  // Follow the mod's picker order (Themes.PRESET_LIST) where available.
  const order = /PRESET_LIST\s*=\s*List\.of\(([\s\S]*?)\);/.exec(themesJava)?.[1] ?? ''
  const rank = (e: ParsedEntry) => {
    const i = order.indexOf(`.${e.const}`)
    return i < 0 ? Number.MAX_SAFE_INTEGER : i
  }
  return entries.map((e, i) => ({ e, i })).sort((a, b) => rank(a.e) - rank(b.e) || a.i - b.i).map(({ e }) => e)
}

export function render(entries: ParsedEntry[]): string {
  const body = entries.map((e) => {
    const theme = COLOR_KEYS.map((k) => `      ${k}: '${hex(e.colors[k])}',`).join('\n')
    const grads = GRADIENT_KEYS.map((k, i) => {
      const g = e.gradients[i]
      return `    ${k}: { enabled: ${g.enabled}, start: '${hex(g.start)}', end: '${hex(g.end)}', angle: ${g.angle} },`
    }).join('\n')
    return `  {\n    id: '${e.id}',\n    name: '${e.name}',\n    builtin: ${e.builtin},\n    theme: {\n${theme}\n    },\n${grads}\n  },`
  })
  return [
    '// generated — do not edit. Source: mod/.../features/theme/presets/ThemePresets{1,2,3}.java',
    '// Regenerate with `bun run gen:themes`.',
    "import type { ThemeEntry } from './types'",
    '',
    'export const PRESETS: ThemeEntry[] = [',
    ...body,
    ']',
    '',
  ].join('\n')
}

if (import.meta.main) {
  const entries = parsePresets(
    PRESET_FILES.map((f) => readFileSync(f, 'utf8')),
    readFileSync(resolve(THEME_DIR, 'Themes.java'), 'utf8'),
  )
  writeFileSync(OUT, render(entries))
  console.log(`Wrote ${entries.length} presets to ${OUT}`)
}
