import { useQuery } from '@tanstack/react-query'
import { useTranslation } from '@shared/i18n/react'
import { ReleaseCard } from '@features/download/ReleaseCard'
import { ReleaseNotes } from '@features/download/ReleaseNotes'
import { RELEASES_URL, SOURCE_URL, fetchReleases } from '@features/download/api'
import { PageTitle } from '@shared/layout/PageTitle'

// Requirement versions mirror mod/gradle.properties (the mod's build source of truth).
const REQUIREMENTS = [
  { key: 'reqMc', value: '26.2' },
  { key: 'reqLoader', value: '0.19.4+' },
  { key: 'reqApi', value: '0.152.2+26.2' },
] as const

const STEPS = ['1', '2', '3'] as const

export default function DownloadPage() {
  const { t } = useTranslation('download')
  const { data } = useQuery({ queryKey: ['releases'], queryFn: fetchReleases, staleTime: 10 * 60 * 1000 })

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-16 px-6 py-16">
      <section className="grid items-center gap-10 lg:grid-cols-[1.05fr_0.95fr]">
        <div className="flex flex-col gap-8">
          <PageTitle lead={t('page.lead')}>{t('page.title')}</PageTitle>
          <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-3 sm:gap-x-8">
            {REQUIREMENTS.map((r) => (
              <div key={r.key} className="contents">
                <dt className="text-frost">{t(`page.${r.key}`)}</dt>
                <dd className="font-code text-snow">{r.value}</dd>
              </div>
            ))}
            <dt className="text-frost">{t('page.reqOptionalTitle')}</dt>
            <dd className="text-snow/85">{t('page.reqOptional')}</dd>
          </dl>
        </div>
        <ReleaseCard release={data?.[0]} />
      </section>

      <section className="flex flex-col gap-6">
        <h2 className="font-display text-xl font-bold uppercase text-snow">{t('page.installTitle')}</h2>
        <ol className="grid gap-6 md:grid-cols-3">
          {STEPS.map((s) => (
            <li key={s} className="flex flex-col gap-3 border-t border-white/10 pt-4">
              <span className="lv-step-num">{s}</span>
              <span className="text-snow/90">{t(`page.step${s}`)}</span>
            </li>
          ))}
        </ol>
      </section>

      <section className="flex flex-col gap-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h2 className="font-display text-xl font-bold uppercase text-snow">{t('notes.title')}</h2>
          <span className="flex gap-5 text-sm">
            <a href={RELEASES_URL} target="_blank" rel="noreferrer" className="text-ice underline-offset-4 hover:underline">
              {t('notes.allReleases')}
            </a>
            <a href={SOURCE_URL} target="_blank" rel="noreferrer" className="text-ice underline-offset-4 hover:underline">
              {t('page.source')}
            </a>
          </span>
        </div>
        <ReleaseNotes />
      </section>
    </div>
  )
}
