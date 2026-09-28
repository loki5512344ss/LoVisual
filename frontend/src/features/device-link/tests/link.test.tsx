import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { LinkDeviceForm } from '../LinkDeviceForm'
import { normalizeUserCode } from '../api'
import { renderApp } from '@test/render'
import { json, mockFetch } from '@test/fetch'

test('normalizes typed codes', () => {
  expect(normalizeUserCode(' abcd1234 ')).toBe('ABCD-1234')
  expect(normalizeUserCode('abcd-1234')).toBe('ABCD-1234')
})

test('confirms a code and shows success', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'POST /device/confirm': () => new Response(null, { status: 200 }),
  })
  renderApp([{ path: '/', element: <LinkDeviceForm /> }])
  await user.type(await screen.findByLabelText('Код из игры'), 'abcd1234')
  await user.click(screen.getByRole('button', { name: 'Привязать' }))
  expect(await screen.findByText(/Игра привязана/)).toBeInTheDocument()
  const body = fetch.mock.calls.find(([u]) => String(u).endsWith('/device/confirm'))?.[1]?.body
  expect(body).toBe('{"user_code":"ABCD-1234"}')
})

test('expired code explains what to do', async () => {
  const user = userEvent.setup()
  mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'POST /device/confirm': () => json({ error: 'unknown or expired user_code' }, 404),
  })
  renderApp([{ path: '/', element: <LinkDeviceForm /> }])
  await user.type(await screen.findByLabelText('Код из игры'), 'ZZZZ-ZZZZ')
  await user.click(screen.getByRole('button', { name: 'Привязать' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Коды живут 10 минут')
})
