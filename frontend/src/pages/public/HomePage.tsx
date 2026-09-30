import { lazy, Suspense } from 'react'
import { useIdleReady } from '@features/landing/effects/useIdleReady'
import { Hero } from '@features/landing/sections/hero/Hero'
import { ThemeStrip } from '@features/landing/sections/hero/ThemeStrip'

const LowerSections = lazy(() => import('@features/landing/LowerSections'))

export default function HomePage() {
  const ready = useIdleReady()
  return (
    <>
      <Hero />
      <ThemeStrip />
      <div className="lv-lower" style={ready ? undefined : { minHeight: '2400px' }}>
        <div aria-hidden="true" className="lv-lower-bg" />
        {ready && (
          <Suspense fallback={null}>
            <LowerSections />
          </Suspense>
        )}
      </div>
    </>
  )
}
