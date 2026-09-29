import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import { json, mockFetch } from '@test/fetch'
import { ParticleField } from '../effects/ParticleField'
import { HudPlayground } from '../sections/explore/HudPlayground'
import { ModuleWall } from '../sections/explore/ModuleWall'
import { ShowcaseTeaser } from '../sections/explore/ShowcaseTeaser'

const reduceMotion = () =>
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))

function withQuery(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter([{ path: '/', element: ui }])
  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

afterEach(() => vi.unstubAllGlobals())

test('HudWidget moves by 8px with ArrowRight', () => {
  reduceMotion()
  withQuery(<HudPlayground />)
  const widget = screen.getByRole('button', { name: /FPS/ })
  const before = Number.parseInt(widget.style.left, 10)
  fireEvent.keyDown(widget, { key: 'ArrowRight' })
  expect(Number.parseInt(screen.getByRole('button', { name: /FPS/ }).style.left, 10)).toBe(before + 8)
})

test('HudWidget stays inside the playfield bounds', () => {
  reduceMotion()
  withQuery(<HudPlayground />)
  for (let i = 0; i < 200; i++) fireEvent.keyDown(screen.getByRole('button', { name: /FPS/ }), { key: 'ArrowRight' })
  const left = Number.parseInt(screen.getByRole('button', { name: /FPS/ }).style.left, 10)
  expect(left).toBeGreaterThanOrEqual(0)
  expect(left).toBeLessThanOrEqual(680)
})

test('ModuleWall lists all four category headings', () => {
  reduceMotion()
  withQuery(<ModuleWall />)
  for (const name of ['Combat', 'Player', 'Visuals', 'Other']) {
    expect(screen.getByRole('heading', { level: 3, name })).toBeInTheDocument()
  }
})

test('ShowcaseTeaser shows an honest empty state on fetch error', async () => {
  reduceMotion()
  mockFetch({}) // no /showcase route -> 404 -> query error
  withQuery(<ShowcaseTeaser />)
  expect(await screen.findByText('Пока пусто. Будь первым, кто поделится конфигом.')).toBeInTheDocument()
})

test('ShowcaseTeaser shows an honest empty state when there are 0 real items', async () => {
  reduceMotion()
  mockFetch({ 'GET /showcase': () => json({ items: [], next_cursor: null }) })
  withQuery(<ShowcaseTeaser />)
  expect(await screen.findByText('Пока пусто. Будь первым, кто поделится конфигом.')).toBeInTheDocument()
})

test('ParticleField schedules no animation frame under reduced motion', () => {
  const ctx = new Proxy({}, { get: () => () => ({ addColorStop() {} }), set: () => true })
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(ctx as unknown as CanvasRenderingContext2D)
  reduceMotion()
  const raf = vi.spyOn(window, 'requestAnimationFrame').mockImplementation(() => 1)
  render(<ParticleField color="rgb(10, 20, 30)" />)
  expect(raf).not.toHaveBeenCalled()
  ;(HTMLCanvasElement.prototype.getContext as unknown as { mockRestore: () => void }).mockRestore()
})
