import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router'
import { describeError } from '@shared/api/errors'
import { Button } from '@shared/ui/Button'
import { Notice } from '@shared/ui/Notice'
import { TextField } from '@shared/ui/text-field/TextField'
import { useSession } from '../session'
import { validateEmail, validateNick, validatePassword } from '../validation'

export function RegisterForm() {
  const { t } = useTranslation('auth')
  const { t: tc } = useTranslation('common')
  const { signUp } = useSession()
  const navigate = useNavigate()
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/configs'
  const [email, setEmail] = useState('')
  const [nick, setNick] = useState('')
  const [password, setPassword] = useState('')
  const [emailError, setEmailError] = useState<string>()
  const [nickError, setNickError] = useState<string>()
  const [passwordError, setPasswordError] = useState<string>()
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(undefined)

    const emailErr = validateEmail(email, t)
    const nickErr = validateNick(nick, t)
    const passwordErr = validatePassword(password, t)
    setEmailError(emailErr)
    setNickError(nickErr)
    setPasswordError(passwordErr)
    if (emailErr || nickErr || passwordErr) return

    setBusy(true)
    try {
      await signUp(email.trim(), password, nick.trim())
      navigate(from, { replace: true })
    } catch (err) {
      setError(describeError(tc, err, { 409: t('register.emailTaken') }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="flex w-full max-w-sm flex-col gap-4" noValidate>
      <TextField
        label={t('register.emailLabel')}
        type="email"
        autoComplete="email"
        value={email}
        error={emailError}
        onChange={(e) => setEmail(e.target.value)}
      />
      <TextField
        label={t('register.nickLabel')}
        autoComplete="nickname"
        value={nick}
        error={nickError}
        hint={nickError ? undefined : t('register.nickHint')}
        onChange={(e) => setNick(e.target.value)}
      />
      <TextField
        label={t('register.passwordLabel')}
        type="password"
        autoComplete="new-password"
        value={password}
        error={passwordError}
        hint={passwordError ? undefined : t('register.passwordHint')}
        onChange={(e) => setPassword(e.target.value)}
      />
      {error ? <Notice tone="error">{error}</Notice> : null}
      <Button type="submit" busy={busy}>
        {t('register.submit')}
      </Button>
    </form>
  )
}
