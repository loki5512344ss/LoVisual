import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { routes } from '@app/routes'
import { renderApp } from '@test/render'
import { json, mockFetch } from '@test/fetch'

const meVerified = {
  id: '1',
  email: 'a@b.c',
  display_nick: 'Rider',
  role: 'user' as const,
  avatar_url: null,
  email_verified: true,
  created_at: '2026-09-24T00:00:00Z',
}
const meUnverified = { ...meVerified, email_verified: false }

const anon = { 'POST /auth/refresh': () => json({ error: 'unauthorized' }, 401) }

test('unverified account sees the confirm banner and can resend the email', async () => {
  localStorage.setItem('lv_session', '1')
  const user = userEvent.setup()
  const fetchFn = mockFetch({
    'POST /auth/refresh': () => json({ access_token: 't' }),
    'GET /me': () => json(meUnverified),
    'POST /auth/resend-verification': () => json({ status: 'verification email sent' }, 202),
  })
  renderApp(routes, '/account')
  expect(await screen.findByRole('status')).toHaveTextContent('Подтверди почту')
  expect(screen.getByRole('status')).toHaveTextContent('a@b.c')

  await user.click(screen.getByRole('button', { name: 'Выслать письмо снова' }))
  expect(await screen.findByText('Письмо отправлено. Загляни в спам, если его нет.')).toBeInTheDocument()
  // The resend call must carry the bearer so the gateway can resolve the account.
  const resendCall = fetchFn.mock.calls.find(
    ([input, init]) => String(input).endsWith('/auth/resend-verification') && (init as RequestInit).method === 'POST',
  )
  expect(resendCall).toBeDefined()
  const headers = (resendCall![1] as RequestInit).headers as Headers
  expect(headers.get('authorization')).toBe('Bearer t')
})

test('verified account sees no confirm banner', async () => {
  localStorage.setItem('lv_session', '1')
  mockFetch({
    'POST /auth/refresh': () => json({ access_token: 't' }),
    'GET /me': () => json(meVerified),
  })
  renderApp(routes, '/account')
  // The nick renders on the account page (and in the top bar): the query
  // answered means the banner logic had its chance to show or stay hidden.
  await screen.findAllByText('Rider')
  expect(screen.queryByRole('status')).not.toBeInTheDocument()
})

test('the email link page verifies the token', async () => {
  mockFetch({ 'POST /auth/verify-email': () => json({ status: 'email verified' }) })
  renderApp(routes, '/verify?token=lvev_ok')
  expect(await screen.findByText('Почта подтверждена. Мод теперь можно привязать на вкладке «Аккаунт».')).toBeInTheDocument()
})

test('a missing token answers with the invalid-link message', async () => {
  mockFetch(anon)
  renderApp(routes, '/verify')
  expect(await screen.findByText('Ссылка недействительна или устарела.')).toBeInTheDocument()
})

test('forgot-password answers the same for any email', async () => {
  const user = userEvent.setup()
  mockFetch({
    ...anon,
    'POST /auth/forgot-password': () => json({ status: 'reset email sent if the account exists' }, 202),
  })
  renderApp(routes, '/forgot-password')
  await user.type(await screen.findByLabelText('Почта'), 'who-knows@example.com')
  await user.click(screen.getByRole('button', { name: 'Отправить письмо' }))
  expect(
    await screen.findByText('Если эта почта зарегистрирована, письмо со сбросом уже летит.'),
  ).toBeInTheDocument()
})
