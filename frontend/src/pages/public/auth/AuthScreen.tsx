import { MotionConfig, motion } from 'motion/react'
import { type ReactNode, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation } from 'react-router'
import { Backdrop } from '@features/clickgui/parts/Backdrop'
import '@features/clickgui/parts/clickgui.css'
import { themeToCssVars } from '@features/clickgui/themeVars'
import { useActiveTheme } from '@features/themes/useActiveTheme'
import './auth.css'

type Mode = 'login' | 'register'

/** Where RequireAuth (or a "log in to copy" link) was sending the visitor, if anywhere we can name. */
function nextStep(from: string | undefined): 'link' | 'configs' | 'account' | 'showcase' | undefined {
  if (!from) return undefined
  if (from.startsWith('/link')) return 'link'
  if (from.startsWith('/configs')) return 'configs'
  if (from.startsWith('/account')) return 'account'
  if (from.startsWith('/showcase')) return 'showcase'
}

function useClock(): string {
  const format = () => new Date().toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
  const [now, setNow] = useState(format)
  useEffect(() => {
    const id = window.setInterval(() => setNow(format()), 15_000)
    return () => window.clearInterval(id)
  }, [])
  return now
}

/**
 * The login and register pages as the mod's menu opened on an account tab: the ClickGui tab bar
 * switches between the two pages, one glass panel holds the form, and the hint row along the
 * bottom says what an account actually does (in the mod's own "key - action" voice).
 */
export function AuthScreen({ mode, title, hints, children }: { mode: Mode; title: string; hints: readonly string[]; children: ReactNode }) {
  const { t } = useTranslation('auth')
  const { theme } = useActiveTheme()
  const location = useLocation()
  const clock = useClock()
  // Carry the redirect target across the tab switch, so registering from /link still ends on /link.
  const state = location.state as { from?: string } | null
  const step = nextStep(state?.from)

  return (
    <MotionConfig reducedMotion="user">
      <section aria-labelledby="auth-title" className="lv-auth-screen" style={themeToCssVars(theme)}>
        <div className="lv-auth-world" aria-hidden="true">
          <Backdrop />
        </div>

        <div className="lv-auth-bar">
          <time className="lv-auth-clock" aria-hidden="true">
            {clock}
          </time>
          <nav aria-label={t('tabs.label')} className="lv-auth-tabs">
            {(['login', 'register'] as const).map((m) => (
              <Link
                key={m}
                to={`/${m}`}
                state={state}
                replace
                aria-current={m === mode ? 'page' : undefined}
                className="lv-auth-tab"
              >
                {m === mode ? (
                  <motion.span
                    layoutId="lv-auth-thumb"
                    className="lv-auth-thumb"
                    transition={{ duration: 0.38, ease: [0.22, 1, 0.36, 1] }}
                  />
                ) : null}
                <span className="lv-auth-tab-text">{t(`tabs.${m}`)}</span>
              </Link>
            ))}
          </nav>
        </div>

        <div className="lv-auth-panel">
          <header className="lv-auth-head">
            <h1 id="auth-title" className="lv-auth-title">
              {title}
            </h1>
          </header>
          <div className="lv-auth-body">
            {step ? <p className="lv-auth-next">{t(`next.${step}`)}</p> : null}
            {children}
          </div>
        </div>

        <ul className="lv-auth-hints" aria-label={t('hintsLabel')}>
          {hints.map((h) => (
            <li key={h}>{h}</li>
          ))}
        </ul>
      </section>
    </MotionConfig>
  )
}
