import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { SessionProvider } from '../features/auth/session'
import { ActiveThemeProvider } from '../features/themes/useActiveTheme'
import { routes } from './routes'

const queryClient = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000 } } })
const router = createBrowserRouter(routes)

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SessionProvider>
        <ActiveThemeProvider>
          <RouterProvider router={router} />
        </ActiveThemeProvider>
      </SessionProvider>
    </QueryClientProvider>
  )
}
