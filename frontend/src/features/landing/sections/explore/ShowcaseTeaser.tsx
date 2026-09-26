import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { browse } from '../../../showcase/api'
import type { Listing } from '../../../showcase/types'
import '../../styles/landing.css'

/** Placeholder cards so the section never collapses while loading or when the API is down. */
const EXAMPLES: Listing[] = [
  { id: 'e1', title: 'Aura / Trails', config_name: 'aura', description: '', copies_count: 128, published_at: '', author: { nick: 'example', avatar_url: null } },
  { id: 'e2', title: 'Clean PvP', config_name: 'pvp', description: '', copies_count: 96, published_at: '', author: { nick: 'example', avatar_url: null } },
  { id: 'e3', title: 'Stream Safe', config_name: 'stream', description: '', copies_count: 74, published_at: '', author: { nick: 'example', avatar_url: null } },
]

export function ShowcaseTeaser() {
  const { t } = useTranslation('landing')
  const { data, isError } = useQuery({
    queryKey: ['showcase', 'popular', 0, null],
    queryFn: () => browse('popular', 0),
  })
  const real = !isError && data ? data.items.slice(0, 3) : []
  const cards = real.length >= 3 ? real : EXAMPLES
  const isExample = real.length < 3

  return (
    <section aria-labelledby="showcase-title" className="grid gap-10 py-20 sm:py-28">
      <div className="flex min-w-0 flex-col items-start gap-4">
        <span className="font-code text-xs uppercase tracking-[0.2em] text-ice">{t('showcase.eyebrow')}</span>
        <h2 id="showcase-title" className="text-[2rem] leading-[1.1] sm:text-[2.6rem]">{t('showcase.title')}</h2>
        <p className="max-w-[52ch] text-lg text-frost">{t('showcase.lead')}</p>
      </div>

      <ul className="grid min-w-0 gap-4 sm:grid-cols-3">
        {cards.map((c) => (
          <li key={c.id} className="lv-config-card flex flex-col gap-3 rounded-panel border border-white/10 bg-black/30 p-5">
            <div className="flex items-center justify-between gap-2">
              <span className="font-display text-lg font-bold text-snow">{c.title}</span>
              {isExample ? <span className="lv-example-tag">{t('showcase.example')}</span> : null}
            </div>
            <span className="text-sm text-frost">{t('showcase.by', { author: c.author?.nick ?? t('showcase.example') })}</span>
            <span className="font-code text-xs text-ice">{t('showcase.downloads', { count: c.copies_count })}</span>
          </li>
        ))}
      </ul>

      <Link to="/showcase" className="text-ice underline-offset-4 hover:underline">
        {t('showcase.cta')}
      </Link>
    </section>
  )
}
