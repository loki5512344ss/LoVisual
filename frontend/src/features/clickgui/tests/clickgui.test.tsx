import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { PRESETS } from '../../themes/presets.generated'
import { argbToCss } from '../../themes/types'
import { ActiveThemeProvider } from '../../themes/useActiveTheme'
import { ClickGuiWindow } from '../ClickGuiWindow'
import { DEMO, guiReducer, initialGuiState, LOOP_MS } from '../demo'

const renderGui = () => render(<ActiveThemeProvider applyToPage={false}><ClickGuiWindow /></ActiveThemeProvider>)
const row = (name: string) => screen.getByRole('button', { name: new RegExp(`^${name}$`) })

test('the Modules screen shows the four in-game category panels', () => {
  renderGui()
  const panels = within(screen.getByRole('list', { name: 'Категории модулей' })).getAllByRole('listitem', { name: /./ })
  expect(panels).toHaveLength(4)
  for (const title of ['Combat', 'Player', 'Visuals', 'Other']) expect(screen.getByRole('heading', { name: title })).toBeInTheDocument()
  expect(screen.getByRole('tab', { name: 'Modules' })).toHaveAttribute('aria-selected', 'true')
})

test('enabled modules carry a check mark; clicking a row toggles it', async () => {
  const user = userEvent.setup()
  renderGui()
  const predictions = row('Predictions')
  expect(predictions).toHaveAttribute('aria-pressed', 'true')
  expect(predictions.querySelector('.lv-gui-row-check')).not.toBeNull()
  const autoLeave = row('Auto Leave')
  expect(autoLeave.querySelector('.lv-gui-row-check')).toBeNull()
  await user.click(autoLeave)
  expect(autoLeave).toHaveAttribute('aria-pressed', 'true')
  expect(autoLeave.querySelector('.lv-gui-row-check')).not.toBeNull()
})

test('reduced motion shows the finished demo frame', () => {
  renderGui()
  expect(row('AmbientParticles')).toHaveAttribute('aria-pressed', 'true')
  expect(row('DamageTint')).toHaveAttribute('aria-pressed', 'true')
})

test('Settings lists every preset as a theme card, and picking one re-themes the GUI', async () => {
  const user = userEvent.setup()
  renderGui()
  await user.click(screen.getByRole('tab', { name: 'Settings' }))
  expect(screen.getByRole('heading', { name: 'Темы' })).toBeInTheDocument()
  expect(screen.getAllByRole('option')).toHaveLength(PRESETS.length)
  const nitro = PRESETS.find((p) => p.id === 'nitro')!
  await user.click(screen.getByRole('option', { name: 'Тема Nitro' }))
  expect(screen.getByRole('option', { name: 'Тема Nitro' })).toHaveAttribute('aria-selected', 'true')
  expect(screen.getByTestId('clickgui-root').style.getPropertyValue('--gui-accent')).toBe(argbToCss(nitro.theme.accent))
  await user.click(screen.getByRole('tab', { name: 'Modules' }))
  expect(screen.getByRole('heading', { name: 'Combat' })).toBeInTheDocument()
})

test('the scripted demo toggles modules, visits Settings and loops back', () => {
  vi.useFakeTimers()
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: !q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))
  renderGui()
  expect(row('AmbientParticles')).toHaveAttribute('aria-pressed', 'false')
  act(() => vi.advanceTimersByTime(2000))
  expect(row('AmbientParticles')).toHaveAttribute('aria-pressed', 'true')
  act(() => vi.advanceTimersByTime(3000))
  expect(screen.getByRole('tab', { name: 'Settings' })).toHaveAttribute('aria-selected', 'true')
  act(() => vi.advanceTimersByTime(LOOP_MS + 600 - 5000 + 500)) // into the next cycle, before its first click
  expect(screen.getByRole('tab', { name: 'Modules' })).toHaveAttribute('aria-selected', 'true')
  expect(row('AmbientParticles')).toHaveAttribute('aria-pressed', 'false')
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

test('demo timeline fits in the loop and the reducer resets', () => {
  expect(Math.max(...DEMO.map((s) => s.at))).toBeLessThan(LOOP_MS)
  const toggled = guiReducer(initialGuiState(), { type: 'toggle', module: 'Chams' })
  expect(toggled.enabled.Chams).toBe(false)
  const settings = guiReducer(toggled, { type: 'tab', id: 'settings' })
  expect(guiReducer(settings, { type: 'reset' })).toEqual(initialGuiState())
})

test('controlled mode: onPickTheme receives the card and the site theme stays put; backdrop is swappable', async () => {
  const user = userEvent.setup()
  const onPickTheme = vi.fn()
  render(
    <ActiveThemeProvider applyToPage={false}>
      <ClickGuiWindow demo={false} onPickTheme={onPickTheme} backdrop="/shots/f1.png" />
    </ActiveThemeProvider>,
  )
  const root = screen.getByTestId('clickgui-root')
  expect(root.style.getPropertyValue('--gui-backdrop-image')).toBe('url("/shots/f1.png")')
  await user.click(screen.getByRole('tab', { name: 'Settings' }))
  const before = screen.getByRole('option', { selected: true })
  const accent = root.style.getPropertyValue('--gui-accent')
  await user.click(screen.getByRole('option', { name: 'Тема Legacy' }))
  expect(onPickTheme).toHaveBeenCalledWith('legacy')
  expect(screen.getByRole('option', { selected: true })).toBe(before)
  expect(root.style.getPropertyValue('--gui-accent')).toBe(accent)
})
