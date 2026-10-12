import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type WheelEvent as ReactWheelEvent,
} from 'react'
import { useTranslation } from '@shared/i18n/react'
import {
  MAX_ZOOM,
  MIN_ZOOM,
  PAN_STEP,
  VIEWPORT,
  ZOOM_STEP,
  clampPan,
  clampZoom,
  coverScale,
  pinchZoom,
  wheelZoom,
} from './cropMath'

export type CropState = {
  zoom: number
  offset: { x: number; y: number }
  natural: { w: number; h: number }
}

type Props = {
  file: File
  /** Fires once on first user interaction so the dialog can flip an "unsaved changes" guard. */
  onDirty?: () => void
  /** Fires whenever the crop geometry changes; the dialog renders previews off this. */
  onCropState?: (state: CropState) => void
}

/**
 * The interactive crop canvas. Owns image layout (drag to pan, wheel / pinch to
 * zoom, arrow keys / +/-/0 for keyboard), the visible rule-of-thirds guides and
 * the corner shade that shows exactly what falls inside the circle. Cancel and
 * Confirm live in {@link AvatarEditDialog}, not here — this component is pure
 * viewport + input handling.
 *
 * <p>Everything the 2026-10 avatar-settings research identified as "missing in
 * ours, present in every reference product":
 * <ul>
 *   <li>bigger viewport (320 vs the previous 240 — the smallest in the sample)</li>
 *   <li>visible guides + shade, so the user knows what will actually end up in
 *       the file (previous `overflow-hidden rounded-full` hid the crop frame
 *       entirely)</li>
 *   <li>wheel zoom + pinch zoom (both no-brainers per react-easy-crop)</li>
 *   <li>keyboard: arrows pan, +/- zoom, 0 resets (Google documents this)</li>
 *   <li>a Reset button returning to zoom=MIN, offset=(0,0)</li>
 * </ul>
 */
