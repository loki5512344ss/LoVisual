import { useTranslation } from 'react-i18next'
import { RegisterForm } from '../../features/auth/forms/RegisterForm'
import { AuthSplit } from './auth/AuthSplit'

export default function RegisterPage() {
  const { t } = useTranslation('auth')
  return (
    <AuthSplit title={t('register.title')}>
      <p className="mb-4 max-w-sm text-frost">{t('register.subtitle')}</p>
      <RegisterForm />
    </AuthSplit>
  )
}
