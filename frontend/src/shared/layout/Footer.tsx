import { useTranslation } from '@shared/i18n/react'
import { Link } from 'react-router'
import { GITHUB_REPO } from '@features/download/api'
import { LanguageSwitch } from '../i18n/LanguageSwitch'

export function Footer() {
  const { t } = useTranslation('common')
  const product = [
    { to: '/showcase', label: t('nav.showcase') },
    { to: '/configs', label: t('nav.myConfigs') },
    { to: '/themes', label: t('nav.themes') },
  ]
  const project: { label: string; to?: string; href?: string }[] = [
    { href: `https://github.com/${GITHUB_REPO}`, label: t('footer.github') },
    // In-site: the download page is where both download paths (ours + GitHub) live.
    { to: '/download', label: t('footer.download') },
  ]

  return (
    <footer className="mt-8 border-t border-shoal bg-slate">
      <div className="mx-auto grid w-full max-w-[1120px] gap-10 px-4 py-12 sm:px-6 md:grid-cols-[1.4fr_1fr_1fr]">
        <div className="flex min-w-0 flex-col gap-3">
          <Link to="/" className="font-display text-xl font-bold text-snow">LoVisual</Link>
          <p className="max-w-[38ch] text-sm text-frost">{t('footer.about')}</p>
          <LanguageSwitch className="self-start" />
        </div>

        <nav aria-label={t('footer.sectionProduct')} className="flex min-w-0 flex-col gap-2">
          <span className="font-display text-sm font-bold text-snow">{t('footer.sectionProduct')}</span>
          {product.map((i) => (
            <Link key={i.to} to={i.to} className="text-sm text-frost hover:text-snow">{i.label}</Link>
          ))}
        </nav>

        <nav aria-label={t('footer.sectionProject')} className="flex min-w-0 flex-col gap-2">
          <span className="font-display text-sm font-bold text-snow">{t('footer.sectionProject')}</span>
          {project.map((i) =>
            i.to ? (
              <Link key={i.label} to={i.to} className="text-sm text-frost hover:text-snow">{i.label}</Link>
            ) : (
              <a key={i.label} href={i.href} target="_blank" rel="noreferrer" className="text-sm text-frost hover:text-snow">
                {i.label}
              </a>
            ),
          )}
        </nav>
      </div>

      <div className="border-t border-shoal">
        <p className="mx-auto w-full max-w-[1120px] px-4 py-5 text-xs text-frost sm:px-6">{t('footer.disclaimer')}</p>
      </div>
    </footer>
  )
}