export function AvatarCropper({ file, onDirty, onCropState }: Props) {
  const { t } = useTranslation('account')
  const [url] = useState(() => URL.createObjectURL(file))
  useEffect(() => () => URL.revokeObjectURL(url), [url])
  const [natural, setNatural] = useState<{ w: number; h: number }>()
  const [zoom, setZoom] = useState(MIN_ZOOM)
  const [offset, setOffset] = useState({ x: 0, y: 0 })

  const pointers = useRef(new Map<number, { x: number; y: number }>())
  const pinchBase = useRef<{ distance: number; zoom: number } | null>(null)
  const drag = useRef<{ x: number; y: number; offset: { x: number; y: number } } | null>(null)

  const markDirty = useCallback(() => onDirty?.(), [onDirty])

  // Publish crop state so the dialog's preview column stays in sync.
  useEffect(() => {
    if (!natural) return
    onCropState?.({ zoom, offset, natural })
  }, [natural, zoom, offset, onCropState])

  const applyZoom = useCallback(
    (next: number) => {
      const clamped = clampZoom(next)
      setZoom(clamped)
      if (natural) setOffset((prev) => clampPan(prev, natural.w, natural.h, VIEWPORT, clamped))
      markDirty()
    },
    [natural, markDirty],
  )

  const applyPan = useCallback(
    (next: { x: number; y: number }) => {
      if (!natural) return
      setOffset(clampPan(next, natural.w, natural.h, VIEWPORT, zoom))
      markDirty()
    },
    [natural, zoom, markDirty],
  )

  function reset() {
    setZoom(MIN_ZOOM)
    setOffset({ x: 0, y: 0 })
    markDirty()
  }

  function onPointerDown(e: ReactPointerEvent<HTMLDivElement>) {
    e.currentTarget.setPointerCapture(e.pointerId)
    pointers.current.set(e.pointerId, { x: e.clientX, y: e.clientY })
    if (pointers.current.size === 2) {
      const [a, b] = [...pointers.current.values()]
      pinchBase.current = { distance: Math.hypot(a.x - b.x, a.y - b.y), zoom }
      drag.current = null
      return
    }
    drag.current = { x: e.clientX, y: e.clientY, offset }
  }

  function onPointerMove(e: ReactPointerEvent<HTMLDivElement>) {
    if (!natural) return
    if (pointers.current.has(e.pointerId)) {
      pointers.current.set(e.pointerId, { x: e.clientX, y: e.clientY })
    }
    if (pointers.current.size === 2 && pinchBase.current) {
      const [a, b] = [...pointers.current.values()]
      const next = Math.hypot(a.x - b.x, a.y - b.y)
      applyZoom(pinchZoom(pinchBase.current.zoom, pinchBase.current.distance, next))
      return
    }
    if (!drag.current) return
    const dx = e.clientX - drag.current.x
    const dy = e.clientY - drag.current.y
    applyPan({ x: drag.current.offset.x + dx, y: drag.current.offset.y + dy })
  }

  function onPointerUp(e: ReactPointerEvent<HTMLDivElement>) {
    pointers.current.delete(e.pointerId)
    if (pointers.current.size < 2) pinchBase.current = null
    if (pointers.current.size === 0) drag.current = null
  }

  function onWheel(e: ReactWheelEvent<HTMLDivElement>) {
    if (!natural) return
    applyZoom(wheelZoom(zoom, e.deltaY))
  }

  function onKeyDown(e: ReactKeyboardEvent<HTMLDivElement>) {
    if (!natural) return
    switch (e.key) {
      case 'ArrowLeft':  e.preventDefault(); applyPan({ x: offset.x - PAN_STEP, y: offset.y }); return
      case 'ArrowRight': e.preventDefault(); applyPan({ x: offset.x + PAN_STEP, y: offset.y }); return
      case 'ArrowUp':    e.preventDefault(); applyPan({ x: offset.x, y: offset.y - PAN_STEP }); return
      case 'ArrowDown':  e.preventDefault(); applyPan({ x: offset.x, y: offset.y + PAN_STEP }); return
      case '+':
      case '=':          e.preventDefault(); applyZoom(zoom + ZOOM_STEP); return
      case '-':
      case '_':          e.preventDefault(); applyZoom(zoom - ZOOM_STEP); return
      case '0':          e.preventDefault(); reset(); return
    }
  }

  const scale = natural ? coverScale(natural.w, natural.h, VIEWPORT) * zoom : 1
  const drawnW = natural ? natural.w * scale : VIEWPORT
  const drawnH = natural ? natural.h * scale : VIEWPORT

  return (
    <div className="flex flex-col items-center gap-2">
      <div
        role="group"
        aria-label={t('avatar.cropperAria')}
        tabIndex={0}
        data-testid="cropper-viewport"
        className="relative overflow-hidden rounded-full border border-shoal outline-none touch-none focus-visible:ring-2 focus-visible:ring-ice"
        style={{ width: VIEWPORT, height: VIEWPORT, cursor: 'grab' }}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
        onWheel={onWheel}
        onKeyDown={onKeyDown}
      >
        <img
          src={url}
          alt=""
          draggable={false}
          onLoad={(e) => setNatural({ w: e.currentTarget.naturalWidth, h: e.currentTarget.naturalHeight })}
          className="pointer-events-none absolute select-none"
          style={{
            width: drawnW,
            height: drawnH,
            left: VIEWPORT / 2 - drawnW / 2 + offset.x,
            top: VIEWPORT / 2 - drawnH / 2 + offset.y,
          }}
        />
        {/* Rule-of-thirds guides. Kept at 40% so they read as an overlay, not a wall. */}
        <div className="pointer-events-none absolute inset-0 opacity-40">
          <div className="absolute inset-y-0 left-1/3 w-px bg-snow" />
          <div className="absolute inset-y-0 left-2/3 w-px bg-snow" />
          <div className="absolute inset-x-0 top-1/3 h-px bg-snow" />
          <div className="absolute inset-x-0 top-2/3 h-px bg-snow" />
        </div>
        {/* Corner shade — the region outside the round crop target gets dimmed, so
            the user sees exactly what falls inside the file. */}
        <div
          className="pointer-events-none absolute inset-0 rounded-full"
          style={{ boxShadow: 'inset 0 0 0 9999px rgba(0,0,0,0.55)' }}
        />
      </div>
      <div className="flex w-full max-w-[360px] items-center gap-2">
        <label className="flex flex-1 flex-col gap-1 text-sm text-snow">
          {t('avatar.zoom')}
          <input
            type="range"
            min={MIN_ZOOM}
            max={MAX_ZOOM}
            step={0.01}
            value={zoom}
            onChange={(e) => applyZoom(Number(e.target.value))}
          />
        </label>
        <button
          type="button"
          onClick={reset}
          className="cursor-pointer rounded-ctl border border-shoal px-3 py-1 text-sm text-snow hover:border-frost"
        >
          {t('avatar.reset')}
        </button>
      </div>
      <p className="text-xs text-shoal">{t('avatar.cropperHint')}</p>
    </div>
  )
}
