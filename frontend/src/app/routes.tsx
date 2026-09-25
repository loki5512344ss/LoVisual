import type { RouteObject } from 'react-router'
import { RequireAuth } from '../features/auth/RequireAuth'
import { AppShell } from '../shared/layout/AppShell'
import HomePage from '../pages/public/HomePage'
import LoginPage from '../pages/public/LoginPage'
import RegisterPage from '../pages/public/RegisterPage'
import NotFoundPage from '../pages/public/NotFoundPage'
// Code-split: the download page pulls in react-markdown, so it loads on demand.
import DownloadRoute from '../pages/download/DownloadRoute'
import ThemesRoute from '../pages/themes/ThemesRoute'

export const routes: RouteObject[] = [
  {
    element: <AppShell />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/download', element: <DownloadRoute /> },
      { path: '/themes', element: <ThemesRoute /> },
      { element: <RequireAuth />, children: [] }, // Tasks 6–8 add /link, /account, /configs
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
