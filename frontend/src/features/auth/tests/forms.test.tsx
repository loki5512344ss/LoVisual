import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { LoginForm } from '../forms/LoginForm'
import { RegisterForm } from '../forms/RegisterForm'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

const me = { id: '1', email: 'a@b.c', display_nick: 'Rider', role: 'user', avatar_url: null, created_at: '2026-09-24T00:00:00Z' }
const anon = { 'POST /auth/refresh': () => json({ error: 'unauthorized' }, 401) }

test('wrong password shows a clear error', async () => {
  const user = userEvent.setup()
  mockFetch({ ...anon, 'POST /auth/login': () => json({ error: 'unauthorized' }, 401) })
  renderApp([{ path: '/login', element: <LoginForm /> }], '/login')
  await user.type(await screen.findByLabelText('Почта'), 'a@b.c')
  await user.type(screen.getByLabelText('Пароль'), 'wrong-password')
  await user.click(screen.getByRole('button', { name: 'Войти' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Неверная почта или пароль.')
})

test('successful login goes to configs', async () => {
  const user = userEvent.setup()
  mockFetch({ ...anon, 'POST /auth/login': () => json({ access_token: 't' }), 'GET /me': () => json(me) })
  renderApp([{ path: '/login', element: <LoginForm /> }, { path: '/configs', element: <p>Мои конфиги</p> }], '/login')
  await user.type(await screen.findByLabelText('Почта'), 'a@b.c')
  await user.type(screen.getByLabelText('Пароль'), 'correct-horse')
  await user.click(screen.getByRole('button', { name: 'Войти' }))
  expect(await screen.findByText('Мои конфиги')).toBeInTheDocument()
})

test('register validates before calling the server', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch(anon)
  renderApp([{ path: '/register', element: <RegisterForm /> }], '/register')
  await user.type(await screen.findByLabelText('Почта'), 'not-an-email')
  await user.type(screen.getByLabelText('Пароль'), 'short')
  await user.click(screen.getByRole('button', { name: 'Создать аккаунт' }))
  expect(screen.getByLabelText('Почта')).toHaveAttribute('aria-invalid', 'true')
  expect(screen.getByLabelText('Пароль')).toHaveAccessibleDescription('Минимум 8 символов.')
  expect(fetch.mock.calls.some(([u]) => String(u).includes('/auth/register'))).toBe(false)
})

test('taken email explains what to do', async () => {
  const user = userEvent.setup()
  mockFetch({ ...anon, 'POST /auth/register': () => json({ error: 'email already registered' }, 409) })
  renderApp([{ path: '/register', element: <RegisterForm /> }], '/register')
  await user.type(await screen.findByLabelText('Почта'), 'a@b.c')
  await user.type(screen.getByLabelText('Ник на сайте'), 'Rider')
  await user.type(screen.getByLabelText('Пароль'), 'correct-horse')
  await user.click(screen.getByRole('button', { name: 'Создать аккаунт' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Эта почта уже занята')
})
