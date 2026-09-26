import { lazy, Suspense } from 'react'

// Loaded on demand: the showcase pages pull in extra query/routing glue not needed on the landing.
const ShowcasePage = lazy(() => import('./ShowcasePage'))

export default function ShowcaseRoute() {
  return (
    <Suspense fallback={<div className="px-6 py-16 text-frost">…</div>}>
      <ShowcasePage />
    </Suspense>
  )
}
