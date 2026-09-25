import type { Cursor } from '../demo'
import type { CategoryId } from './modules'

const PATHS: Record<CategoryId, string> = {
  combat: 'M14.5 3.5 20.5 3.5 20.5 9.5 10 20 7 20 4 17 4 14ZM6 13.5 10.5 18M3.5 20.5 6.5 17.5',
  visuals: 'M2.5 12s3.5-6.5 9.5-6.5 9.5 6.5 9.5 6.5-3.5 6.5-9.5 6.5S2.5 12 2.5 12ZM12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z',
  player: 'M12 11.5a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4.5 20.5c.8-3.8 3.9-6 7.5-6s6.7 2.2 7.5 6',
  misc: 'M5 5h5v5H5ZM14 5h5v5h-5ZM5 14h5v5H5ZM14 14h5v5h-5Z',
}

export function CategoryIcon({ id }: { id: CategoryId }) {
  return (
    <svg viewBox="0 0 24 24" className="lv-gui-icon" aria-hidden="true">
      <path d={PATHS[id]} fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function Chevron({ open }: { open: boolean }) {
  return (
    <svg viewBox="0 0 16 16" className="lv-gui-chevron" data-open={open} aria-hidden="true">
      <path d="M4 6l4 4 4-4" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function LogoMark() {
  return (
    <svg viewBox="0 0 20 20" className="lv-gui-logo-mark" aria-hidden="true">
      <path d="M10 1.5 18.5 10 10 18.5 1.5 10Z" fill="var(--gui-accent)" />
      <path d="M10 5.5 14.5 10 10 14.5 5.5 10Z" fill="var(--gui-window-bg)" />
    </svg>
  )
}

export function SearchGlyph() {
  return (
    <svg viewBox="0 0 16 16" className="lv-gui-icon" aria-hidden="true">
      <circle cx="7" cy="7" r="4.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
      <path d="M10.5 10.5 14 14" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  )
}

/** The demo's fake mouse pointer; positioned in the window's own (untransformed) coordinates. */
export function DemoCursor({ cursor }: { cursor: Cursor }) {
  return (
    <div
      aria-hidden="true"
      className="lv-gui-cursor"
      data-visible={cursor.visible}
      data-pressed={cursor.pressed}
      style={{
        transform: `translate3d(${cursor.x}px, ${cursor.y}px, 0)`,
        transitionDuration: `${cursor.duration}ms, 200ms`,
      }}
    >
      <span className="lv-gui-cursor-ring" />
      <svg viewBox="0 0 20 24" width="20" height="24">
        <path
          d="M2 1.5v17.2l4.6-4.3 3 7 3.2-1.4-3-6.8h6.4Z"
          fill="#fff"
          stroke="#0b1012"
          strokeWidth="1.3"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  )
}
