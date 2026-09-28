import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { BreakableBlock } from '../BreakableBlock'
import { renderApp } from '@test/render'

// motion's useReducedMotion reads matchMedia, which jsdom lacks.
vi.stubGlobal('matchMedia', (query: string) => ({
  matches: !query.includes('reduce'),
  media: query,
  onchange: null,
  addEventListener() {},
  removeEventListener() {},
  addListener() {},
  removeListener() {},
  dispatchEvent: () => false,
}))

test('a block drops in and cracks over three clicks, then bursts and respawns', async () => {
  const user = userEvent.setup()
  renderApp([{ path: '/', element: <BreakableBlock /> }])

  // The block remounts (new key) on respawn, so re-query it before each click.
  const block = () => screen.getByRole('button', { name: 'Сломать блок' })
  expect(block().querySelector('.lv-block-crack')).toBeNull()

  await user.click(block())
  expect(block().querySelector('.lv-block-crack')).toHaveAttribute('data-stage', '1')
  await user.click(block())
  expect(block().querySelector('.lv-block-crack')).toHaveAttribute('data-stage', '2')

  // Third click shatters: cracks clear (respawn) and debris spawns.
  await user.click(block())
  expect(block().querySelector('.lv-block-crack')).toBeNull()
  expect(document.querySelectorAll('.lv-block-particle').length).toBeGreaterThan(0)
})
