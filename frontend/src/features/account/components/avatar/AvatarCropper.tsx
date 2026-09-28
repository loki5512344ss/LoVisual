import { useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@shared/ui/Button'
import { clampPan, clampZoom, coverScale, MAX_ZOOM, MIN_ZOOM, sourceRect } from './cropMath'

const VIEWPORT = 240
const OUTPUT_SIZE = 512

type Props = {
  file: File
  onCancel: () => void
  onConfirm: (blob: Blob) => void
}

/** Drag-to-reposition, zoom-to-scale circular crop UI. Renders the selection onto an offscreen canvas on confirm. */
export function AvatarCropper({ file, onCancel, onConfirm }: Props) {
  const { t } = useTranslation('account')
  const [url] = useState(() => URL.createObjectURL(file))
  const [natural, setNatural] = useState<{ w: number; h: number }>()
  const [zoom, setZoom] = useState(MIN_ZOOM)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const [busy, setBusy] = useState(false)
  const drag = useRef<{ x: number; y: number; offset: { x: number; y: number } } | null>(null)

  function onImgLoad(e: React.SyntheticEvent<HTMLImageElement>) {
    setNatural({ w: e.currentTarget.naturalWidth, h: e.currentTarget.naturalHeight })
  }

  function onPointerDown(e: ReactPointerEvent<HTMLDivElement>) {
    e.currentTarget.setPointerCapture(e.pointerId)
    drag.current = { x: e.clientX, y: e.clientY, offset }
  }

  function onPointerMove(e: ReactPointerEvent<HTMLDivElement>) {
    if (!drag.current || !natural) return
    const dx = e.clientX - drag.current.x
    const dy = e.clientY - drag.current.y
    const next = { x: drag.current.offset.x + dx, y: drag.current.offset.y + dy }
    setOffset(clampPan(next, natural.w, natural.h, VIEWPORT, zoom))
  }

  function onPointerUp() {
    drag.current = null
  }

  function onZoomChange(next: number) {
    const clamped = clampZoom(next)
    setZoom(clamped)
    if (natural) setOffset((prev) => clampPan(prev, natural.w, natural.h, VIEWPORT, clamped))
  }

  async function confirm() {
    if (!natural) return
    setBusy(true)
    const img = new Image()
    img.src = url
    await img.decode()
    const { sx, sy, sw, sh } = sourceRect(natural.w, natural.h, VIEWPORT, zoom, offset)
    const canvas = document.createElement('canvas')
    canvas.width = OUTPUT_SIZE
    canvas.height = OUTPUT_SIZE
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      setBusy(false)
      return
    }
    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE)
    canvas.toBlob((blob) => {
      setBusy(false)
      if (blob) onConfirm(blob)
    }, 'image/png')
  }

  const scale = natural ? coverScale(natural.w, natural.h, VIEWPORT) * zoom : 1
  const drawnW = natural ? natural.w * scale : VIEWPORT
  const drawnH = natural ? natural.h * scale : VIEWPORT

  return (
    <div className="flex flex-col items-center gap-3">
      <div
        className="relative overflow-hidden rounded-full border border-shoal touch-none"
        style={{ width: VIEWPORT, height: VIEWPORT, cursor: 'grab' }}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
      >
        <img
          src={url}
          alt=""
          draggable={false}
          onLoad={onImgLoad}
          className="pointer-events-none absolute select-none"
          style={{
            width: drawnW,
            height: drawnH,
            left: VIEWPORT / 2 - drawnW / 2 + offset.x,
            top: VIEWPORT / 2 - drawnH / 2 + offset.y,
          }}
        />
      </div>
      <label className="flex w-full max-w-[240px] flex-col gap-1 text-sm text-snow">
        {t('avatar.zoom')}
        <input
          type="range"
          min={MIN_ZOOM}
          max={MAX_ZOOM}
          step={0.01}
          value={zoom}
          onChange={(e) => onZoomChange(Number(e.target.value))}
        />
      </label>
      <p className="text-xs text-shoal">{t('avatar.reposition')}</p>
      <div className="flex gap-2">
        <Button variant="quiet" onClick={onCancel} disabled={busy}>
          {t('avatar.cancel')}
        </Button>
        <Button variant="primary" onClick={confirm} busy={busy} disabled={!natural}>
          {t('avatar.confirm')}
        </Button>
      </div>
    </div>
  )
}
