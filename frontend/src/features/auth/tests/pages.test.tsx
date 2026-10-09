import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { routes } from '@app/routes'
import { renderApp } from '@test/render'
import { json, mockFetch } from '@test/fetch'

const me = { id: '1', email: 'a@b.c', display_nick: 'Rider', role: 'user', avatar_url: null, email_verified: true, created_at: '2026-09-24T00:00:00Z' }
const anon = { 'POST /auth/refresh': () => json({ error: 'unauthorized' }, 401) }

test('the tab bar switches between login and register', async () => {
  const user = userEvent.setup()
  mockFetch(anon)
  renderApp(routes, '/login')
  expect(await screen.findByRole('heading', { level: 1, name: 'С возвращением' })).toBeInTheDocument()
  const tabs = screen.getByRole('navigation', { name: 'Вход или регистрация' })
  expect(within(tabs).getByRole('link', { name: 'Вход' })).toHaveAttribute('aria-current', 'page')

  await user.click(within(tabs).getByRole('link', { name: 'Регистрация' }))
  expect(await screen.findByRole('heading', { level: 1, name: 'Новый аккаунт' })).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Регистрация' })).toHaveAttribute('aria-current', 'page')
  expect(screen.getByRole('list', { name: 'Подсказки' })).toHaveTextContent('4 облачных слота')
})

test('a redirect from /link says what comes next and survives the tab switch', async () => {
  const user = userEvent.setup()
  mockFetch(anon)
  renderApp(routes, '/link')
  expect(await screen.findByText('Дальше: подтвердишь код из игры.')).toBeInTheDocument()
  await user.click(screen.getByRole('link', { name: 'Регистрация' }))
  expect(await screen.findByRole('heading', { level: 1, name: 'Новый аккаунт' })).toBeInTheDocument()
  expect(screen.getByText('Дальше: подтвердишь код из игры.')).toBeInTheDocument()
})

test('registering after a redirect lands back where the visitor was going', async () => {
  const user = userEvent.setup()
  mockFetch({
    ...anon,
    'POST /auth/register': () => json({}, 201),
    'POST /auth/login': () => json({ access_token: 't' }),
    'GET /me': () => json(me),
  })
  const { router } = renderApp(routes, '/link')
  await user.click(await screen.findByRole('link', { name: 'Регистрация' }))
  // /login and /register are lazy-loaded and share the "Почта" email label, so the
  // previous (login) page can still be mounted for a tick while the register chunk
  // resolves. Wait for a register-only marker before querying its email field, or a
  // stale reference to the login field (about to unmount) can end up typed into instead.
  await screen.findByLabelText('Ник на сайте')
  await user.type(screen.getByLabelText('Почта'), 'a@b.c')
  await user.type(screen.getByLabelText('Ник на сайте'), 'Rider')
  await user.type(screen.getByLabelText('Пароль'), 'correct-horse')
  await user.click(screen.getByRole('button', { name: 'Создать аккаунт' }))
  await screen.findByLabelText('Код из игры')
  expect(router.state.location.pathname).toBe('/link')
})
