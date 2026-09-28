import { useTranslation } from 'react-i18next'
import { themeToCssVars } from '../../../clickgui/themeVars'
import { useActiveTheme } from '../../../themes/useActiveTheme'
import { DOWNLOAD_URL, GITHUB_REPO } from '../../../download/api'
import '../../styles/landing.css'

const KEYS = ['1', '2', '3', '4', '5'] as const

export function FaqDownload() {
  const { t } = useTranslation('landing')
  const { theme } = useActiveTheme()
  const items = KEYS.map((k) => ({ q: t(`faq.q${k}`), a: t(`faq.a${k}`) }))

  return (
    <section aria-labelledby="faq-title" style={themeToCssVars(theme)} className="grid gap-12 py-20 sm:py-28 lg:grid-cols-2 lg:gap-16">
      <div className="flex min-w-0 flex-col gap-6">
        <span className="font-code text-xs uppercase tracking-[0.2em] text-ice">{t('faq.eyebrow')}</span>
        <h2 id="faq-title" className="text-[2rem] leading-[1.1] sm:text-[2.6rem]">{t('faq.title')}</h2>
        <div className="flex flex-col gap-2">
          {items.map((it) => (
            <details key={it.q} className="lv-faq-item lv-glass">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-4 text-base font-semibold text-snow">
                {it.q}
                <span aria-hidden="true" className="lv-faq-marker" />
              </summary>
              <p className="mt-2 max-w-[52ch] text-sm leading-relaxed text-frost">{it.a}</p>
            </details>
          ))}
        </div>
      </div>

      <div className="lv-download-band lv-glass flex min-w-0 flex-col items-start justify-center gap-5 rounded-panel p-8 sm:p-10">
        <h3 className="text-[1.75rem] leading-tight sm:text-[2.1rem]">{t('faq.downloadTitle')}</h3>
        <p className="max-w-[42ch] text-lg text-frost">{t('faq.downloadLead')}</p>
        <a href={DOWNLOAD_URL} download className="lv-cta-primary lv-cta-lg">
          {t('faq.downloadCta')}
        </a>
        <p className="font-code text-xs text-frost">{t('faq.downloadNote')}</p>
        <a
          href={`https://github.com/${GITHUB_REPO}/releases`}
          target="_blank"
          rel="noreferrer"
          className="text-sm text-ice underline-offset-4 hover:underline"
        >
          {t('faq.releaseLink')}
        </a>
      </div>
    </section>
  )
}
