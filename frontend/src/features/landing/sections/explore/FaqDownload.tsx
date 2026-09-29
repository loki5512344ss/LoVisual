import { useTranslation } from 'react-i18next'
import { DOWNLOAD_URL, GITHUB_REPO } from '@features/download/api'
import '@features/landing/styles/landing.css'
import '@features/landing/styles/sections.css'

const KEYS = ['1', '2', '3', '4', '5'] as const

function DownloadIcon() {
  return (
    <svg viewBox="0 0 20 20" className="size-6" aria-hidden="true">
      <path d="M10 3v10m0 0-4-4m4 4 4-4M4 16h12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/** Plain FAQ list, then the page closes on a full-width download band in the page accent. */
export function FaqDownload() {
  const { t } = useTranslation('landing')
  const items = KEYS.map((k) => ({ q: t(`faq.q${k}`), a: t(`faq.a${k}`) }))

  return (
    <>
      <section aria-labelledby="faq-title" className="grid gap-8 py-20 sm:py-24 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)] lg:gap-14">
        <h2 id="faq-title" className="text-[2rem] leading-[1.1] sm:text-[2.6rem]">{t('faq.title')}</h2>
        <div className="flex min-w-0 flex-col">
          {items.map((it) => (
            <details key={it.q} className="lv-faq-item group border-t border-white/10 last:border-b">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-5 text-lg font-semibold text-snow">
                {it.q}
                <span aria-hidden="true" className="lv-faq-marker" />
              </summary>
              <p className="max-w-[58ch] pb-5 leading-relaxed text-frost">{it.a}</p>
            </details>
          ))}
        </div>
      </section>

      <section aria-labelledby="download-title" className="lv-bleed lv-download-final">
        <div className="mx-auto flex max-w-[1120px] flex-col items-start gap-6 px-4 py-24 sm:px-6 sm:py-32">
          <h2 id="download-title" className="lv-download-title">{t('faq.downloadTitle')}</h2>
          <p className="max-w-[46ch] text-lg text-snow/85">{t('faq.downloadLead')}</p>
          <div className="flex flex-wrap items-center gap-x-6 gap-y-4">
            <a href={DOWNLOAD_URL} download className="lv-cta-primary lv-cta-lg">
              <DownloadIcon />
              {t('faq.downloadCta')}
            </a>
            <a
              href={`https://github.com/${GITHUB_REPO}/releases`}
              target="_blank"
              rel="noreferrer"
              className="text-snow underline underline-offset-4"
            >
              {t('faq.releaseLink')}
            </a>
          </div>
          <p className="text-sm text-frost">{t('faq.downloadNote')}</p>
        </div>
      </section>
    </>
  )
}
