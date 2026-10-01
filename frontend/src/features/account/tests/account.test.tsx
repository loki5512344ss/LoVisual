import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getI18n } from '@shared/i18n/react'
import { expect, test } from 'vitest'
import { checkAvatarFile } from '../api'
import { DeviceList } from '@features/account/components/DeviceList'
import { renderApp } from '@test/render'
import { json, mockFetch } from '@test/fetch'

test('avatar file checks', () => {
  const t = getI18n().getFixedT('ru', 'account')
  expect(checkAvatarFile(new File(['x'], 'a.gif', { type: 'image/gif' }), t)).toBe('Подойдёт PNG, JPG или WebP.')
  const big = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'a.png', { type: 'image/png' })
  expect(checkAvatarFile(big, t)).toBe('Файл больше 5 МБ.')
  expect(checkAvatarFile(new File(['x'], 'a.png', { type: 'image/png' }), t)).toBeUndefined()
})

test('revoking a device asks for confirmation first', async () => {
  const user = userEvent.setup()
  let devices = [{ id: 'd1', linked_at: '2026-09-20T10:00:00Z', last_seen: null }]
  const fetch = mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /device/links': () => json(devices),
    'DELETE /device/links/d1': () => {
      devices = []
      return new Response(null, { status: 204 })
    },
  })
  renderApp([{ path: '/', element: <DeviceList /> }])
  const row = await screen.findByRole('listitem')
  await user.click(within(row).getByRole('button', { name: 'Отвязать' }))
  expect(fetch.mock.calls.some(([, i]) => i?.method === 'DELETE')).toBe(false)
  await user.click(within(row).getByRole('button', { name: 'Точно отвязать?' }))
  expect(await screen.findByText(/Пока ни одной игры/)).toBeInTheDocument()
})
