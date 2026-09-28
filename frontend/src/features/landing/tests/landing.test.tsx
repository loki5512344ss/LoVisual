import { render, screen } from '@testing-library/react'
import { expect, test, vi } from 'vitest'
import { I18nextProvider } from 'react-i18next'
import { CommandHero } from '../CommandHero'
import { Hero } from '../sections/hero/Hero'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { initI18n } from '@app/i18n'
import { SessionProvider } from '@features/auth/session'
import { json, mockFetch } from '@test/fetch'

test('reduced motion shows the full command at once', async () => {
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  const router = createMemoryRouter([{ path: '/', element: <CommandHero /> }])
  render(
    <QueryClientProvider client={new QueryClient()}>
      <SessionProvider><RouterProvider router={router} /></SessionProvider>
    </QueryClientProvider>,
  )
  expect(await screen.findByText('%config load 7KQ3M9XA')).toBeInTheDocument()
  expect(screen.getByText('[LoVisual] Конфиг «pvp» загружен')).toBeInTheDocument()
  expect(screen.getByLabelText('Пример: загрузка конфига по коду в чате игры')).toBeInTheDocument()
})

test('english locale renders the landing hero headline', async () => {
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  const i18n = initI18n('en')
  const router = createMemoryRouter([{ path: '/', element: <Hero /> }])
  render(
    <I18nextProvider i18n={i18n}>
      <QueryClientProvider client={new QueryClient()}>
        <SessionProvider>
          <RouterProvider router={router} />
        </SessionProvider>
      </QueryClientProvider>
    </I18nextProvider>,
  )
  expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent("you'll fall in love with")
})
