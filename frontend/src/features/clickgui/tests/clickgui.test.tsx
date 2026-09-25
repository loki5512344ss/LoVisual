import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { ActiveThemeProvider } from '../../themes/useActiveTheme'
import { ClickGuiWindow } from '../ClickGuiWindow'
import { DEMO, guiReducer, initialGuiState, LOOP_MS } from '../demo'

const renderGui = () => render(<ActiveThemeProvider applyToPage={false}><ClickGuiWindow /></ActiveThemeProvider>)

test('reduced motion shows the finished demo frame with no cursor', () => {
  renderGui()
  expect(screen.getByRole('button', { name: 'Visuals' })).toHaveAttribute('aria-pressed', 'true')
  expect(screen.getByRole('switch', { name: 'Включить Trails' })).toHaveAttribute('aria-checked', 'true')
  expect(screen.getByRole('slider', { name: 'Length' })).toHaveValue('85')
})

test('cards are interactive: switch categories, toggle and expand', async () => {
  const user = userEvent.setup()
  renderGui()
  await user.click(screen.getByRole('button', { name: 'Misc' }))
  const toggle = screen.getByRole('switch', { name: 'Включить StreamerMode' })
  expect(toggle).toHaveAttribute('aria-checked', 'false')
  await user.click(toggle)
  expect(toggle).toHaveAttribute('aria-checked', 'true')
  await user.click(screen.getByRole('button', { name: 'Настройки StreamerMode' }))
  const card = toggle.closest('article') as HTMLElement
  expect(within(card).getByRole('checkbox', { name: /Hide IP/ })).toHaveAttribute('aria-checked', 'true')
})

test('the scripted demo toggles Trails and loops back to the start', () => {
  vi.useFakeTimers()
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: !q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))
  renderGui()
  expect(screen.getByRole('button', { name: 'Combat' })).toHaveAttribute('aria-pressed', 'true')
  act(() => vi.advanceTimersByTime(3200))
  expect(screen.getByRole('switch', { name: 'Включить Trails' })).toHaveAttribute('aria-checked', 'true')
  act(() => vi.advanceTimersByTime(LOOP_MS))
  expect(screen.getByRole('button', { name: 'Combat' })).toHaveAttribute('aria-pressed', 'true')
  vi.useRealTimers()
})

test('demo timeline fits in the loop and the reducer resets', () => {
  expect(Math.max(...DEMO.map((s) => s.at))).toBeLessThan(LOOP_MS)
  const toggled = guiReducer(initialGuiState(), { type: 'toggle', module: 'Trails' })
  expect(toggled.enabled.Trails).toBe(true)
  expect(guiReducer(toggled, { type: 'reset' }).enabled.Trails).toBe(false)
})
