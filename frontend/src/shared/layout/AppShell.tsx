import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router'
import { KonamiEffect } from '../easter/KonamiEffect'
import { Footer } from './Footer'
import { TopBar } from './topbar/TopBar'

export function AppShell() {
  const { t } = useTranslation('common')
  return (
    <div className="flex min-h-dvh flex-col">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-2 focus:z-50 focus:rounded-md focus:bg-slate focus:px-3 focus:py-2"
      >
        {t('skipToContent')}
      </a>
      <KonamiEffect />
      <TopBar />
      <main id="main" className="mx-auto flex w-full max-w-[1120px] flex-1 flex-col px-4 pt-(--topbar-h) sm:px-6">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
