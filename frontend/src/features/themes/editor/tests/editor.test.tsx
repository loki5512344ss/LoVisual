import { fireEvent, render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import { PRESETS } from '../../presets.generated'
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
