import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

export function HowItWorks() {
  const { t } = useTranslation('landing')
  const slots = [
    { name: t('howItWorks.slotNamePvp'), code: '7KQ3M9XA' },
    { name: t('howItWorks.slotNameStream'), code: 'R2WD8HNC' },
    { name: t('howItWorks.slotNameLazy'), code: 'L5TB4QZE' },
    { name: null, code: null },
  ]
  const linkSteps = [t('howItWorks.linkStep1'), t('howItWorks.linkStep2'), t('howItWorks.linkStep3')]

  return (
    <div className="flex flex-col gap-16 pb-20">
      <section className="grid items-center gap-8 md:grid-cols-2">
        <div className="flex flex-col gap-3">
          <h2 className="text-[1.5625rem] leading-tight">{t('howItWorks.slotsTitle')}</h2>
          <p className="max-w-[60ch] text-frost">{t('howItWorks.slotsDesc')}</p>
        </div>
        <ul className="grid grid-cols-2 gap-3" aria-label={t('howItWorks.slotsAriaLabel')}>
          {slots.map((slot, i) => (
            <li key={i} className="flex min-w-0 flex-col gap-1 rounded-panel border border-shoal bg-slate px-4 py-3">
              {slot.name ? (
                <>
                  <span className="truncate text-snow">{slot.name}</span>
                  <span className="font-code text-sm tracking-wider text-ice">{slot.code}</span>
                </>
              ) : (
                <span className="italic text-frost">{t('howItWorks.emptySlot')}</span>
              )}
            </li>
          ))}
        </ul>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-[1.5625rem] leading-tight">{t('howItWorks.linkTitle')}</h2>
        <ol className="flex max-w-[60ch] list-decimal flex-col gap-2 pl-6 marker:text-frost">
          {linkSteps.map((step) => (
            <li key={step} className="pl-1">
              {step}
            </li>
          ))}
        </ol>
      </section>

      <p className="max-w-[60ch] text-frost">
        {t('howItWorks.showcaseText')}{' '}
        <Link to="/showcase" className="text-ice underline-offset-4 hover:underline">
          {t('howItWorks.showcaseLink')}
        </Link>
      </p>
    </div>
  )
}
