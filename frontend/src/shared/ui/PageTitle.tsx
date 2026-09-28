import type { ReactNode } from 'react'

/**
 * The canonical page heading. Every top-level page (except the landing hero and the
 * auth panes) uses this so titles share one size, weight and uppercase treatment.
 */
export function PageTitle({ children, lead }: { children: ReactNode; lead?: ReactNode }) {
  return (
    <header className="flex flex-col gap-2">
      <h1 className="font-display text-3xl font-black uppercase leading-tight tracking-[0.01em] text-snow">
        {children}
      </h1>
      {lead ? <p className="max-w-[60ch] text-frost">{lead}</p> : null}
    </header>
  )
}
