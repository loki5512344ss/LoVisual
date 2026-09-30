import { useTranslation } from 'react-i18next'
import { LoginForm } from '@features/auth/forms/LoginForm'
import { AuthScreen } from './AuthScreen'

const HINTS = ['one', 'two', 'three'] as const

export default function LoginPage() {
  const { t } = useTranslation('auth')
  return (
    <AuthScreen mode="login" title={t('login.title')} hints={HINTS.map((k) => t(`login.hints.${k}`))}>
      <LoginForm />
    </AuthScreen>
  )
}
