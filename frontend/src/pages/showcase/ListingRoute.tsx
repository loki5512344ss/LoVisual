import { lazy, Suspense } from 'react'

const ListingPage = lazy(() => import('./ListingPage'))

export default function ListingRoute() {
  return (
    <Suspense fallback={<div className="px-6 py-16 text-frost">…</div>}>
      <ListingPage />
    </Suspense>
  )
}
