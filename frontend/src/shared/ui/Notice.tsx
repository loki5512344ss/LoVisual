import type { ReactNode } from 'react'

export function Notice({ tone, children }: { tone: 'info' | 'error' | 'success'; children: ReactNode }) {
  const styles =
    tone === 'error' ? 'border-ember/60 text-ember' : tone === 'success' ? 'border-ice/60 text-ice' : 'border-shoal text-frost'
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`rounded-ctl border px-3 py-2 text-sm ${styles}`}>
      {children}
    </div>
  )
}
