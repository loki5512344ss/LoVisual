import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { routes } from '../../../app/routes'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

test('anonymous visitor sees nav and a login link', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/')
  expect(await screen.findByRole('link', { name: 'Войти' })).toHaveAttribute('href', '/login')
  expect(screen.getAllByRole('link', { name: 'Витрина' }).length).toBeGreaterThan(0)
  expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Визуалы, которые видно')
})

test('signed-in visitor sees their nick instead of login', async () => {
  mockFetch({
    'POST /auth/refresh': () => json({ access_token: 't' }),
    'GET /me': () => json({ id: '1', email: 'a@b.c', display_nick: 'Rider', role: 'user', avatar_url: null, created_at: '' }),
  })
  renderApp(routes, '/')
  expect(await screen.findByRole('link', { name: /Rider/ })).toHaveAttribute('href', '/account')
  expect(screen.queryByRole('link', { name: 'Войти' })).not.toBeInTheDocument()
})

test('unknown path shows not found', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/definitely-not-here')
  expect(await screen.findByRole('heading', { name: 'Такой страницы нет' })).toBeInTheDocument()
})

test('menu disclosure toggles aria-expanded', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/')
  const toggle = await screen.findByRole('button', { name: 'Меню' })
  expect(toggle).toHaveAttribute('aria-expanded', 'false')
  await userEvent.click(toggle)
  expect(toggle).toHaveAttribute('aria-expanded', 'true')
})

test('footer carries the github link and the disclaimer', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/')
  expect(await screen.findByText('Не связано с Mojang или Microsoft.')).toBeInTheDocument()
  const gh = screen.getByRole('link', { name: 'GitHub' })
  expect(gh.getAttribute('href')).toMatch(/^https:\/\/github\.com\//)
})
