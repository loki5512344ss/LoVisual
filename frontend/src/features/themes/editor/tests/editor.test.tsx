import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { PRESETS } from '../../presets.generated'
import { ActiveThemeProvider, useActiveTheme } from '../../useActiveTheme'
import { ThemeEditor } from '../ThemeEditor'

test('editor shows the initial preset name and one toggle per gradient slot', () => {
  render(<ThemeEditor initial={PRESETS[0]} />)
  const name = screen.getByDisplayValue('Classic')
  expect(name).toBeInTheDocument()
  expect(screen.getAllByRole('checkbox', { name: /Градиент/ })).toHaveLength(5)
})

test('picking a preset chip loads it into the editor', () => {
  const next = PRESETS[1]
  render(<ThemeEditor initial={PRESETS[0]} />)
  fireEvent.click(screen.getByRole('button', { name: next.name }))
  expect(screen.getByDisplayValue(next.name)).toBeInTheDocument()
})

function SiteTheme() {
  return <output aria-label="site theme">{useActiveTheme().theme.id}</output>
}

test('a theme card inside the preview loads that preset into the draft, not the site', async () => {
  const user = userEvent.setup()
  render(
    <ActiveThemeProvider applyToPage={false}>
      <SiteTheme />
      <ThemeEditor initial={PRESETS[0]} />
    </ActiveThemeProvider>,
  )
  const gui = screen.getByTestId('clickgui-root')
  await user.click(within(gui).getByRole('tab', { name: 'Settings' }))
  await user.click(within(gui).getByRole('option', { name: 'Тема Nitro' }))
  expect(screen.getByDisplayValue('Nitro')).toBeInTheDocument()
  expect(within(gui).getByRole('option', { name: 'Тема Nitro' })).toHaveAttribute('aria-selected', 'true')
  // The site keeps its default theme (amber) even though the draft loaded Classic.
  expect(screen.getByRole('status', { name: 'site theme' })).toHaveTextContent('amber')
  expect(window.localStorage.getItem('lv_theme')).toBeNull()
})
