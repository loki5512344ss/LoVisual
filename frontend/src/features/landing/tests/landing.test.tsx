import { render, screen } from '@testing-library/react'
import { expect, test, vi } from 'vitest'
import { I18nProvider } from '@shared/i18n/I18nProvider'
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
  expect(await screen.findByText('%config load ••••••••')).toBeInTheDocument()
  expect(screen.getByText('[LoVisual] Конфиг «pvp» загружен')).toBeInTheDocument()
  expect(screen.getByLabelText('Пример: загрузка конфига по коду в чате игры')).toBeInTheDocument()
})

test('english locale renders the landing hero headline', async () => {
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  const i18n = initI18n('en')
  const router = createMemoryRouter([{ path: '/', element: <Hero /> }])
  render(
    <I18nProvider i18n={i18n}>
      <QueryClientProvider client={new QueryClient()}>
        <SessionProvider>
          <RouterProvider router={router} />
        </SessionProvider>
      </QueryClientProvider>
    </I18nProvider>,
  )
  expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent("you'll fall in love with")
})

test('cloud section speaks in player terms in both languages', async () => {
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  for (const [lng, title] of [
    ['ru', 'Увидел у друга крутой конфиг? Одна команда в чат.'],
    ['en', 'Your friend has a sick config? One command in chat.'],
  ] as const) {
    const router = createMemoryRouter([{ path: '/', element: <CommandHero /> }])
    const { unmount } = render(
      <I18nProvider i18n={initI18n(lng)}>
        <QueryClientProvider client={new QueryClient()}>
          <SessionProvider><RouterProvider router={router} /></SessionProvider>
        </QueryClientProvider>
      </I18nProvider>,
    )
    expect(await screen.findByRole('heading', { level: 2 })).toHaveTextContent(title)
    unmount()
  }
})

test('hero download CTA goes to the page with both download buttons', async () => {
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  const router = createMemoryRouter([{ path: '/', element: <Hero /> }])
  // Explicit ru: an earlier test in this file re-inits the default instance in English.
  render(
    <I18nProvider i18n={initI18n('ru')}>
      <QueryClientProvider client={new QueryClient()}>
        <SessionProvider><RouterProvider router={router} /></SessionProvider>
      </QueryClientProvider>
    </I18nProvider>,
  )
  const cta = await screen.findByRole('link', { name: 'Скачать бесплатно' })
  expect(cta).toHaveAttribute('href', '/download')
  expect(cta).not.toHaveAttribute('download')
})
