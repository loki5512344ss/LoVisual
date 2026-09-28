import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { copiesLabel } from '../copies'
import { ListingList } from '../components/ListingList'
import { CopyToSlot } from '../components/CopyToSlot'
import { renderApp } from '@test/render'
import { json, mockFetch } from '@test/fetch'

const listing = {
  id: 'l1',
  title: 'Aura / Trails',
  description: 'Мой любимый набор',
  config_name: 'pvp',
  copies_count: 3,
  published_at: '2026-09-20T10:00:00Z',
  author: { nick: 'loki', avatar_url: null },
}

test('russian plural for copies', () => {
  expect(copiesLabel(1)).toBe('Скопировали 1 раз')
  expect(copiesLabel(3)).toBe('Скопировали 3 раза')
  expect(copiesLabel(5)).toBe('Скопировали 5 раз')
  expect(copiesLabel(21)).toBe('Скопировали 21 раз')
})

test('list renders items with author and plural', async () => {
  mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /showcase': () => json({ items: [listing], page: 0, has_more: false }),
  })
  renderApp([{ path: '/', element: <ListingList /> }])
  expect(await screen.findByText('Aura / Trails')).toBeInTheDocument()
  expect(screen.getByText('loki')).toBeInTheDocument()
  expect(screen.getByText('Скопировали 3 раза')).toBeInTheDocument()
})

test('sort toggle updates the query and refetches', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /showcase': () => json({ items: [listing], page: 0, has_more: false }),
  })
  renderApp([{ path: '/', element: <ListingList /> }], '/?sort=new')
  await screen.findByText('Aura / Trails')
  await user.click(screen.getByRole('button', { name: 'Популярные' }))
  expect(fetch.mock.calls.some(([u]) => String(u).includes('sort=popular'))).toBe(true)
})

test('anonymous copy shows the login prompt', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp([{ path: '/', element: <CopyToSlot listingId="l1" /> }])
  expect(await screen.findByText('Войди, чтобы скопировать конфиг к себе')).toBeInTheDocument()
})

test('authenticated copy disables occupied slots and posts the slot', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch({
    'POST /auth/refresh': () => json({ access_token: 't' }),
    'GET /me': () => json({ id: 'u1', email: 'a@b.c', nick: 'loki', avatar_url: null }),
    'GET /configs': () => json([{ slot: 1, name: 'x', updated_at: '2026-09-20T10:00:00Z', share_code: 'A', published: false }]),
    'POST /showcase/l1/copy': () => json({}, 201),
  })
  renderApp([{ path: '/', element: <CopyToSlot listingId="l1" /> }])
  const slot1 = await screen.findByRole('button', { name: 'Слот 1' })
  await waitFor(() => expect(slot1).toBeDisabled())
  const slot2 = screen.getByRole('button', { name: 'Слот 2' })
  await user.click(slot2)
  expect(await screen.findByText('Скопировано в слот 2')).toBeInTheDocument()
  const body = fetch.mock.calls.find(([u]) => String(u).endsWith('/copy'))?.[1]?.body
  expect(JSON.parse(String(body))).toEqual({ slot: 2 })
})

test('409 shows the conflict message', async () => {
  const user = userEvent.setup()
  mockFetch({
    'POST /auth/refresh': () => json({ access_token: 't' }),
    'GET /me': () => json({ id: 'u1', email: 'a@b.c', nick: 'loki', avatar_url: null }),
    'GET /configs': () => json([]),
    'POST /showcase/l1/copy': () => json({ error: 'target slot is not empty' }, 409),
  })
  renderApp([{ path: '/', element: <CopyToSlot listingId="l1" /> }])
  const slot1 = await screen.findByRole('button', { name: 'Слот 1' })
  await user.click(slot1)
  const panel = slot1.closest('div') as HTMLElement
  expect(await within(panel.parentElement as HTMLElement).findByText(/Этот слот уже занят/)).toBeInTheDocument()
})
