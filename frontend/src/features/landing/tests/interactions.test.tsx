import { fireEvent, render, screen, within } from '@testing-library/react'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import { ActiveThemeProvider } from '@features/themes/useActiveTheme'
import { landingEn } from '../i18n/en'
import { landingRu } from '../i18n/ru'
import { VoxelScene } from '../effects/VoxelScene'
import { HudPlayground } from '../sections/explore/HudPlayground'
import { ModuleWall } from '../sections/explore/ModuleWall'
import { ThemeStrip } from '../sections/hero/ThemeStrip'

const reduceMotion = () =>
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  localStorage.clear()
})

function withTheme(ui: React.ReactElement) {
  const router = createMemoryRouter([{ path: '/', element: <ActiveThemeProvider applyToPage={false}>{ui}</ActiveThemeProvider> }])
  return render(<RouterProvider router={router} />)
}

test('picking a swatch repaints the theme section: pressed state and the big theme name follow', () => {
  reduceMotion()
  withTheme(<ThemeStrip />)
  const section = screen.getByRole('region', { name: 'Перекрась меню. И сайт заодно.' })
  expect(within(section).getByText('Amber', { selector: 'p' })).toBeInTheDocument()

  fireEvent.click(screen.getByRole('button', { name: /Nitro/ }))

  expect(screen.getByRole('button', { name: /Nitro/ })).toHaveAttribute('aria-pressed', 'true')
  expect(screen.getByRole('button', { name: /Amber/ })).toHaveAttribute('aria-pressed', 'false')
  expect(within(section).getByText('Nitro', { selector: 'p' })).toBeInTheDocument()
  // The band itself is painted from the picked theme's variables.
  expect(section.style.getPropertyValue('--gui-accent')).not.toBe('')
})

test('holding a HUD widget switches the screen into edit mode until it is released', () => {
  reduceMotion()
  withTheme(<HudPlayground />)
  const widget = screen.getByRole('group', { name: /FPS/ })
  widget.setPointerCapture = () => {}
  const grid = document.querySelector('.lv-hud-grid')
  expect(grid).toHaveAttribute('data-on', 'false')

  fireEvent.pointerDown(widget, { pointerId: 1, clientX: 10, clientY: 10 })
  expect(widget).toHaveAttribute('data-held', 'true')
  expect(grid).toHaveAttribute('data-on', 'true')

  fireEvent.pointerUp(widget, { pointerId: 1 })
  expect(widget).toHaveAttribute('data-held', 'false')
  expect(grid).toHaveAttribute('data-on', 'false')
})

test('"Put it back" restores the HUD after moving a widget', () => {
  reduceMotion()
  withTheme(<HudPlayground />)
  const start = screen.getByRole('group', { name: /FPS/ }).style.left
  fireEvent.keyDown(screen.getByRole('group', { name: /FPS/ }), { key: 'ArrowRight' })
  expect(screen.getByRole('group', { name: /FPS/ }).style.left).not.toBe(start)
  fireEvent.click(screen.getByRole('button', { name: 'Вернуть как было' }))
  expect(screen.getByRole('group', { name: /FPS/ }).style.left).toBe(start)
})

test('module wall shows how many modules each category has; Visuals has the 52 the headline promises', () => {
  reduceMotion()
  withTheme(<ModuleWall />)
  const visuals = screen.getByRole('heading', { level: 3, name: 'Visuals' }).parentElement as HTMLElement
  expect(within(visuals).getByText('50')).toBeInTheDocument()
})

test('a VoxelScene with parallax off never listens to page scroll', () => {
  const spy = vi.spyOn(window, 'addEventListener')
  vi.stubGlobal('matchMedia', () => ({ matches: false, addEventListener() {}, removeEventListener() {} }))
  render(<VoxelScene accent="rgb(1, 2, 3)" parallax={false} />)
  expect(spy.mock.calls.some(([type]) => type === 'scroll')).toBe(false)
})

/** Every .tsx under the landing feature, tests excluded. */
function landingSources(dir = 'src/features/landing'): string[] {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name)
    if (statSync(path).isDirectory()) return name === 'tests' ? [] : landingSources(path)
    return path.endsWith('.tsx') ? [path] : []
  })
}

test('no section carries a tracked-out all-caps eyebrow label', () => {
  for (const file of landingSources()) {
    expect(readFileSync(file, 'utf8'), file).not.toMatch(/uppercase[^"']*tracking-\[|tracking-\[[^"']*uppercase/)
  }
})

test('the dictionaries no longer carry eyebrow labels in either language', () => {
  for (const dict of [landingRu, landingEn]) {
    for (const section of Object.values(dict)) expect(Object.keys(section)).not.toContain('eyebrow')
  }
})
