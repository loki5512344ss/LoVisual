import { render, screen } from '@testing-library/react'
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
  html_url: 'https://github.com/x/y/releases/tag/v0.1.1',
  assets: [{ name: 'lovisual.jar', size: 4_400_000, download_count: 12 }],
})

afterEach(() => vi.restoreAllMocks())

test('DownloadButton links straight to the latest lovisual.jar on our own backend', () => {
  const { container } = withQuery(<DownloadButton />)
  const link = container.querySelector('a')
  expect(link?.getAttribute('href')).toBe('/api/downloads/lovisual.jar')
  expect(link?.hasAttribute('download')).toBe(true)
})

test('the page offers both paths: our jar and a GitHub button locked behind a ribbon', async () => {
  vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('offline'))
  withQuery(<DownloadPage />)

  const direct = await screen.findByRole('link', { name: 'Скачать бесплатно' })
  expect(direct).toHaveAttribute('href', '/api/downloads/lovisual.jar')
  expect(direct).toHaveAttribute('download')

  // The GitHub path is visible but not a link, and says why.
  const gh = screen.getByText('Код на GitHub')
  expect(gh.closest('a')).toBeNull()
  expect(gh).toHaveAttribute('aria-disabled', 'true')
  expect(screen.getByText('временно не работает')).toBeInTheDocument()
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
