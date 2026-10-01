import { useQuery } from '@tanstack/react-query'
import { useTranslation } from '@shared/i18n/react'
import { Markdown } from '@shared/ui/Markdown'
import { fetchReleases } from './api'

/**
 * Changelog feed. Markdown bodies go through our own renderer, which builds React
 * elements and never raw HTML, so a `<script>` or `onerror=` in a release body stays inert text.
 */
export function ReleaseNotes() {
  const { t, i18n } = useTranslation('download')
  const { data, isError } = useQuery({
    queryKey: ['releases'],
    queryFn: fetchReleases,
    staleTime: 10 * 60 * 1000,
  })

  if (isError || !data || data.length === 0) {
    return (
      <p className="rounded-panel border border-white/10 bg-black/30 p-6 text-frost">{t('notes.empty')}</p>
    )
  }

  const date = new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium' })

  return (
    <ol className="flex flex-col gap-8">
      {data.map((r) => (
        <li key={r.tag_name} className="flex flex-col gap-2">
          <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
            <h3 className="font-display text-xl font-bold text-snow">{r.name || r.tag_name}</h3>
            <time className="font-code text-xs text-frost" dateTime={r.published_at}>
              {date.format(new Date(r.published_at))}
            </time>
          </div>
          <div className="lv-markdown text-[0.95rem] leading-relaxed text-snow/85">
            <Markdown>{r.body}</Markdown>
          </div>
        </li>
      ))}
    </ol>
  )
}
