import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation } from 'react-router'
import { Notice } from '../../../shared/ui/Notice'
import { ApiError } from '../../../shared/api/client'
import { useSession } from '../../auth/session'
import { listSlots } from '../../configs/api'
import { copyToSlot } from '../api'

const SLOTS = [1, 2, 3, 4] as const

export function CopyToSlot({ listingId }: { listingId: string }) {
  const { t } = useTranslation('showcase')
  const { status } = useSession()
  const location = useLocation()
  const client = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const [copiedSlot, setCopiedSlot] = useState<number | null>(null)

  const { data: slots } = useQuery({ queryKey: ['configs'], queryFn: listSlots, enabled: status === 'authenticated' })
  const occupied = new Set((slots ?? []).map((s) => s.slot))

  const mutation = useMutation({
    mutationFn: (slot: number) => copyToSlot(listingId, slot),
    onSuccess: (_data, slot) => {
      setError(null)
      setCopiedSlot(slot)
      client.invalidateQueries({ queryKey: ['configs'] })
    },
    onError: (err) => {
      setError(err instanceof ApiError && err.status === 409 ? t('copy.conflict') : t('page.error'))
    },
  })

  if (status !== 'authenticated') {
    return (
      <div className="flex flex-col gap-2">
        <p className="text-frost">{t('copy.loginPrompt')}</p>
        <Link to="/login" state={{ from: location.pathname }} className="text-ice hover:underline">
          {t('copy.login')}
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <h2 className="font-display text-lg text-snow">{t('copy.heading')}</h2>
      {copiedSlot ? (
        <Notice tone="info">
          {t('copy.success', { slot: copiedSlot })}{' '}
          <Link to="/configs" className="text-ice hover:underline">
            {t('copy.openConfigs')}
          </Link>
        </Notice>
      ) : null}
      {error ? <Notice tone="error">{error}</Notice> : null}
      <div role="group" aria-label={t('copy.heading')} className="inline-flex overflow-hidden rounded-ctl border border-shoal">
        {SLOTS.map((slot) => (
          <button
            key={slot}
            type="button"
            disabled={occupied.has(slot) || mutation.isPending}
            title={occupied.has(slot) ? t('copy.occupied') : undefined}
            onClick={() => mutation.mutate(slot)}
            className="border-r border-shoal px-4 py-2 text-sm font-semibold text-snow last:border-r-0 hover:bg-ice/10 disabled:cursor-not-allowed disabled:text-frost/50 disabled:hover:bg-transparent"
          >
            {t('copy.slot', { slot })}
          </button>
        ))}
      </div>
    </div>
  )
}
