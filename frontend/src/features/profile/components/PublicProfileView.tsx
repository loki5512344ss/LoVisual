import { useQuery } from '@tanstack/react-query'
import { useTranslation } from '@shared/i18n/react'
import { ApiError } from '@shared/api/client'
import { Notice } from '@shared/ui/Notice'
import { ListingList } from '@features/showcase/components/ListingList'
import { getProfile } from '../api'

export function PublicProfileView({ id }: { id: string }) {
  const { t, i18n } = useTranslation('profile')
  const { data, error, isPending } = useQuery({ queryKey: ['profile', id], queryFn: () => getProfile(id), retry: false })

  if (isPending) return <p className="text-frost">…</p>
  if (error || !data) {
    const missing = error instanceof ApiError && error.status === 404
    return <Notice tone="error">{missing ? t('notFound') : t('error')}</Notice>
  }

  const joined = new Intl.DateTimeFormat(i18n.language, { dateStyle: 'long' }).format(new Date(data.created_at))
  return (
    <div className="flex flex-col gap-10">
      <div className="flex items-center gap-5 rounded-(--radius-panel) border border-shoal bg-slate/70 p-6">
        <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-ice bg-abyss font-display text-3xl">
          {data.avatar_url ? (
            <img src={data.avatar_url} alt="" className="h-full w-full object-cover" />
          ) : (
            <span aria-hidden="true">{data.display_nick.slice(0, 1).toUpperCase()}</span>
          )}
        </div>
        <div className="min-w-0">
          <h1 className="truncate font-display text-2xl">{data.display_nick}</h1>
          <p className="mt-1 text-sm text-frost">{t('memberSince', { date: joined })}</p>
          {data.badges.includes('early') ? (
            <span className="mt-2 inline-block rounded-full border border-ice/40 px-3 py-0.5 font-code text-xs text-ice">
              {t('badgeEarly')}
            </span>
          ) : null}
        </div>
      </div>
      <section className="flex flex-col gap-4">
        <h2 className="font-display text-xl">{t('configs')}</h2>
        <ListingList author={id} />
      </section>
    </div>
  )
}
