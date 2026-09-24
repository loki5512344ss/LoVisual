import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { expect, test } from 'vitest'

test('router renders a route', () => {
  const router = createMemoryRouter([{ path: '/', element: <h1>LoVisual</h1> }])
  render(<RouterProvider router={router} />)
  expect(screen.getByRole('heading', { name: 'LoVisual' })).toBeInTheDocument()
})
