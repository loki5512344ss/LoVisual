import { useQuery } from '@tanstack/react-query'
import { useTranslation } from '@shared/i18n/react'
import { DownloadButton } from '@features/download/DownloadButton'
import { ReleaseNotes } from '@features/download/ReleaseNotes'
import { GITHUB_RELEASES_URL, fetchReleases, releaseSize } from '@features/download/api'
import { PageTitle } from '@shared/layout/PageTitle'

// Requirement versions mirror mod/gradle.properties (the mod's build source of truth).
const REQUIREMENTS = [
  { key: 'reqMc', value: '26.2' },
  { key: 'reqLoader', value: '0.19.4+' },
  { key: 'reqApi', value: '0.152.2+26.2' },
] as const

/**
 * The second download path, kept visible but locked: GitHub releases are
 * temporarily off (see TODO.md «Скачивание»), so the button is not a link and
 * carries a ribbon saying so.
 */
function GithubButtonOnHold() {
  const { t } = useTranslation('download')
  return (
    <span className="relative inline-flex">
      <span aria-disabled="true" className="lv-cta-quiet-lg cursor-not-allowed opacity-60">
        {t('page.githubButton')}
      </span>
      {/* Band sits on the top edge: the ribbon stays on top of the button
          without hiding its label. */}
      <span className="pointer-events-none absolute left-1/2 top-0 w-[calc(100%+1.5rem)] -translate-x-1/2 -translate-y-1/2 -rotate-[7deg] rounded-sm bg-ember px-3 py-1 text-center font-code text-xs font-bold uppercase text-abyss shadow-lg">
        {t('page.githubSoon')}
      </span>
    </span>
  )
}

export default function DownloadPage() {
  const { t } = useTranslation('download')
  const { data } = useQuery({ queryKey: ['releases'], queryFn: fetchReleases, staleTime: 10 * 60 * 1000 })
  const latest = data?.[0]
  const steps = ['1', '2', '3'] as const

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-12 px-6 py-16">
      <header className="flex flex-col gap-4">
        <PageTitle lead={t('page.lead')}>{t('page.title')}</PageTitle>
        <div className="mt-2 flex flex-wrap items-start gap-x-6 gap-y-5">
          <DownloadButton
            size="lg"
            version={latest ? latest.tag_name.replace(/^v/, '') : undefined}
            bytes={latest ? releaseSize(latest) : undefined}
          />
          <GithubButtonOnHold />
        </div>
      </header>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-xl font-bold uppercase text-snow">{t('page.reqTitle')}</h2>
        <ul className="grid gap-2 sm:grid-cols-3">
          {REQUIREMENTS.map((r) => (
            <li key={r.key} className="rounded-panel border border-white/10 bg-black/30 p-4">
              <span className="block text-xs text-frost">{t(`page.${r.key}`)}</span>
              <span className="font-code text-sm text-snow">{r.value}</span>
            </li>
          ))}
        </ul>
        <p className="text-sm text-frost">
          <strong className="text-snow">{t('page.reqOptionalTitle')}</strong> {t('page.reqOptional')}
        </p>
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-xl font-bold uppercase text-snow">{t('page.installTitle')}</h2>
        <ol className="flex flex-col gap-2">
          {steps.map((s) => (
            <li key={s} className="flex items-baseline gap-3">
              <span className="lv-step-num">{s}</span>
              <span className="text-snow/90">{t(`page.step${s}`)}</span>
            </li>
          ))}
        </ol>
      </section>

      <section className="flex flex-col gap-4">
        <div className="flex items-center justify-between gap-4">
          <h2 className="font-display text-xl font-bold uppercase text-snow">{t('notes.title')}</h2>
          <a
            href={GITHUB_RELEASES_URL}
            target="_blank"
            rel="noreferrer"
            className="text-sm text-ice underline-offset-4 hover:underline"
          >
            {t('notes.allReleases')}
          </a>
        </div>
        <ReleaseNotes />
      </section>
    </div>
  )
}
