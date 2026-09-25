import { lazy, Suspense } from 'react'

// Loaded on demand so react-markdown stays out of the landing bundle.
const DownloadPage = lazy(() => import('./DownloadPage'))

export default function DownloadRoute() {
  return (
    <Suspense fallback={<div className="px-6 py-16 text-frost">…</div>}>
      <DownloadPage />
    </Suspense>
  )
}
