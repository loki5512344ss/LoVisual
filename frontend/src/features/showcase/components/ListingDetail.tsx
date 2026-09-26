import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Notice } from '../../../shared/ui/Notice'
import { getListing } from '../api'
import { copiesText } from '../copies'
import { CopyToSlot } from './CopyToSlot'

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long' }).format(new Date(iso))
}

export function ListingDetail({ id }: { id: string }) {
  const { t, i18n } = useTranslation('showcase')
  const { data, isError, isPending } = useQuery({ queryKey: ['listing', id], queryFn: () => getListing(id) })

  if (isPending) return null
  if (isError || !data) {
    return (
      <div className="flex flex-col gap-4">
        <Notice tone="error">{t('detail.notFound')}</Notice>
        <Link to="/showcase" className="text-ice hover:underline">
          {t('detail.back')}
        </Link>
      </div>
    )
  }

  return (
    <article className="flex flex-col gap-4 rounded-(--radius-panel) border border-shoal bg-slate/60 px-5 py-6 shadow-[inset_0_1px_0_rgb(255_255_255/0.05)]">
      <Link to="/showcase" className="text-sm text-ice hover:underline">
        {t('detail.back')}
      </Link>
      <h1 className="font-display text-3xl text-snow">{data.title}</h1>
      <p className="font-code text-sm text-ice">{data.config_name}</p>
      <p className="whitespace-pre-line text-frost">{data.description}</p>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-frost">
        {data.author && data.author_id ? (
          <Link to={`/u/${data.author_id}`} className="text-ice underline-offset-4 hover:underline">
            {data.author.nick}
          </Link>
        ) : (
          <span>{data.author ? data.author.nick : t('item.authorHidden')}</span>
        )}
        <span aria-hidden="true">·</span>
        <span>{formatDate(data.published_at)}</span>
        <span aria-hidden="true">·</span>
        <span>{copiesText(t, i18n.language, data.copies_count)}</span>
      </div>
      <CopyToSlot listingId={data.id} />
    </article>
  )
}
