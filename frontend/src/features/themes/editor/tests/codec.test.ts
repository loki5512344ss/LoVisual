import { describe, expect, test } from 'vitest'
import { PRESETS } from '../../presets.generated'
import { fromProfileJson, fromShareHash, toProfileJson, toShareHash } from '../codec'

// The mod's ThemesProfileCodec.toProfileValues writes exactly these keys, in this order.
const PROFILE_KEYS = [
  'name',
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
  'windowGradientEnabled',
  'windowGradientStart',
  'windowGradientEnd',
  'windowGradientAngle',
  'headerGradientEnabled',
  'headerGradientStart',
  'headerGradientEnd',
  'headerGradientAngle',
  'surfaceGradientEnabled',
  'surfaceGradientStart',
  'surfaceGradientEnd',
  'surfaceGradientAngle',
  'cardGradientEnabled',
  'cardGradientStart',
  'cardGradientEnd',
  'cardGradientAngle',
  'strokeGradientEnabled',
  'strokeGradientStart',
  'strokeGradientEnd',
  'strokeGradientAngle',
]

const editable = (e: (typeof PRESETS)[number]) => ({
  name: e.name,
  theme: e.theme,
  windowGradient: e.windowGradient,
  headerGradient: e.headerGradient,
  surfaceGradient: e.surfaceGradient,
  cardGradient: e.cardGradient,
  strokeGradient: e.strokeGradient,
})

describe('theme profile codec', () => {
  test('JSON keys match the mod profile exactly', () => {
    expect(Object.keys(JSON.parse(toProfileJson(PRESETS[0])))).toEqual(PROFILE_KEYS)
  })

  test('round-trips every preset through the profile JSON', () => {
    for (const preset of PRESETS) {
      const result = fromProfileJson(toProfileJson(preset))
      expect('error' in result).toBe(false)
      if (!('error' in result)) expect(editable(result)).toEqual(editable(preset))
    }
  })

  test('share hash round-trips', () => {
    for (const preset of PRESETS) {
      const result = fromShareHash(toShareHash(preset))
      if ('error' in result) throw new Error('unexpected hash error')
      expect(editable(result)).toEqual(editable(preset))
    }
  })

  test('rejects malformed JSON and non-objects', () => {
    expect(fromProfileJson('{ nope')).toEqual({ error: 'invalidJson' })
    expect(fromProfileJson('[1,2,3]')).toEqual({ error: 'invalidShape' })
    expect(fromShareHash('%%%not-base64%%%')).toEqual({ error: 'invalidHash' })
  })

  test('is tolerant: missing keys fall back to Classic', () => {
    const result = fromProfileJson(JSON.stringify({ name: 'Sparse' }))
    if ('error' in result) throw new Error('unexpected error')
    expect(result.name).toBe('Sparse')
    expect(result.theme).toEqual(PRESETS[0].theme)
  })
})
