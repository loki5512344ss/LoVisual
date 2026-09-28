import { fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { AvatarUpload } from '@features/account/components/avatar/AvatarUpload'
import { ProfileCard } from '@features/account/components/ProfileCard'
import { renderApp } from '@test/render'
import { json, mockFetch } from '@test/fetch'

const me = (avatar_url: string | null) => ({
  id: '1',
  email: 'a@b.c',
  display_nick: 'Rider',
  role: 'user',
  avatar_url,
  created_at: '2026-01-01T00:00:00Z',
})

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

test('the reposition button is hidden when no avatar is set', async () => {
  mockFetch({
    'POST /auth/refresh': () => json({ access_token: 't' }),
    'GET /me': () => json(me(null)),
  })
  renderApp([{ path: '/', element: <AvatarUpload /> }])
  expect(await screen.findByText('Сменить аватар')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Перекадрировать' })).not.toBeInTheDocument()
})

test('the reposition button shows when an avatar is set', async () => {
  mockFetch({
    'POST /auth/refresh': () => json({ access_token: 't' }),
    'GET /me': () => json(me('https://cdn/avatar.png')),
  })
  renderApp([{ path: '/', element: <AvatarUpload /> }])
  expect(await screen.findByRole('button', { name: 'Перекадрировать' })).toBeInTheDocument()
})

test('dropping a file on the avatar circle opens the crop step', async () => {
  mockFetch({
    'POST /auth/refresh': () => json({ access_token: 't' }),
    'GET /me': () => json(me(null)),
  })
  renderApp([{ path: '/', element: <ProfileCard /> }])
  const target = await screen.findByTestId('avatar-drop')
  fireEvent.drop(target, { dataTransfer: { files: [new File(['x'], 'a.png', { type: 'image/png' })] } })

  expect(await screen.findByRole('button', { name: 'Подтвердить' })).toBeInTheDocument()
})
