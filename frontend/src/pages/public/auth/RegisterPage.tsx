import { useTranslation } from 'react-i18next'
import { RegisterForm } from '@features/auth/forms/RegisterForm'
import { AuthScreen } from './AuthScreen'

const HINTS = ['one', 'two', 'three', 'four'] as const

export default function RegisterPage() {
  const { t } = useTranslation('auth')
  return (
    <AuthScreen mode="register" title={t('register.title')} hints={HINTS.map((k) => t(`register.hints.${k}`))}>
      <RegisterForm />
    </AuthScreen>
  )
}
