import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { AvatarUpload } from '../components/AvatarUpload'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

beforeEach(() => {
  URL.createObjectURL = vi.fn(() => 'blob:mock')
  URL.revokeObjectURL = vi.fn()
})

afterEach(() => {
  vi.restoreAllMocks()
})

test('picking a file opens the crop step instead of uploading immediately', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'POST /avatars': () => json({ avatar_url: 'x' }),
  })
  renderApp([{ path: '/', element: <AvatarUpload /> }])
  const input = document.querySelector('input[type="file"]') as HTMLInputElement
  const file = new File(['x'], 'a.png', { type: 'image/png' })
  await user.upload(input, file)

  expect(await screen.findByRole('button', { name: 'Подтвердить' })).toBeInTheDocument()
  expect(fetch.mock.calls.some(([url]) => String(url).includes('/avatars'))).toBe(false)
})

test('cancelling the crop step returns to the picker without uploading', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'POST /avatars': () => json({ avatar_url: 'x' }),
  })
  renderApp([{ path: '/', element: <AvatarUpload /> }])
  const input = document.querySelector('input[type="file"]') as HTMLInputElement
  const file = new File(['x'], 'a.png', { type: 'image/png' })
  await user.upload(input, file)

  await user.click(await screen.findByRole('button', { name: 'Отмена' }))

  expect(await screen.findByText('Сменить аватар')).toBeInTheDocument()
  expect(fetch.mock.calls.some(([url]) => String(url).includes('/avatars'))).toBe(false)
})
