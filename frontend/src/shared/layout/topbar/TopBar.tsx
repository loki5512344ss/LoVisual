import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { Link, useLocation } from 'react-router'
import { LanguageSwitch } from '../../i18n/LanguageSwitch'
import { MobileMenu } from './MobileMenu'
import { NavPill } from './NavPill'
import { AccountSlot, DownloadIcon, MenuIcon, Watermark } from './parts'

const MENU_ID = 'site-menu'

function useNavItems() {
  const { t } = useTranslation('common')
  return [
    { to: '/showcase', label: t('nav.showcase') },
    { to: '/themes', label: t('nav.themes') },
    { to: '/configs', label: t('nav.myConfigs') },
    { to: '/link', label: t('nav.linkGame') },
  ]
}

function useScrolled(threshold = 24): boolean {
  const [scrolled, setScrolled] = useState(false)
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > threshold)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [threshold])
  return scrolled
}

/**
 * A floating glass bar in the mod's own UI language: HUD-watermark logo, the [Modules | Settings]
 * pill as navigation, and a compact right cluster. It sits on the page's content column
 * (the bar overhangs by its own padding so the watermark lines up with the text below).
 */
export function TopBar() {
  const { t } = useTranslation(['common', 'download'])
  const scrolled = useScrolled()
  const [open, setOpen] = useState(false)
  const toggleRef = useRef<HTMLButtonElement>(null)
  const { pathname } = useLocation()
  const items = useNavItems()
  const close = useCallback(() => setOpen(false), [])

  // A route change (link in the sheet, back button) always closes the menu.
  const [lastPath, setLastPath] = useState(pathname)
  if (lastPath !== pathname) {
    setLastPath(pathname)
    setOpen(false)
  }

  return (
    <header className="fixed inset-x-0 top-0 z-40">
      <div
        className={`mx-auto w-full max-w-[1120px] px-2 transition-[padding] duration-300 sm:px-4 ${scrolled ? 'pt-2' : 'pt-3'}`}
      >
        <div
          data-scrolled={scrolled}
          className={`flex items-center gap-3 rounded-full border px-2 backdrop-blur-xl backdrop-saturate-150 transition-[height,background-color,border-color,box-shadow] duration-300 ${
            scrolled
              ? 'h-13 border-white/12 bg-[rgb(10_17_19/0.9)] shadow-[0_14px_40px_-16px_rgb(0_0_0/0.9),inset_0_1px_0_rgb(255_255_255/0.06)]'
              : 'h-14 border-white/8 bg-[rgb(12_20_22/0.6)] shadow-[0_10px_40px_-20px_rgb(0_0_0/0.7),inset_0_1px_0_rgb(255_255_255/0.05)]'
          }`}
        >
          <Watermark />
          <NavPill items={items} className="mx-auto hidden lg:block" />

          <div className="ml-auto flex items-center gap-2 lg:ml-0">
            <LanguageSwitch className="hidden lg:grid" />
            <Link
              to="/download"
              className="flex h-9 items-center gap-2 rounded-full bg-ice px-4 text-sm font-semibold text-abyss shadow-[0_0_20px_-4px_var(--page-accent-glow)] transition-[filter] hover:brightness-110 max-md:w-9 max-md:justify-center max-md:px-0"
            >
              <DownloadIcon />
              <span className="max-md:sr-only">{t('download:nav')}</span>
            </Link>
            <AccountSlot className="hidden lg:flex" />
            <button
              ref={toggleRef}
              type="button"
              aria-expanded={open}
              aria-controls={MENU_ID}
              aria-label={t('nav.menu')}
              onClick={() => setOpen((v) => !v)}
              className={`flex size-9 items-center justify-center rounded-full border transition-colors lg:hidden ${
                open ? 'border-white/25 bg-white/10 text-snow' : 'border-white/10 bg-black/30 text-frost hover:text-snow'
              }`}
            >
              <MenuIcon open={open} />
            </button>
          </div>
        </div>
        <div className="lg:hidden">
          <MobileMenu id={MENU_ID} items={items} open={open} onClose={close} toggleRef={toggleRef} />
        </div>
      </div>
    </header>
  )
}
