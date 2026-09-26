import type { ReactNode } from 'react'
import type { Cursor } from '../demo'

/** The Lucide icons the mod ships in `assets/lovisual/svg/`, drawn the same way (2px stroke). */
const ICONS = {
  combat: (
    <>
      <polyline points="14.5 17.5 3 6 3 3 6 3 17.5 14.5" />
      <path d="M13 19 19 13M16 16l4 4M19 21l2-2" />
      <polyline points="14.5 6.5 18 3 21 3 21 6 17.5 9.5" />
      <path d="M5 14l4 4M7 17l-3 3M3 19l2 2" />
    </>
  ),
  player: (
    <>
      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </>
  ),
  visuals: (
    <>
      <path d="m17 14 3 3.3a1 1 0 0 1-.7 1.7H4.7a1 1 0 0 1-.7-1.7L7 14h-.3a1 1 0 0 1-.7-1.7L9 9h-.2A1 1 0 0 1 8 7.3L12 3l4 4.3a1 1 0 0 1-.8 1.7H15l3 3.3a1 1 0 0 1-.7 1.7H17Z" />
      <path d="M12 22v-3" />
    </>
  ),
  other: (
    <>
      <circle cx="12" cy="12" r="1" />
      <circle cx="19" cy="12" r="1" />
      <circle cx="5" cy="12" r="1" />
    </>
  ),
  check: <path d="M20 6 9 17l-5-5" />,
  search: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-4.2-4.2" />
    </>
  ),
  themes: (
    <>
      <path d="M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z" />
      <circle cx="13.5" cy="6.5" r=".5" fill="currentColor" />
      <circle cx="17.5" cy="10.5" r=".5" fill="currentColor" />
      <circle cx="6.5" cy="12.5" r=".5" fill="currentColor" />
      <circle cx="8.5" cy="7.5" r=".5" fill="currentColor" />
    </>
  ),
  layout: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M9 3v18M9 12h12M15 12v9" />
    </>
  ),
  account: (
    <>
      <path d="M11.5 15H7a4 4 0 0 0-4 4v2" />
      <path d="M21.378 16.626a1 1 0 0 0-3.004-3.004l-4.01 4.012a2 2 0 0 0-.506.854l-.837 2.87a.5.5 0 0 0 .62.62l2.87-.837a2 2 0 0 0 .854-.506z" />
      <circle cx="10" cy="7" r="4" />
    </>
  ),
  configs: (
    <>
      <path d="M10.3 20H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.98a2 2 0 0 1 1.69.9l.66 1.2A2 2 0 0 0 12 6h8a2 2 0 0 1 2 2v3.3" />
      <circle cx="18" cy="18" r="3" />
      <path d="m14.3 19.5.9-.4M15.2 16.9l-.9-.4M16.9 15.2l-.4-.9M16.9 20.8l-.4.9M19.1 15.2l.4-.9M19.5 21.7l-.4-.9M20.8 16.9l.9-.4M20.8 19.1l.9.4" />
    </>
  ),
} satisfies Record<string, ReactNode>

export type IconName = keyof typeof ICONS

export function Icon({ name, className = 'lv-gui-icon' }: { name: IconName; className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {ICONS[name]}
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
        <path d="M2 1.5v17.2l4.6-4.3 3 7 3.2-1.4-3-6.8h6.4Z" fill="#fff" stroke="#0b1012" strokeWidth="1.3" strokeLinejoin="round" />
      </svg>
    </div>
  )
}
