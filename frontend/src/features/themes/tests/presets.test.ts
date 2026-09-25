import { expect, test } from 'vitest'
import { PRESETS } from '../presets.generated'
import { argbToCss, parseArgb } from '../types'

const javaSources = import.meta.glob<string>(
  '../../../../../mod/src/main/java/dev/loki/lovisual/features/theme/presets/ThemePresets*.java',
  { query: '?raw', import: 'default', eager: true },
)

test('generated presets include Classic with the mod accent', () => {
  const classic = PRESETS.find((p) => p.id === 'classic')
  expect(classic?.name).toBe('Classic')
  expect(classic?.theme.accent).toBe('#FF5CC8E7')
  expect(classic?.theme.windowBg).toBe('#F012191B')
  // 0x334046 in Java is a 6-digit literal: alpha 00.
  expect(classic?.theme.cardDisabled).toBe('#00334046')
})

test('one preset per ThemeEntry constant in the mod sources', () => {
  const sources = Object.values(javaSources)
  expect(sources).toHaveLength(3)
  const count = sources.reduce((n, src) => n + (src.match(/static\s+final\s+ThemeEntry\s+\w+\s*=/g)?.length ?? 0), 0)
  expect(count).toBeGreaterThan(0)
  expect(PRESETS).toHaveLength(count)
  expect(new Set(PRESETS.map((p) => p.id)).size).toBe(count)
})

test('derived header gradient matches ThemesBlending.subtleHeaderGradient', () => {
  const classic = PRESETS.find((p) => p.id === 'classic')
  // 0xF0142226 lightened by 8% and darkened by 6%, as the mod computes it.
  expect(classic?.headerGradient).toEqual({ enabled: true, start: '#F0273437', end: '#F0132024', angle: 90 })
})

test('argbToCss converts packed ARGB', () => {
  expect(argbToCss('#F012191B')).toBe('rgba(18, 25, 27, 0.941)')
  expect(argbToCss('#FF5CC8E7')).toBe('rgba(92, 200, 231, 1)')
  expect(argbToCss('#00334046')).toBe('rgba(51, 64, 70, 0)')
  expect(parseArgb('#5CC8E7')).toEqual({ a: 255, r: 92, g: 200, b: 231 })
})
