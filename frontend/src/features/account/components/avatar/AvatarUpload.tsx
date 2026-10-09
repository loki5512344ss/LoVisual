import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useId, useState, type ChangeEvent } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { Notice } from '@shared/ui/Notice'
import { useSession } from '@features/auth/session/useSession'
import {
  addAvatarHistory,
  checkAvatarFile,
  fetchCurrentAvatarAsFile,
  uploadAvatarWithProgress,
} from '@features/account/api'
import { AvatarCropper } from './AvatarCropper'
import { AvatarHistoryStrip } from './AvatarHistoryStrip'

/** Picker + drop target + history strip: validates a file, opens the cropper, uploads it with progress and confirms in place. */
export function AvatarUpload({ onReady }: { onReady?: (acceptFile: (file: File) => void) => void } = {}) {
  const { t } = useTranslation('account')
  const { me } = useSession()
  const id = useId()
  const client = useQueryClient()
  const [error, setError] = useState<string>()
  const [picked, setPicked] = useState<File>()
  const [progress, setProgress] = useState<number | null>(null)
  const [done, setDone] = useState(false)
  const [historyTick, setHistoryTick] = useState(0)
  const mutation = useMutation({
    mutationFn: (file: File) => uploadAvatarWithProgress(file, setProgress),
    onSuccess: async (_data, file) => {
      client.invalidateQueries({ queryKey: ['me'] })
      // Await the write before bumping the tick, otherwise the history strip
      // re-reads IndexedDB too early and only fills in after a remount (F5).
      await addAvatarHistory(file).catch(() => undefined)
      setHistoryTick((n) => n + 1)
      setPicked(undefined)
      setProgress(null)
      setDone(true)
    },
    onError: (err: Error) => {
      setProgress(null)
      setError(err.message)
    },
  })

  useEffect(() => {
    if (!done) return
    const timer = setTimeout(() => setDone(false), 2500)
    return () => clearTimeout(timer)
  }, [done])

  const validatePick = useCallback(
    (file: File) => {
      const problem = checkAvatarFile(file, t)
      if (problem) {
        setError(problem)
        return
      }
      setError(undefined)
      setPicked(file)
    },
    [t],
  )
  // Hand the validate+open-cropper path to ProfileCard's drop target without duplicating it.
  useEffect(() => {
    onReady?.(validatePick)
  }, [onReady, validatePick])

  function onChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (file) validatePick(file)
  }

  async function recropCurrent() {
    if (!me?.avatar_url) return
    try {
      setPicked(await fetchCurrentAvatarAsFile(me.avatar_url))
      setError(undefined)
    } catch {
      setError(t('avatar.fetchFailed'))
    }
  }

  function onConfirmCrop(blob: Blob) {
    mutation.mutate(new File([blob], 'avatar.png', { type: 'image/png' }))
  }

  if (picked) {
    return (
      <div className="flex flex-col items-center gap-2">
        <AvatarCropper file={picked} onCancel={() => setPicked(undefined)} onConfirm={onConfirmCrop} />
        {progress !== null && <ProgressBar pct={progress} />}
        {error ? <Notice tone="error">{error}</Notice> : null}
      </div>
    )
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <div className="flex flex-wrap items-center justify-center gap-2">
        <label
          htmlFor={id}
          className="cursor-pointer rounded-ctl border border-shoal px-3 py-1.5 text-sm text-snow hover:border-frost"
        >
          {t('avatar.change')}
          <input
            id={id}
            type="file"
            accept="image/png,image/jpeg,image/webp"
            className="sr-only"
            onChange={onChange}
          />
        </label>
        {me?.avatar_url ? (
          <button
            type="button"
            onClick={recropCurrent}
            className="cursor-pointer rounded-ctl border border-shoal px-3 py-1.5 text-sm text-snow hover:border-frost"
          >
            {t('avatar.recrop')}
          </button>
        ) : null}
      </div>
      {progress !== null && <ProgressBar pct={progress} />}
      {done ? <Notice tone="success">{t('avatar.uploaded')}</Notice> : null}
      {error ? <Notice tone="error">{error}</Notice> : null}
      <AvatarHistoryStrip
        tick={historyTick}
        pending={mutation.isPending}
        onPick={(blob) => mutation.mutate(new File([blob], 'avatar.png', { type: 'image/png' }))}
      />
    </div>
  )
}

function ProgressBar({ pct }: { pct: number }) {
  return (
    <div
      role="progressbar"
      aria-valuenow={pct}
      aria-valuemin={0}
      aria-valuemax={100}
      className="h-1 w-full max-w-[240px] overflow-hidden rounded-full bg-shoal"
    >
      <div className="h-full bg-ice transition-[width] duration-150" style={{ width: `${pct}%` }} />
    </div>
  )
}
