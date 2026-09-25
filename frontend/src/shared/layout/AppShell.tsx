import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router'
import { Footer } from './Footer'
import { TopBar } from './TopBar'

export function AppShell() {
  const { t } = useTranslation('common')
  return (
    <>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-2 focus:z-10 focus:rounded-md focus:bg-slate focus:px-3 focus:py-2"
      >
        {t('skipToContent')}
      </a>
      <TopBar />
      <main id="main" className="mx-auto w-full max-w-[1120px] px-4 sm:px-6">
        <Outlet />
      </main>
      <Footer />
    </>
  )
}
