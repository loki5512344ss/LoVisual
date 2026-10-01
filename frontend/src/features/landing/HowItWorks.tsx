import { useTranslation } from '@shared/i18n/react'
import { Link } from 'react-router'
import '@features/landing/styles/sections.css'

export function HowItWorks() {
  const { t } = useTranslation('landing')
  // Placeholder-style codes (not plausible real share codes) — this is an illustration of
  // the slot/code concept, not a real config, so it shouldn't look like fabricated data.
  const placeholderCode = t('howItWorks.slotCodePlaceholder')
  const slots = [
    { name: t('howItWorks.slotNamePvp'), code: placeholderCode },
    { name: t('howItWorks.slotNameStream'), code: placeholderCode },
    { name: t('howItWorks.slotNameLazy'), code: placeholderCode },
    { name: null, code: null },
  ]
  const linkSteps = [t('howItWorks.linkStep1'), t('howItWorks.linkStep2'), t('howItWorks.linkStep3')]

  return (
    <div className="flex flex-col gap-16 pb-20">
      <section className="grid items-center gap-8 md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
        <div className="flex flex-col gap-3">
          <h2 className="text-[1.5625rem] leading-tight">{t('howItWorks.slotsTitle')}</h2>
          <p className="max-w-[46ch] text-frost">{t('howItWorks.slotsDesc')}</p>
        </div>
        {/* The four slots drawn as a Minecraft hotbar; the first one is the "selected" slot. */}
        <ul className="grid grid-cols-2 gap-2 sm:grid-cols-4" aria-label={t('howItWorks.slotsAriaLabel')}>
          {slots.map((slot, i) => (
            <li
              key={i}
              data-selected={i === 0}
              data-empty={!slot.name}
              className="lv-hotbar-slot flex aspect-[4/3] min-w-0 flex-col justify-end gap-1 p-3 sm:aspect-square"
            >
              {slot.name ? (
                <>
                  <span className="truncate font-display text-lg font-bold text-snow">{slot.name}</span>
                  <span className="font-code text-xs tracking-wider text-ice">{slot.code}</span>
                </>
              ) : (
                <span className="text-frost">{t('howItWorks.emptySlot')}</span>
              )}
            </li>
          ))}
        </ul>
      </section>

      <section className="flex flex-col gap-6">
        <h2 className="text-[1.5625rem] leading-tight">{t('howItWorks.linkTitle')}</h2>
        <ol className="grid gap-6 md:grid-cols-3">
          {linkSteps.map((step, i) => (
            <li key={step} className="flex gap-4 border-t border-white/10 pt-4">
              <span aria-hidden="true" className="font-display text-3xl font-bold leading-none text-ice">{i + 1}</span>
              <span className="text-snow/90">{step}</span>
            </li>
          ))}
        </ol>
      </section>

      <p className="max-w-[60ch] text-frost">
        {t('howItWorks.showcaseText')}{' '}
        <Link to="/showcase" className="text-ice underline underline-offset-4">
          {t('howItWorks.showcaseLink')}
        </Link>
      </p>
    </div>
  )
}
