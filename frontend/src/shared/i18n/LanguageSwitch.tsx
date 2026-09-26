import { useTranslation } from 'react-i18next'

const LANGS = ['ru', 'en'] as const

/** RU/EN as a two-segment pill (the in-game switch shape); the accent thumb slides to the active one. */
export function LanguageSwitch({ className = '' }: { className?: string }) {
  const { i18n, t } = useTranslation()
  const current = i18n.language === 'en' ? 'en' : 'ru'

  return (
    <div
      role="group"
      aria-label={t('languageSwitch.label')}
      className={`relative grid h-8 shrink-0 grid-cols-2 rounded-full border border-white/10 bg-black/30 p-0.5 text-xs font-semibold ${className}`}
    >
      <span
        aria-hidden="true"
        className={`absolute inset-y-0.5 left-0.5 w-[calc(50%-2px)] rounded-full bg-white/12 shadow-[inset_0_0_0_1px_rgb(255_255_255/0.12)] transition-transform duration-300 ease-out ${current === 'en' ? 'translate-x-full' : ''}`}
      />
      {LANGS.map((lng) => (
        <button
          key={lng}
          type="button"
          aria-pressed={current === lng}
          onClick={() => i18n.changeLanguage(lng)}
          className={`relative min-w-9 rounded-full px-2 transition-colors ${current === lng ? 'text-snow' : 'text-frost hover:text-snow'}`}
        >
          {t(`languageSwitch.${lng}`)}
        </button>
      ))}
    </div>
  )
}
