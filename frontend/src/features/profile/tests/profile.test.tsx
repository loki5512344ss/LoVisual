import { screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import { json, mockFetch } from '../../../test/fetch'
import { renderApp } from '../../../test/render'
import { PublicProfileView } from '../components/PublicProfileView'

const profile = { id: 'u1', display_nick: 'loki', avatar_url: null, created_at: '2026-01-05T10:00:00Z', badges: ['early'] }

test('shows nick, early badge and the player configs', async () => {
  mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /users/u1': () => json(profile),
    'GET /showcase': () => json({ items: [], page: 0, has_more: false }),
  })
  renderApp([{ path: '/', element: <PublicProfileView id="u1" /> }])
  expect(await screen.findByText('loki')).toBeInTheDocument()
  expect(screen.getByText('Один из первых 1000')).toBeInTheDocument()
})

test('unknown player shows not found', async () => {
  mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /users/nope': () => json({ error: 'no such account' }, 404),
  })
  renderApp([{ path: '/', element: <PublicProfileView id="nope" /> }])
  expect(await screen.findByText('Такого игрока нет.')).toBeInTheDocument()
})
