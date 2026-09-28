import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../../../shared/ui/Button'
import { TextField } from '../../../shared/ui/text-field/TextField'
import { publish } from '../api'

const MAX_DESCRIPTION = 500

export function PublishForm({
  slot,
  onCancel,
  onPublished,
}: {
  slot: number
  onCancel: () => void
  onPublished: () => void
}) {
  const { t } = useTranslation('configs')
  const client = useQueryClient()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [touched, setTouched] = useState(false)

  const mutation = useMutation({
    mutationFn: () => publish(slot, title.trim(), description),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['configs'] })
      onPublished()
    },
  })

  const invalid = touched && title.trim().length === 0

  function submit() {
    setTouched(true)
    if (title.trim().length === 0 || title.trim().length > 64 || description.length > MAX_DESCRIPTION) return
    mutation.mutate()
  }

  return (
    <div className="flex flex-col gap-3 rounded-ctl border border-shoal bg-abyss/60 p-3">
      <TextField
        label={t('publishForm.title')}
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        error={invalid ? t('publishForm.titleRequired') : undefined}
        maxLength={64}
      />
      <div className="flex flex-col gap-1.5">
        <label htmlFor="publish-description" className="text-sm text-frost">
          {t('publishForm.description')}
        </label>
        <textarea
          id="publish-description"
          value={description}
          onChange={(e) => setDescription(e.target.value.slice(0, MAX_DESCRIPTION))}
          rows={3}
          className="rounded-ctl border border-shoal bg-abyss px-3 py-2 text-snow focus:border-ice"
        />
        <span className="self-end text-xs text-frost">{t('publishForm.counter', { count: description.length })}</span>
      </div>
      <div className="flex gap-2">
        <Button variant="primary" busy={mutation.isPending} onClick={submit}>
          {t('publishForm.submit')}
        </Button>
        <Button variant="quiet" onClick={onCancel}>
          {t('publishForm.cancel')}
        </Button>
      </div>
    </div>
  )
}
