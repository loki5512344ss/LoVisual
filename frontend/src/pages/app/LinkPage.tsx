import { useTranslation } from 'react-i18next'
import { LinkDeviceForm } from '../../features/device-link/LinkDeviceForm'
import { PageTitle } from '../../shared/layout/PageTitle'

const STEPS = ['one', 'two', 'three'] as const

export default function LinkPage() {
  const { t } = useTranslation('deviceLink')
  return (
    <section className="flex flex-col gap-8 py-12">
      <PageTitle lead={t('page.lead')}>{t('page.title')}</PageTitle>

      <ol className="flex flex-col gap-3 sm:flex-row sm:gap-4">
        {STEPS.map((step, i) => (
          <li
            key={step}
            className="flex flex-1 items-start gap-3 rounded-(--radius-panel) border border-shoal bg-slate/60 px-4 py-3"
          >
            <span
              aria-hidden="true"
              className="flex h-6 w-6 flex-none items-center justify-center rounded-full bg-ice font-code text-sm font-bold text-abyss"
            >
              {i + 1}
            </span>
            <p className="text-sm text-frost">{t(`steps.${step}`)}</p>
          </li>
        ))}
      </ol>

      <LinkDeviceForm />
    </section>
  )
}
