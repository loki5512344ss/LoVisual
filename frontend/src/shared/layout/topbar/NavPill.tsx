import { useSharedThumb } from '@shared/motion/useSharedThumb'
import { useTranslation } from '@shared/i18n/react'
import { NavLink } from 'react-router'

export type NavItem = { to: string; label: string }

function NavThumb() {
  const ref = useSharedThumb<HTMLSpanElement>('topbar-nav-thumb', 320)
  return (
    <span
      ref={ref}
      data-testid="nav-indicator"
      aria-hidden="true"
      className="absolute inset-0 rounded-full border border-white/25 bg-[linear-gradient(90deg,var(--color-ice),color-mix(in_srgb,var(--color-ice)_72%,white))] shadow-[0_0_18px_-2px_var(--page-accent-glow)]"
    />
  )
}

/** The in-game [Modules | Settings] switch as site navigation: an accent thumb slides to the active route. */
export function NavPill({ items, className = '' }: { items: NavItem[]; className?: string }) {
  const { t } = useTranslation('common')
  return (
    <nav aria-label={t('nav.mainNav')} className={className}>
        <ul className="flex h-10 items-center rounded-full border border-white/10 bg-black/35 p-1 shadow-[inset_0_1px_2px_rgb(0_0_0/0.35)]">
          {items.map((item) => (
            <li key={item.to} className="h-full">
              <NavLink
                to={item.to}
                className={({ isActive }) =>
                  `relative flex h-full items-center rounded-full px-4 text-sm font-medium whitespace-nowrap transition-colors duration-200 ${
                    isActive ? 'text-abyss' : 'text-frost hover:text-snow'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    {isActive && <NavThumb />}
                    <span className="relative">{item.label}</span>
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
    </nav>
  )
}
