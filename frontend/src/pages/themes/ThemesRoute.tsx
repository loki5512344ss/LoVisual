import { lazy, Suspense } from 'react'

// Code-split so the editor and its CSS stay out of the landing bundle.
const ThemesPage = lazy(() => import('./ThemesPage'))

export default function ThemesRoute() {
  return (
    <Suspense fallback={<div className="px-6 py-16 text-frost">…</div>}>
      <ThemesPage />
    </Suspense>
  )
}
