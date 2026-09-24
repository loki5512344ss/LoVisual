import type { ReactNode } from 'react'

export function Notice({ tone, children }: { tone: 'info' | 'error'; children: ReactNode }) {
  const styles = tone === 'error' ? 'border-ember/60 text-ember' : 'border-shoal text-frost'
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`rounded-md border px-3 py-2 text-sm ${styles}`}>
      {children}
    </div>
  )
}
