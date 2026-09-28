import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useId, useState, type ChangeEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Notice } from '../../../shared/ui/Notice'
import { checkAvatarFile, uploadAvatar } from '../api'
import { AvatarCropper } from './AvatarCropper'

/** A label-based file picker (keyboard reachable) that opens a crop step before uploading and refreshing the cached profile. */
export function AvatarUpload() {
  const { t } = useTranslation('account')
  const id = useId()
  const client = useQueryClient()
  const [error, setError] = useState<string>()
  const [picked, setPicked] = useState<File>()
  const mutation = useMutation({
    mutationFn: uploadAvatar,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['me'] })
      setPicked(undefined)
    },
  })

  function onChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    const problem = checkAvatarFile(file, t)
    if (problem) {
      setError(problem)
      return
    }
    setError(undefined)
    setPicked(file)
  }

  function onConfirmCrop(blob: Blob) {
    const cropped = new File([blob], 'avatar.png', { type: 'image/png' })
    mutation.mutate(cropped)
  }

  if (picked) {
    return (
      <div className="flex flex-col items-center gap-2">
        <AvatarCropper file={picked} onCancel={() => setPicked(undefined)} onConfirm={onConfirmCrop} />
        {error ? <Notice tone="error">{error}</Notice> : null}
      </div>
    )
  }

  return (
    <div className="flex flex-col items-center gap-2">
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
      {error ? <Notice tone="error">{error}</Notice> : null}
    </div>
  )
}
