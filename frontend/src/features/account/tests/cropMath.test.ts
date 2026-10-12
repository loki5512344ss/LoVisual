import { expect, test } from 'vitest'
import {
  MAX_ZOOM,
  MIN_ZOOM,
  clampPan,
  clampZoom,
  coverScale,
  pinchZoom,
  sourceRect,
  wheelZoom,
} from '@features/account/components/avatar/cropMath'

test('coverScale scales the shorter side to fill the viewport', () => {
  expect(coverScale(1000, 2000, 200)).toBeCloseTo(0.2)
  expect(coverScale(2000, 1000, 200)).toBeCloseTo(0.2)
})

test('clampZoom keeps zoom within [1, 4]', () => {
  expect(clampZoom(0.5)).toBe(1)
  expect(clampZoom(10)).toBe(4)
  expect(clampZoom(2)).toBe(2)
})

test('clampPan keeps a square image centered at zoom 1 (no room to pan)', () => {
  const clamped = clampPan({ x: 50, y: 50 }, 400, 400, 200, 1)
  expect(clamped).toEqual({ x: 0, y: 0 })
})

test('clampPan allows panning up to the overflow at higher zoom', () => {
  // 400x400 image, viewport 200, zoom 2 -> scale = 0.5*2 = 1, scaledSize 400, overflow (400-200)/2=100
  const clamped = clampPan({ x: 1000, y: -1000 }, 400, 400, 200, 2)
  expect(clamped).toEqual({ x: 100, y: -100 })
})

test('clampPan never lets the image show empty space (overflow stays >= 0)', () => {
  const clamped = clampPan({ x: 5, y: 5 }, 300, 900, 240, 1)
  // shorter side (300) covers the viewport exactly at zoom 1, so no pan room on x
  expect(clamped.x).toBe(0)
  // long side has plenty of room
  expect(Math.abs(clamped.y)).toBeLessThanOrEqual(5)
})

test('sourceRect at zoom 1 with no offset centers the full covering square', () => {
  const rect = sourceRect(400, 400, 200, 1, { x: 0, y: 0 })
  expect(rect.sx).toBeCloseTo(0)
  expect(rect.sy).toBeCloseTo(0)
  expect(rect.sw).toBeCloseTo(400)
  expect(rect.sh).toBeCloseTo(400)
})

test('sourceRect shrinks the source square as zoom increases', () => {
  const rect = sourceRect(400, 400, 200, 2, { x: 0, y: 0 })
  expect(rect.sw).toBeCloseTo(200)
  expect(rect.sh).toBeCloseTo(200)
  expect(rect.sx).toBeCloseTo(100)
  expect(rect.sy).toBeCloseTo(100)
})

test('sourceRect shifts opposite the pan offset', () => {
  const zoomed = sourceRect(400, 400, 200, 2, { x: 50, y: 0 })
  // positive offset.x pans the image right on screen -> source window moves left
  expect(zoomed.sx).toBeLessThan(100)
})

test('wheelZoom is symmetric: +deltaY then -deltaY of the same magnitude returns to start', () => {
  const start = 2
  const up = wheelZoom(start, -100)
  const back = wheelZoom(up, 100)
  expect(back).toBeCloseTo(start, 5)
})

test('wheelZoom scrolls down (positive deltaY) to zoom out and clamps at MIN_ZOOM', () => {
  const zoomed = wheelZoom(MIN_ZOOM, 2000)
  expect(zoomed).toBe(MIN_ZOOM)
})

test('wheelZoom scrolls up (negative deltaY) to zoom in and clamps at MAX_ZOOM', () => {
  const zoomed = wheelZoom(MAX_ZOOM, -2000)
  expect(zoomed).toBe(MAX_ZOOM)
})

test('pinchZoom multiplies by the two-finger distance ratio', () => {
  expect(pinchZoom(2, 100, 150)).toBeCloseTo(3)
  expect(pinchZoom(2, 100, 50)).toBeCloseTo(1)
})

test('pinchZoom guards against a zero baseline distance', () => {
  expect(pinchZoom(2, 0, 100)).toBe(2)
})
