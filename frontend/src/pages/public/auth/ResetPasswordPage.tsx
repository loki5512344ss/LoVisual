import { useState, type FormEvent } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { resetPassword } from '@features/auth/api'
import { describeError } from '@shared/api/errors'
import { Button } from '@shared/ui/Button'
import { Notice } from '@shared/ui/Notice'
import { TextField } from '@shared/ui/text-field/TextField'

/** /reset-password?token=...: consumes the email link and sets the new password. */
export default function ResetPasswordPage() {
  const { t } = useTranslation('auth')
  const { t: tc } = useTranslation('common')
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [badToken, setBadToken] = useState(false)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(undefined)
    try {
      await resetPassword(token, password)
      navigate('/login', { replace: true })
    } catch (err) {
      // 400 covers unknown/expired/used links: same message, next step is a
      // fresh reset request.
      if (err instanceof Error && 'status' in err && (err as { status: number }).status === 400) {
        setBadToken(true)
      } else {
        setError(describeError(tc, err))
      }
    } finally {
      setBusy(false)
    }
  }

  if (badToken) {
    return (
      <section className="mx-auto mt-16 w-full max-w-md">
        <h1 className="mb-4 text-2xl font-semibold text-frost">{t('reset.title')}</h1>
        <Notice tone="error">{t('reset.badToken')}</Notice>
        <Link to="/forgot-password" className="mt-4 inline-block text-ice hover:underline">
          {t('reset.goLogin')}
        </Link>
      </section>
    )
  }

  return (
    <section className="mx-auto mt-16 w-full max-w-md">
      <h1 className="mb-4 text-2xl font-semibold text-frost">{t('reset.title')}</h1>
      <form onSubmit={submit} className="flex w-full flex-col gap-4" noValidate>
        <TextField
          label={t('reset.passwordLabel')}
          hint={t('reset.passwordHint')}
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error ? <Notice tone="error">{error}</Notice> : null}
        <Button type="submit" busy={busy}>
          {t('reset.submit')}
        </Button>
      </form>
    </section>
  )
}
