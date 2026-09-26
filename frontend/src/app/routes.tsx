import type { RouteObject } from 'react-router'
import { RequireAuth } from '../features/auth/RequireAuth'
import { AppShell } from '../shared/layout/AppShell'
import HomePage from '../pages/public/HomePage'
import LoginPage from '../pages/public/LoginPage'
import RegisterPage from '../pages/public/RegisterPage'
import ProfilePage from '../pages/public/ProfilePage'
import NotFoundPage from '../pages/public/NotFoundPage'
// Code-split: the download page pulls in react-markdown, so it loads on demand.
import DownloadRoute from '../pages/download/DownloadRoute'
import AccountPage from '../pages/app/AccountPage'
import LinkPage from '../pages/app/LinkPage'
import ThemesRoute from '../pages/themes/ThemesRoute'
import ConfigsPage from '../pages/app/ConfigsPage'
import ShowcaseRoute from '../pages/showcase/ShowcaseRoute'
import ListingRoute from '../pages/showcase/ListingRoute'

export const routes: RouteObject[] = [
  {
    element: <AppShell />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/download', element: <DownloadRoute /> },
      { path: '/themes', element: <ThemesRoute /> },
      { path: '/u/:id', element: <ProfilePage /> },
      { path: '/showcase', element: <ShowcaseRoute /> },
      { path: '/showcase/:id', element: <ListingRoute /> },
      {
        element: <RequireAuth />,
        children: [
          { path: '/link', element: <LinkPage /> },
          { path: '/account', element: <AccountPage /> },
          { path: '/configs', element: <ConfigsPage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
