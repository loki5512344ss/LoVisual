import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { DownloadButton } from '../../features/download/DownloadButton'
import { ReleaseNotes } from '../../features/download/ReleaseNotes'
import { GITHUB_REPO, fetchReleases, releaseSize } from '../../features/download/api'

// Requirement versions mirror mod/gradle.properties (the mod's build source of truth).
const REQUIREMENTS = [
  { key: 'reqMc', value: '26.2' },
  { key: 'reqLoader', value: '0.19.4+' },
  { key: 'reqApi', value: '0.152.2+26.2' },
] as const

export default function DownloadPage() {
  const { t } = useTranslation('download')
  const { data } = useQuery({ queryKey: ['releases'], queryFn: fetchReleases, staleTime: 10 * 60 * 1000 })
  const latest = data?.[0]
  const steps = ['1', '2', '3'] as const

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-12 px-6 py-16">
      <header className="flex flex-col gap-4">
        <h1 className="font-display text-4xl font-black uppercase text-snow">{t('page.title')}</h1>
        <p className="text-frost">{t('page.lead')}</p>
        <DownloadButton
          size="lg"
          className="mt-2"
          version={latest ? latest.tag_name.replace(/^v/, '') : undefined}
          bytes={latest ? releaseSize(latest) : undefined}
        />
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
            href={`https://github.com/${GITHUB_REPO}/releases`}
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
