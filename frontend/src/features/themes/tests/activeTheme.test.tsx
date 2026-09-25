import { act, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { ActiveThemeProvider, useActiveTheme } from '../useActiveTheme'

function Probe() {
  const { theme, setTheme, userPicked } = useActiveTheme()
  return (
    <>
      <p data-testid="id">{theme.id}</p>
      <p data-testid="picked">{String(userPicked)}</p>
      <button onClick={() => setTheme('nitro', 'user')}>user</button>
      <button onClick={() => setTheme('sunset', 'demo')}>demo</button>
    </>
  )
}

afterEach(() => {
  window.localStorage.clear()
  document.documentElement.removeAttribute('style')
})

test('a user pick persists to lv_theme, re-accents the page and locks out demo switches', () => {
  render(<ActiveThemeProvider><Probe /></ActiveThemeProvider>)
  expect(screen.getByTestId('id')).toHaveTextContent('classic')
  act(() => screen.getByText('demo').click())
  expect(screen.getByTestId('id')).toHaveTextContent('sunset')
  expect(window.localStorage.getItem('lv_theme')).toBeNull()
  act(() => screen.getByText('user').click())
  expect(screen.getByTestId('id')).toHaveTextContent('nitro')
  expect(window.localStorage.getItem('lv_theme')).toBe('nitro')
  expect(document.documentElement.style.getPropertyValue('--color-ice')).toBe('rgb(122, 92, 255)')
  act(() => screen.getByText('demo').click())
  expect(screen.getByTestId('id')).toHaveTextContent('nitro')
})

test('restores the stored theme and survives blocked storage', () => {
  window.localStorage.setItem('lv_theme', 'mint')
  const { unmount } = render(<ActiveThemeProvider><Probe /></ActiveThemeProvider>)
  expect(screen.getByTestId('id')).toHaveTextContent('mint')
  expect(screen.getByTestId('picked')).toHaveTextContent('true')
  unmount()

  vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
    throw new Error('blocked')
  })
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new Error('blocked')
  })
  render(<ActiveThemeProvider><Probe /></ActiveThemeProvider>)
  expect(screen.getByTestId('id')).toHaveTextContent('classic')
  act(() => screen.getByText('user').click())
  expect(screen.getByTestId('id')).toHaveTextContent('nitro')
  vi.restoreAllMocks()
})
