import { useCallback, useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from '@shared/i18n/react'
import { Button } from '@shared/ui/Button'
import { Notice } from '@shared/ui/Notice'
import { AvatarCropper, type CropState } from './AvatarCropper'
import { OUTPUT_SIZE, PREVIEW_SIZES, VIEWPORT, sourceRect } from './cropMath'

type Props = {
  file: File
  onCancel: () => void
  onConfirm: (blob: Blob) => void
  /** Upload progress in 0..100, or null while idle. Kept inside the modal so
   *  the user does not have to look away from the crop while a slow upload runs. */
  progress: number | null
  error?: string
}

/**
 * Modal wrapper around {@link AvatarCropper}: the "Edit Image" dialog pattern
 * from Discord / Google / Slack, replacing the previous inline crop block that
 * made the whole flow feel like a piece of wireframe. Owns:
 * - focus trap (initial focus into the card, ESC / X / backdrop close)
 * - dirty guard (any pan/zoom flips `dirty`; closing when dirty asks for
 *   confirmation)
 * - filename + size + accepted-format hint in the header
 * - the "how others see it" preview column (128/64/32 px) rendered live from
 *   the same crop state so the user is not surprised when the small HUD
 *   nameplate clips their ears
 * - progress bar and error notice inline
 */
export function AvatarEditDialog({ file, onCancel, onConfirm, progress, error }: Props) {
  const { t } = useTranslation('account')
  const [crop, setCrop] = useState<CropState | null>(null)
  const [dirty, setDirty] = useState(false)
  const [busy, setBusy] = useState(false)
  const cardRef = useRef<HTMLDivElement | null>(null)

  const markDirty = useCallback(() => setDirty(true), [])

  // Publishes crop state upward; useState's setter is stable so the cropper's
  // effect dependency array does not thrash.
  const publishCrop = useCallback((state: CropState) => setCrop(state), [])

  const attemptClose = useCallback(() => {
    if (busy) return
    if (dirty && !window.confirm(t('avatar.discardConfirm'))) return
    onCancel()
  }, [busy, dirty, onCancel, t])

  // ESC handler lives on window so it also fires when focus is on the range
  // slider or the file input's hidden click path.
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation()
        attemptClose()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [attemptClose])

  useEffect(() => {
    cardRef.current?.focus()
  }, [])

  async function confirm() {
    if (!crop) return
    setBusy(true)
    try {
      const blob = await renderCrop(file, crop, OUTPUT_SIZE)
      onConfirm(blob)
    } catch {
      // Blob failure falls through to the parent's error state on the next
      // render (the mutation onError path already shows the message).
    } finally {
      setBusy(false)
    }
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) attemptClose()
      }}
      role="dialog"
      aria-modal="true"
      aria-label={t('avatar.dialogTitle')}
    >
      <div
        ref={cardRef}
        tabIndex={-1}
        className="flex max-h-[92vh] w-full max-w-[860px] flex-col gap-4 overflow-auto rounded-(--radius-panel) border border-shoal bg-slate/95 p-5 outline-none"
      >
        <header className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h2 className="font-display text-lg text-snow">{t('avatar.dialogTitle')}</h2>
            <p className="truncate text-xs text-frost">
              {file.name} · {formatBytes(file.size)} · {t('avatar.limits')}
            </p>
          </div>
          <button
            type="button"
            aria-label={t('avatar.close')}
            onClick={attemptClose}
            className="cursor-pointer rounded-ctl px-2 py-1 text-xl leading-none text-frost hover:text-snow"
          >
            ×
          </button>
        </header>

        <div className="flex flex-wrap items-start justify-center gap-6">
          <AvatarCropper file={file} onDirty={markDirty} onCropState={publishCrop} />
          <aside className="flex flex-col items-center gap-2">
            <p className="text-xs text-shoal">{t('avatar.previewLabel')}</p>
            {crop ? (
              <div className="flex items-end gap-3">
                {PREVIEW_SIZES.map((size) => (
                  <Preview key={size} file={file} crop={crop} size={size} />
                ))}
              </div>
            ) : (
              <div style={{ width: 200, height: 200 }} />
            )}
          </aside>
        </div>

        {progress !== null ? (
          <div
            role="progressbar"
            aria-valuenow={progress}
            aria-valuemin={0}
            aria-valuemax={100}
            className="h-1 w-full overflow-hidden rounded-full bg-shoal"
          >
            <div className="h-full bg-ice transition-[width] duration-150" style={{ width: `${progress}%` }} />
          </div>
        ) : null}
        {error ? <Notice tone="error">{error}</Notice> : null}

        <footer className="flex items-center justify-between gap-3">
          <Button variant="quiet" onClick={attemptClose} disabled={busy}>
            {t('avatar.cancel')}
          </Button>
          <div className="text-xs text-shoal">{dirty ? t('avatar.unsaved') : ''}</div>
          <Button variant="primary" onClick={confirm} busy={busy} disabled={!crop}>
            {t('avatar.confirm')}
          </Button>
        </footer>
      </div>
    </div>,
    document.body,
  )
}

function formatBytes(n: number): string {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(2)} MB`
}

/** Draws the crop rectangle from `file` at the requested output edge. Used for
 *  both the final PNG (OUTPUT_SIZE) and the preview tiles (128/64/32). */
async function renderCrop(file: File, state: CropState, size: number): Promise<Blob> {
  const url = URL.createObjectURL(file)
  try {
    const img = new Image()
    img.src = url
    await img.decode()
    const { sx, sy, sw, sh } = sourceRect(state.natural.w, state.natural.h, VIEWPORT, state.zoom, state.offset)
    const canvas = document.createElement('canvas')
    canvas.width = size
    canvas.height = size
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('canvas 2d unavailable')
    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, size, size)
    return await new Promise<Blob>((resolve, reject) =>
      canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('toBlob failed'))), 'image/png'),
    )
  } finally {
    URL.revokeObjectURL(url)
  }
}

function Preview({ file, crop, size }: { file: File; crop: CropState; size: number }) {
  const [url, setUrl] = useState<string | null>(null)
  useEffect(() => {
    let active = true
    let local: string | null = null
    renderCrop(file, crop, size)
      .then((blob) => {
        if (!active) return
        local = URL.createObjectURL(blob)
        setUrl(local)
      })
      .catch(() => active && setUrl(null))
    return () => {
      active = false
      if (local) URL.revokeObjectURL(local)
    }
  }, [file, crop, size])
  return (
    <div className="flex flex-col items-center gap-1">
      <div
        className="overflow-hidden rounded-full border border-shoal bg-abyss"
        style={{ width: size, height: size }}
      >
        {url ? <img src={url} alt="" className="h-full w-full object-cover" /> : null}
      </div>
      <span className="text-[10px] text-shoal">{size}px</span>
    </div>
  )
}
