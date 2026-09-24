import { Navigate, Outlet, useLocation } from 'react-router'
import { useSession } from './session'

export function RequireAuth() {
  const { status } = useSession()
  const location = useLocation()
  if (status === 'loading') return <p className="text-frost">Проверяем вход…</p>
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
