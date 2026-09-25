import { PRESETS } from '../presets.generated'
import type { GradientSpec, ThemeColors, ThemeEntry } from '../types'

// The mod serialises a theme as a FLAT map: `name`, the 12 color slots as
// `#AARRGGBB`, then for each of the 5 gradient slots `Enabled/Start/End/Angle`.
// See mod ThemesProfileCodec.toProfileValues — keep these lists in lockstep.
const COLOR_KEYS: (keyof ThemeColors)[] = [
  'windowBg',
  'windowHeader',
  'windowStroke',
  'surface',
  'surfaceHover',
  'cardEnabled',
  'cardDisabled',
  'textPrimary',
  'textMuted',
  'accent',
  'accentSoft',
  'strokeSoft',
]

/** The 12 editable palette slots, in the mod's profile order. */
export const COLOR_SLOTS = COLOR_KEYS

/** The 5 gradient slots: profile prefix + the matching `ThemeEntry` field. */
export const GRADIENT_SLOTS = [
  { prefix: 'window', key: 'windowGradient' },
  { prefix: 'header', key: 'headerGradient' },
  { prefix: 'surface', key: 'surfaceGradient' },
  { prefix: 'card', key: 'cardGradient' },
  { prefix: 'stroke', key: 'strokeGradient' },
] as const satisfies { prefix: string; key: keyof ThemeEntry }[]

const GRADIENT_KEYS: [string, keyof ThemeEntry][] = [
  ['window', 'windowGradient'],
  ['header', 'headerGradient'],
  ['surface', 'surfaceGradient'],
  ['card', 'cardGradient'],
  ['stroke', 'strokeGradient'],
]

const CLASSIC = PRESETS[0]

type Profile = Record<string, string | boolean | number>

const upper = (hex: string) => hex.toUpperCase()

/** Pack an entry into the mod's flat profile object. */
export function toProfile(entry: ThemeEntry): Profile {
  const out: Profile = { name: entry.name }
  for (const k of COLOR_KEYS) out[k] = upper(entry.theme[k])
  for (const [prefix, key] of GRADIENT_KEYS) {
    const g = entry[key] as GradientSpec
    out[`${prefix}GradientEnabled`] = g.enabled
    out[`${prefix}GradientStart`] = upper(g.start)
    out[`${prefix}GradientEnd`] = upper(g.end)
    out[`${prefix}GradientAngle`] = g.angle
  }
  return out
}

/** The mod's profile as an ordered JSON string (human-editable, keys match the mod exactly). */
export function toProfileJson(entry: ThemeEntry): string {
  return JSON.stringify(toProfile(entry), null, 2)
}

const str = (v: unknown, fallback: string) => (typeof v === 'string' && v ? upper(v) : fallback)
const rawStr = (v: unknown, fallback: string) => (typeof v === 'string' && v.trim() ? v : fallback)
const bool = (v: unknown, fallback: boolean) => (typeof v === 'boolean' ? v : fallback)
const num = (v: unknown, fallback: number) => (typeof v === 'number' && Number.isFinite(v) ? v : fallback)

function readGradient(map: Profile, prefix: string, fallback: GradientSpec): GradientSpec {
  return {
    enabled: bool(map[`${prefix}GradientEnabled`], fallback.enabled),
    start: str(map[`${prefix}GradientStart`], fallback.start),
    end: str(map[`${prefix}GradientEnd`], fallback.end),
    angle: num(map[`${prefix}GradientAngle`], fallback.angle),
  }
}

/** Slug matching the mod's `uniqueCustomId` spirit: lowercase, non-alphanumeric → `_`. */
const slug = (name: string) => name.trim().toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'custom'

function fromProfile(map: Profile): ThemeEntry {
  const theme = Object.fromEntries(
    COLOR_KEYS.map((k) => [k, str(map[k], CLASSIC.theme[k])]),
  ) as unknown as ThemeColors
  const name = rawStr(map.name, 'Custom theme')
  return {
    id: slug(name),
    name,
    builtin: false,
    theme,
    windowGradient: readGradient(map, 'window', CLASSIC.windowGradient),
    headerGradient: readGradient(map, 'header', CLASSIC.headerGradient),
    surfaceGradient: readGradient(map, 'surface', CLASSIC.surfaceGradient),
    cardGradient: readGradient(map, 'card', CLASSIC.cardGradient),
    strokeGradient: readGradient(map, 'stroke', CLASSIC.strokeGradient),
  }
}

/** Rebuild an entry from JSON. Tolerant: missing keys fall back to Classic; bad input → error. */
export function fromProfileJson(json: string): ThemeEntry | { error: string } {
  let parsed: unknown
  try {
    parsed = JSON.parse(json)
  } catch {
    return { error: 'invalidJson' }
  }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return { error: 'invalidShape' }
  return fromProfile(parsed as Profile)
}

// base64url without padding, so the share hash is URL-fragment safe.
function b64url(text: string): string {
  const b64 = btoa(unescape(encodeURIComponent(text)))
  return b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function unb64url(b64: string): string {
  const pad = b64.length % 4 ? '='.repeat(4 - (b64.length % 4)) : ''
  const std = b64.replace(/-/g, '+').replace(/_/g, '/') + pad
  return decodeURIComponent(escape(atob(std)))
}

/** Compact profile → URL-safe hash for the `#t=` share link. */
export function toShareHash(entry: ThemeEntry): string {
  return b64url(JSON.stringify(toProfile(entry)))
}

/** Decode a `#t=` hash back into an entry (or an error shape). */
export function fromShareHash(hash: string): ThemeEntry | { error: string } {
  try {
    return fromProfileJson(unb64url(hash))
  } catch {
    return { error: 'invalidHash' }
  }
}
