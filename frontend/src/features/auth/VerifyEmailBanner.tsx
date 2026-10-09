import { useState } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { Notice } from '@shared/ui/Notice'
import { resendVerification } from './api'
import { useSession } from './session/useSession'

/**
 * The "confirm your email" strip: shown under the top bar for every
 * signed-in visitor with an unconfirmed address, because device linking
 * (and later addon publishing) stays locked until the email is verified.
 */
export function VerifyEmailBanner() {
  const { t } = useTranslation('auth')
  const { status, me } = useSession()
  const [state, setState] = useState<'idle' | 'busy' | 'sent' | 'failed'>('idle')

  if (status !== 'authenticated' || !me || me.email_verified) return null

  async function resend() {
    setState('busy')
    try {
      await resendVerification()
      setState('sent')
    } catch {
      setState('failed')
    }
  }

  return (
    <div className="mb-4">
      <Notice tone="info">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
          <span>{t('banner.text', { email: me.email })}</span>
          <button
            type="button"
            onClick={resend}
            disabled={state === 'busy'}
            className="rounded-ctl border border-shoal px-2 py-1 text-sm text-frost transition-colors hover:border-ice hover:text-ice disabled:opacity-50"
          >
            {t('banner.resend')}
          </button>
        </div>
        {state === 'sent' ? <p className="mt-2 text-ice">{t('banner.resent')}</p> : null}
        {state === 'failed' ? <p className="mt-2 text-ember">{t('banner.failed')}</p> : null}
      </Notice>
    </div>
  )
}
