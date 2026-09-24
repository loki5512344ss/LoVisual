import { screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import { RequireAuth } from '../RequireAuth'
import { useSession } from '../session'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

const me = { id: '1', email: 'a@b.c', display_nick: 'Rider', role: 'user', avatar_url: null, created_at: '2026-09-24T00:00:00Z' }

function Whoami() {
  const { me } = useSession()
  return <p>Привет, {me?.display_nick}</p>
}

const routes = [
  { path: '/login', element: <p>Страница входа</p> },
  { element: <RequireAuth />, children: [{ path: '/configs', element: <Whoami /> }] },
]

test('restores the session from the refresh cookie', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({ access_token: 't' }), 'GET /me': () => json(me) })
  renderApp(routes, '/configs')
  expect(await screen.findByText('Привет, Rider')).toBeInTheDocument()
})

test('anonymous visitor is sent to login', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({ error: 'unauthorized' }, 401) })
  renderApp(routes, '/configs')
  expect(await screen.findByText('Страница входа')).toBeInTheDocument()
})
