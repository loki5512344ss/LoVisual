import { expect, test } from 'vitest'
import { PRESETS } from '../../themes/presets.generated'
import { cssAngle, themeToCssVars } from '../themeVars'

const preset = (id: string) => {
  const p = PRESETS.find((x) => x.id === id)
  if (!p) throw new Error(id)
  return p
}

test('maps every palette slot of Classic exactly', () => {
  const vars = themeToCssVars(preset('classic'))
  expect(vars['--gui-window-bg']).toBe('rgba(18, 25, 27, 0.941)')
  expect(vars['--gui-header']).toBe('rgba(20, 34, 38, 0.941)')
  expect(vars['--gui-stroke']).toBe('rgba(32, 48, 48, 0.376)')
  expect(vars['--gui-card-off']).toBe('rgba(51, 64, 70, 0)')
  expect(vars['--gui-text']).toBe('rgba(255, 255, 255, 1)')
  expect(vars['--gui-text-muted']).toBe('rgba(198, 210, 205, 0.533)')
  expect(vars['--gui-accent']).toBe('rgba(92, 200, 231, 1)')
  expect(vars['--gui-accent-soft']).toBe('rgba(92, 200, 231, 0.502)')
  expect(vars['--gui-stroke-soft']).toBe('rgba(255, 255, 255, 0.376)')
  for (const key of ['--gui-surface', '--gui-surface-hover', '--gui-card-on']) expect(vars).toHaveProperty(key)
})

test('disabled gradients fall back to the flat color, enabled ones become linear-gradients', () => {
  const classic = themeToCssVars(preset('classic'))
  expect(classic['--gui-window-gradient']).toBe('rgba(18, 25, 27, 0.941)')
  expect(classic['--gui-header-gradient']).toBe('linear-gradient(180deg, rgba(39, 52, 55, 0.941), rgba(19, 32, 36, 0.941))')

  const nitro = themeToCssVars(preset('nitro'))
  expect(nitro['--gui-window-gradient']).toBe('linear-gradient(135deg, rgba(54, 32, 95, 1), rgba(11, 125, 255, 1))')
  expect(nitro['--gui-window-from']).toBe('rgba(54, 32, 95, 1)')
  expect(nitro['--gui-window-to']).toBe('rgba(11, 125, 255, 1)')
  expect(nitro['--gui-window-angle']).toBe('135deg')
})

test('mod angles convert to CSS angles', () => {
  expect(cssAngle(90)).toBe(180)
  expect(cssAngle(0)).toBe(90)
  expect(cssAngle(300)).toBe(30)
})
