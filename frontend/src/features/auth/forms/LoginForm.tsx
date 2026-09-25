import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useNavigate } from 'react-router'
import { describeError } from '../../../shared/api/errors'
import { Button } from '../../../shared/ui/Button'
import { Notice } from '../../../shared/ui/Notice'
import { TextField } from '../../../shared/ui/TextField'
import { useSession } from '../session'

export function LoginForm() {
  const { t } = useTranslation('auth')
  const { t: tc } = useTranslation('common')
  const { signIn } = useSession()
  const navigate = useNavigate()
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/configs'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(undefined)
    try {
      await signIn(email.trim(), password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(describeError(tc, err, { 401: t('login.wrongCredentials') }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="flex w-full max-w-sm flex-col gap-4" noValidate>
      <TextField
        label={t('login.emailLabel')}
        type="email"
        autoComplete="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <TextField
        label={t('login.passwordLabel')}
        type="password"
        autoComplete="current-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      {error ? <Notice tone="error">{error}</Notice> : null}
      <Button type="submit" busy={busy}>
        {t('login.submit')}
      </Button>
      <p className="text-sm text-frost">
        {t('login.noAccount')}{' '}
        <Link to="/register" className="text-ice underline-offset-4 hover:underline">
          {t('login.createAccount')}
        </Link>
      </p>
    </form>
  )
}
