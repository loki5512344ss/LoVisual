import { useEffect } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { useKonami } from './useKonami'

export function KonamiEffect() {
  const { t } = useTranslation('common')
  const active = useKonami()

  useEffect(() => {
    document.documentElement.classList.toggle('lv-konami', active)
    return () => document.documentElement.classList.remove('lv-konami')
  }, [active])

  if (!active) return null
  return (
    <div role="status" className="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-panel border border-ice/40 bg-slate/90 px-4 py-2 font-code text-sm text-ice">
      {t('konami')}
    </div>
  )
}
