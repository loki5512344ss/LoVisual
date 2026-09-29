import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { browse } from '@features/showcase/api'
import '@features/landing/styles/sections.css'

/** The three most copied configs as a plain list with the copy count as the figure that matters. */
export function ShowcaseTeaser() {
  const { t } = useTranslation('landing')
  const { data, isError } = useQuery({
    queryKey: ['showcase', 'popular', 0, null],
    queryFn: () => browse('popular', 0),
  })
  const rows = !isError && data ? data.items.slice(0, 3) : []

  return (
    <section aria-labelledby="showcase-title" className="grid gap-10 py-20 sm:py-24 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)] lg:gap-14">
      <div className="flex min-w-0 flex-col items-start gap-4">
        <h2 id="showcase-title" className="text-[2rem] leading-[1.1] sm:text-[2.6rem]">{t('showcase.title')}</h2>
        <p className="max-w-[40ch] text-lg text-frost">{t('showcase.lead')}</p>
        <Link to="/showcase" className="mt-2 text-ice underline underline-offset-4">
          {t('showcase.cta')}
        </Link>
      </div>

      {rows.length === 0 ? (
        <p className="text-lg text-frost">{t('showcase.empty')}</p>
      ) : (
        <ul className="flex min-w-0 flex-col">
          {rows.map((c) => (
            <li key={c.id} className="lv-list-row grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4 py-5">
              <div className="flex min-w-0 flex-col gap-1">
                <span className="flex items-center gap-2 truncate font-display text-lg font-bold text-snow sm:text-xl">
                  {c.title}
                </span>
                {c.author ? <span className="text-sm text-frost">{t('showcase.by', { author: c.author.nick })}</span> : null}
              </div>
              <span className="flex flex-col items-end">
                <span className="font-display text-2xl font-bold text-ice">{c.copies_count}</span>
                <span className="text-xs text-frost">{t('showcase.copies')}</span>
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
