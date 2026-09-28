import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getI18n } from 'react-i18next'
import { afterEach, expect, test } from 'vitest'
import { routes } from '../../../app/routes'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'
import { PageTitle } from '../PageTitle'

afterEach(async () => {
  await getI18n().changeLanguage('ru')
})

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

test('the nav pill marks the active route with the sliding indicator', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/themes')
  const nav = (await screen.findAllByRole('navigation', { name: 'Основная навигация' }))[0]
  const active = within(nav).getByRole('link', { name: 'Темы' })
  expect(active).toHaveAttribute('aria-current', 'page')
  expect(within(active).getByTestId('nav-indicator')).toBeInTheDocument()
  expect(within(nav).getAllByTestId('nav-indicator')).toHaveLength(1)
  expect(within(nav).getByRole('link', { name: 'Витрина' })).not.toHaveAttribute('aria-current')
})

test('the mobile menu opens a sheet, traps focus and closes with Esc', async () => {
  const user = userEvent.setup()
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/')
  const toggle = await screen.findByRole('button', { name: 'Меню' })
  await user.click(toggle)
  const sheet = document.getElementById(toggle.getAttribute('aria-controls')!)!
  expect(sheet).toBeInTheDocument()
  expect(within(sheet).getByRole('link', { name: 'Витрина' })).toHaveFocus()
  expect(within(sheet).getByRole('group', { name: 'Язык' })).toBeInTheDocument()
  await user.keyboard('{Shift>}{Tab}{Tab}{/Shift}')
  expect(within(sheet).getByRole('link', { name: 'Войти' })).toHaveFocus()
  await user.keyboard('{Escape}')
  expect(toggle).toHaveAttribute('aria-expanded', 'false')
  expect(toggle).toHaveFocus()
})

test('the account slot shows a skeleton while the session restores', async () => {
  let release = () => {}
  mockFetch({ 'POST /auth/refresh': () => new Promise<Response>((r) => (release = () => r(json({}, 401)))) })
  renderApp(routes, '/nowhere')
  expect(await screen.findByRole('status', { name: 'Проверяем вход' })).toBeInTheDocument()
  expect(screen.queryByRole('link', { name: 'Войти' })).not.toBeInTheDocument()
  release()
  expect(await screen.findByRole('link', { name: 'Войти' })).toBeInTheDocument()
  expect(screen.queryByRole('status', { name: 'Проверяем вход' })).not.toBeInTheDocument()
})

test('the header language toggle switches the UI to English and back', async () => {
  const user = userEvent.setup()
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/nowhere')
  await screen.findByRole('link', { name: 'Войти' })
  const group = screen.getAllByRole('group', { name: 'Язык' })[0]
  await user.click(within(group).getByRole('button', { name: 'EN' }))
  expect(await screen.findByRole('link', { name: 'Log in' }, { timeout: 3000 })).toBeInTheDocument()
  const en = screen.getAllByRole('group', { name: 'Language' })[0]
  expect(within(en).getByRole('button', { name: 'EN' })).toHaveAttribute('aria-pressed', 'true')
  await user.click(within(en).getByRole('button', { name: 'RU' }))
  expect(await screen.findByRole('link', { name: 'Войти' }, { timeout: 3000 })).toBeInTheDocument()
})

test('footer carries the github link and the disclaimer', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/')
  expect(await screen.findByText('Не связано с Mojang или Microsoft.')).toBeInTheDocument()
  const gh = screen.getByRole('link', { name: 'GitHub' })
  expect(gh.getAttribute('href')).toMatch(/^https:\/\/github\.com\//)
})

test('page title renders one h1 with an optional lead', () => {
  render(
    <PageTitle lead="Короткое описание">Мои конфиги</PageTitle>,
  )
  expect(screen.getByRole('heading', { level: 1, name: 'Мои конфиги' })).toBeInTheDocument()
  expect(screen.getByText('Короткое описание')).toBeInTheDocument()
})
