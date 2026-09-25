import { useTranslation } from 'react-i18next'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useSession } from './session'

export function RequireAuth() {
  const { t } = useTranslation('auth')
  const { status } = useSession()
  const location = useLocation()
  if (status === 'loading') return <p className="text-frost">{t('checkingSession')}</p>
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
