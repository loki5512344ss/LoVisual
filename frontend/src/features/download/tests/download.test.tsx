import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, expect, test, vi } from 'vitest'
import { DownloadButton } from '../DownloadButton'
import { ReleaseNotes } from '../ReleaseNotes'
import DownloadPage from '../../../pages/download/DownloadPage'

function withQuery(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
}

const release = (body: string) => ({
  tag_name: 'v0.1.1',
  name: 'v0.1.1',
  published_at: '2026-09-01T00:00:00Z',
  body,
  html_url: 'https://git.wihuk.pro/boba/LoVisual/releases/tag/v0.1.1',
  assets: [{ name: 'lovisual.jar', size: 4_400_000, download_count: 12 }],
})

afterEach(() => vi.restoreAllMocks())

test('DownloadButton links straight to the latest lovisual.jar on our own backend', () => {
  const { container } = withQuery(<DownloadButton />)
  const link = container.querySelector('a')
  expect(link?.getAttribute('href')).toBe('/api/downloads/lovisual.jar')
  expect(link?.hasAttribute('download')).toBe(true)
})

test('the page shows the jar as an item card and links to releases and source on our Forgejo', async () => {
  vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('offline'))
  withQuery(<DownloadPage />)

  const direct = await screen.findByRole('link', { name: 'Скачать бесплатно' })
  expect(direct).toHaveAttribute('href', '/api/downloads/lovisual.jar')
  expect(direct).toHaveAttribute('download')

  expect(screen.getByRole('link', { name: 'Все релизы' })).toHaveAttribute(
    'href',
    'https://git.wihuk.pro/boba/LoVisual/releases',
  )
  expect(screen.getByRole('link', { name: 'Исходный код' })).toHaveAttribute(
    'href',
    'https://git.wihuk.pro/boba/LoVisual',
  )
  // GitHub is gone from the page entirely.
  expect(document.body.innerHTML).not.toContain('github.com')
})

test('the release card shows the SHA-256 of the served jar and can copy it', async () => {
  const hash = 'a'.repeat(63) + 'b'
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = String(input)
    if (url.endsWith('.sha256')) return { ok: true, status: 200, text: async () => `${hash}  lovisual.jar\n` } as unknown as Response
    return { ok: true, status: 200, json: async () => [release('x')] } as unknown as Response
  })
  const write = vi.fn().mockResolvedValue(undefined)
  Object.assign(navigator, { clipboard: { writeText: write } })

  withQuery(<DownloadPage />)
  expect(await screen.findByText(hash)).toBeInTheDocument()
  expect(await screen.findByText(/LoVisual 0\.1\.1/)).toBeInTheDocument()
  screen.getByRole('button', { name: 'Скопировать SHA-256' }).click()
  await waitFor(() => expect(write).toHaveBeenCalledWith(hash))
})

test('ReleaseNotes renders Markdown but never raw HTML from a release body', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => [release('# Заголовок\n\n<script>alert(1)</script>\n\n<img src=x onerror=alert(1)>')],
  } as unknown as Response)

  const { container } = withQuery(<ReleaseNotes />)
  expect(await screen.findByRole('heading', { level: 1, name: 'Заголовок' })).toBeInTheDocument()
  expect(container.querySelector('script')).toBeNull()
  expect(container.querySelector('img')).toBeNull()
})

test('ReleaseNotes shows the fallback when the releases fetch fails', async () => {
  vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'))
  withQuery(<ReleaseNotes />)
  expect(await screen.findByText(/первый публичный релиз/)).toBeInTheDocument()
})
