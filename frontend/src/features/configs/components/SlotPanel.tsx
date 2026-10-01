import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useId, useState } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { Button } from '@shared/ui/Button'
import { Notice } from '@shared/ui/Notice'
import { ShareCode } from '@shared/ui/share-code/ShareCode'
import { regenerateCode, unpublish } from '../api'
import type { SlotSummary } from '../types'
import { PublishForm } from './PublishForm'

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long' }).format(new Date(iso))
}

export function SlotPanel({ slot, data }: { slot: 1 | 2 | 3 | 4; data: SlotSummary | null }) {
  const { t } = useTranslation('configs')
  const client = useQueryClient()
  const titleId = useId()
  const [confirmingCode, setConfirmingCode] = useState(false)
  const [confirmingUnpublish, setConfirmingUnpublish] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)

  const regen = useMutation({
    mutationFn: () => regenerateCode(slot),
    onSuccess: (res) => {
      client.setQueryData<SlotSummary[]>(['configs'], (prev) =>
        (prev ?? []).map((s) => (s.slot === slot ? { ...s, share_code: res.share_code } : s)),
      )
      setConfirmingCode(false)
    },
  })

  const unpub = useMutation({
    mutationFn: () => unpublish(slot),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['configs'] })
      setConfirmingUnpublish(false)
      setNotice(t('slot.unpublished'))
    },
  })

  return (
    <article
      aria-labelledby={titleId}
      className="flex flex-col gap-3 rounded-(--radius-panel) border border-shoal bg-slate/60 px-4 py-4 shadow-[inset_0_1px_0_rgb(255_255_255/0.05)]"
    >
      <div className="flex items-center gap-2">
        <span aria-hidden="true" className="size-2 rounded-full bg-ice shadow-[0_0_6px_var(--color-ice)]" />
        <h3 id={titleId} className="font-display text-lg text-snow">
          {t('slot.heading', { slot })}
        </h3>
      </div>

      {!data ? (
        <div className="flex flex-col gap-1">
          <p className="text-frost">{t('slot.empty')}</p>
          <p className="font-code text-sm text-frost/80">{t('slot.emptyHint', { cmd: `%config save ${slot}` })}</p>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          <p className="font-display text-xl text-snow">{data.name}</p>
          <p className="text-sm text-frost">{t('slot.updated', { when: formatDate(data.updated_at) })}</p>
          <ShareCode code={data.share_code} label={t('slot.codeLabel')} />
          <p className="font-code text-xs text-frost/80">{t('slot.codeHint', { cmd: `%config load ${data.share_code}` })}</p>

          {notice ? <Notice tone="info">{notice}</Notice> : null}

          <div className="flex flex-wrap items-center gap-2">
            {confirmingCode ? (
              <>
                <span className="text-sm text-ember">{t('slot.newCodeConfirm')}</span>
                <Button variant="danger" busy={regen.isPending} onClick={() => regen.mutate()}>
                  {t('slot.change')}
                </Button>
                <Button variant="quiet" onClick={() => setConfirmingCode(false)}>
                  {t('slot.cancel')}
                </Button>
              </>
            ) : (
              <Button variant="quiet" onClick={() => setConfirmingCode(true)}>
                {t('slot.newCode')}
              </Button>
            )}

            {!publishing &&
              (data.published ? (
                confirmingUnpublish ? (
                  <>
                    <span className="text-sm text-ember">{t('slot.unpublishConfirm')}</span>
                    <Button variant="danger" busy={unpub.isPending} onClick={() => unpub.mutate()}>
                      {t('slot.confirmUnpublish')}
                    </Button>
                    <Button variant="quiet" onClick={() => setConfirmingUnpublish(false)}>
                      {t('slot.cancel')}
                    </Button>
                  </>
                ) : (
                  <Button variant="danger" onClick={() => setConfirmingUnpublish(true)}>
                    {t('slot.unpublish')}
                  </Button>
                )
              ) : (
                <Button variant="primary" onClick={() => setPublishing(true)}>
                  {t('slot.publish')}
                </Button>
              ))}
          </div>

          {publishing ? (
            <PublishForm
              slot={slot}
              onCancel={() => setPublishing(false)}
              onPublished={() => {
                setPublishing(false)
                setNotice(t('slot.published'))
              }}
            />
          ) : null}
        </div>
      )}
    </article>
  )
}
