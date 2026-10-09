import type { ComponentType } from 'react'
import type { RouteObject } from 'react-router'
import { RequireAuth } from '@features/auth/RequireAuth'
import { AppShell } from '@shared/layout/AppShell'
import HomePage from '@pages/public/HomePage'
import NotFoundPage from '@pages/public/NotFoundPage'

// Adapts a page module's default export to the shape React Router's `lazy` expects.
function toRoute(m: { default: ComponentType }) {
  return { Component: m.default }
}

// Everything below is route-level code-split via React Router's `lazy` field:
// each page's JS only downloads when a visitor actually navigates to it, so the
// initial bundle only carries the app shell + the home page.
export const routes: RouteObject[] = [
  {
    element: <AppShell />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', lazy: () => import('@pages/public/auth/LoginPage').then(toRoute) },
      { path: '/register', lazy: () => import('@pages/public/auth/RegisterPage').then(toRoute) },
      { path: '/verify', lazy: () => import('@pages/public/auth/VerifyPage').then(toRoute) },
      { path: '/forgot-password', lazy: () => import('@pages/public/auth/ForgotPasswordPage').then(toRoute) },
      { path: '/reset-password', lazy: () => import('@pages/public/auth/ResetPasswordPage').then(toRoute) },
      { path: '/download', lazy: () => import('@pages/download/DownloadRoute').then(toRoute) },
      { path: '/themes', lazy: () => import('@pages/themes/ThemesRoute').then(toRoute) },
      { path: '/u/:id', lazy: () => import('@pages/public/ProfilePage').then(toRoute) },
      { path: '/showcase', lazy: () => import('@pages/showcase/ShowcaseRoute').then(toRoute) },
      { path: '/showcase/:id', lazy: () => import('@pages/showcase/ListingRoute').then(toRoute) },
      {
        element: <RequireAuth />,
        children: [
          { path: '/link', lazy: () => import('@pages/app/LinkPage').then(toRoute) },
          { path: '/account', lazy: () => import('@pages/app/AccountPage').then(toRoute) },
          { path: '/configs', lazy: () => import('@pages/app/ConfigsPage').then(toRoute) },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
