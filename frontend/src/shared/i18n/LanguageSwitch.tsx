import { useTranslation } from 'react-i18next'

const LANGS = ['ru', 'en'] as const

export function LanguageSwitch() {
  const { i18n, t } = useTranslation()
  const current = i18n.language

  return (
    <div className="flex items-center gap-1 text-sm text-frost" role="group" aria-label="Language">
      {LANGS.map((lng) => (
        <button
          key={lng}
          type="button"
          aria-pressed={current === lng}
          onClick={() => i18n.changeLanguage(lng)}
          className={`rounded-md px-2 py-1 transition-colors ${current === lng ? 'text-snow' : 'hover:text-snow'}`}
        >
          {t(`languageSwitch.${lng}`)}
        </button>
      ))}
    </div>
  )
}
