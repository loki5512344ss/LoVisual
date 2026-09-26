import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { copiesText } from '../copies'
import type { Listing } from '../types'

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long' }).format(new Date(iso))
}

export function ListingItem({ listing }: { listing: Listing }) {
  const { t, i18n } = useTranslation('showcase')
  const copies = copiesText(t, i18n.language, listing.copies_count)

  return (
    <article className="flex flex-col gap-2 rounded-(--radius-panel) border border-shoal bg-slate/60 px-4 py-4 shadow-[inset_0_1px_0_rgb(255_255_255/0.05)] transition-colors hover:border-ice/60">
      <Link to={`/showcase/${listing.id}`} className="flex flex-col gap-2">
        <h3 className="font-display text-lg text-snow">{listing.title}</h3>
        <p className="font-code text-xs text-ice">{listing.config_name}</p>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-frost">
          <span>{listing.author ? listing.author.nick : t('item.authorHidden')}</span>
          <span aria-hidden="true">·</span>
          <span>{copies}</span>
          <span aria-hidden="true">·</span>
          <span>{formatDate(listing.published_at)}</span>
        </div>
      </Link>
    </article>
  )
}
