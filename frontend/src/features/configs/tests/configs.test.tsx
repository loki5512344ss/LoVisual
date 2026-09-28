import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { SlotGrid } from '../components/SlotGrid'
import { renderApp } from '@test/render'
import { json, mockFetch } from '@test/fetch'

const filled = { slot: 2, name: 'pvp', updated_at: '2026-09-24T10:00:00Z', share_code: '7KQ3M9XA', published: false }

test('always shows four slots', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401), 'GET /configs': () => json([filled]) })
  renderApp([{ path: '/', element: <SlotGrid /> }])
  expect(await screen.findByText('7KQ3M9XA')).toBeInTheDocument()
  expect(screen.getAllByRole('article')).toHaveLength(4)
  expect(screen.getAllByText('Пусто')).toHaveLength(3)
})

test('regenerating a code needs confirmation', async () => {
  const user = userEvent.setup()
  let code = '7KQ3M9XA'
  mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /configs': () => json([{ ...filled, share_code: code }]),
    'POST /configs/2/regenerate-code': () => {
      code = 'NEWC0DE2'
      return json({ share_code: code })
    },
  })
  renderApp([{ path: '/', element: <SlotGrid /> }])
  const panel = (await screen.findByText('pvp')).closest('article') as HTMLElement
  await user.click(within(panel).getByRole('button', { name: 'Новый код' }))
  expect(within(panel).getByText(/Старый код перестанет работать/)).toBeInTheDocument()
  await user.click(within(panel).getByRole('button', { name: 'Сменить' }))
  expect(await within(panel).findByText('NEWC0DE2')).toBeInTheDocument()
})

test('publish requires a title and sends the form', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /configs': () => json([filled]),
    'POST /configs/2/publish': () => json({ listing_id: 'l1' }),
  })
  renderApp([{ path: '/', element: <SlotGrid /> }])
  const panel = (await screen.findByText('pvp')).closest('article') as HTMLElement
  await user.click(within(panel).getByRole('button', { name: 'Опубликовать' }))
  await user.click(within(panel).getByRole('button', { name: 'Опубликовать' }))
  expect(within(panel).getByLabelText('Название на витрине')).toHaveAttribute('aria-invalid', 'true')
  await user.type(within(panel).getByLabelText('Название на витрине'), 'Лучший PvP')
  await user.click(within(panel).getByRole('button', { name: 'Опубликовать' }))
  expect(await within(panel).findByText(/Опубликовано/)).toBeInTheDocument()
  const body = fetch.mock.calls.find(([u]) => String(u).endsWith('/publish'))?.[1]?.body
  expect(JSON.parse(String(body))).toEqual({ title: 'Лучший PvP', description: '' })
})

test('published slot offers to remove from showcase', async () => {
  mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /configs': () => json([{ ...filled, published: true }]),
  })
  renderApp([{ path: '/', element: <SlotGrid /> }])
  const panel = (await screen.findByText('pvp')).closest('article') as HTMLElement
  expect(within(panel).getByRole('button', { name: 'Убрать с витрины' })).toBeInTheDocument()
})
