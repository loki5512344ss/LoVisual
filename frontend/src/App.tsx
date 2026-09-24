import { createBrowserRouter, RouterProvider } from 'react-router'

const router = createBrowserRouter([{ path: '/', element: <h1>LoVisual</h1> }])

export default function App() {
  return <RouterProvider router={router} />
}
