/** Pure geometry for the circular avatar cropper. No DOM/canvas here so it's plain-arithmetic testable. */

export const MIN_ZOOM = 1
export const MAX_ZOOM = 4

/**
 * Cropper viewport edge (px). Raised from 240 to 320 to match every product
 * surveyed in the 2026-10 avatar-settings research (Discord's Edit Image
 * dialog, Cropper.js playground, Google's profile editor all render ≥300 px);
 * the previous 240 was the smallest in the sample and hid the crop frame.
 */
export const VIEWPORT = 320

/** Output edge (px) written to the file on confirm. Matches Slack's 512x512 minimum. */
export const OUTPUT_SIZE = 512

/** Arrow-key pan step, in viewport px (independent of zoom). */
export const PAN_STEP = 12

/** `+` / `-` zoom step; roughly 10 % of the range per press. */
export const ZOOM_STEP = 0.15

/**
 * Wheel-to-zoom factor. Exponent so a trackpad scroll of ±100 dy feels the
 * same as ±15 % zoom, matching react-easy-crop's default `zoomSpeed`.
 */
export const WHEEL_ZOOM_FACTOR = 0.002

/** Preview widths (px) the dialog renders side-by-side so the user sees
 *  how the crop reads in the actual placements (ProfileCard, comment rows,
 *  HUD nameplate). Mirrors Discord's "Preview" panel. */
export const PREVIEW_SIZES = [128, 64, 32] as const

/** The zoom that makes the image exactly cover a `viewport`x`viewport` square at zoom=1 (our baseline). */
export function coverScale(naturalWidth: number, naturalHeight: number, viewport: number): number {
  return viewport / Math.min(naturalWidth, naturalHeight)
}

/**
 * Clamp a pan offset (in viewport pixels, center-relative) so the scaled image never
 * shows empty space inside the `viewport`x`viewport` crop square.
 */
export function clampPan(
  offset: { x: number; y: number },
  naturalWidth: number,
  naturalHeight: number,
  viewport: number,
  zoom: number,
): { x: number; y: number } {
  const scale = coverScale(naturalWidth, naturalHeight, viewport) * zoom
  const scaledW = naturalWidth * scale
  const scaledH = naturalHeight * scale
  const maxX = Math.max(0, (scaledW - viewport) / 2)
  const maxY = Math.max(0, (scaledH - viewport) / 2)
  return {
    x: Math.min(maxX, Math.max(-maxX, offset.x)),
    y: Math.min(maxY, Math.max(-maxY, offset.y)),
  }
}

export function clampZoom(zoom: number): number {
  return Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, zoom))
}

/**
 * Zoom for a wheel event: exponential in deltaY so a light scroll and a hard
 * fling feel proportional, and symmetric (scrolling up then down by the same
 * total returns to the starting zoom, which a linear `zoom + k*delta` does not).
 */
export function wheelZoom(current: number, deltaY: number): number {
  return clampZoom(current * Math.exp(-deltaY * WHEEL_ZOOM_FACTOR))
}

/** Zoom for a two-finger pinch: multiplicative in the ratio newDist/prevDist. */
export function pinchZoom(current: number, prevDistance: number, nextDistance: number): number {
  if (prevDistance <= 0) return current
  return clampZoom(current * (nextDistance / prevDistance))
}

/**
 * Given the image's natural size, the crop viewport size, the current pan offset (viewport px,
 * center-relative) and zoom, compute the source rectangle (in natural image pixels) that should
 * be drawn onto the output canvas.
 */
export function sourceRect(
  naturalWidth: number,
  naturalHeight: number,
  viewport: number,
  zoom: number,
  offset: { x: number; y: number },
): { sx: number; sy: number; sw: number; sh: number } {
  const scale = coverScale(naturalWidth, naturalHeight, viewport) * zoom
  const sw = viewport / scale
  const sh = viewport / scale
  const centerX = naturalWidth / 2 - offset.x / scale
  const centerY = naturalHeight / 2 - offset.y / scale
  return {
    sx: centerX - sw / 2,
    sy: centerY - sh / 2,
    sw,
    sh,
  }
}
