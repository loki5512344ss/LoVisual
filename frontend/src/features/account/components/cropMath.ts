/** Pure geometry for the circular avatar cropper. No DOM/canvas here so it's plain-arithmetic testable. */

export const MIN_ZOOM = 1
export const MAX_ZOOM = 4

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
