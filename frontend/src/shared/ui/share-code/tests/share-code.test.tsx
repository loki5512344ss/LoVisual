import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { ShareCode } from '../ShareCode'

test('share code copies to clipboard and confirms', async () => {
  const user = userEvent.setup()
  const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue()
  render(<ShareCode code="7KQ3M9XA" />)
  expect(screen.getByText('7KQ3M9XA')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Скопировать' }))
  expect(writeText).toHaveBeenCalledWith('7KQ3M9XA')
  expect(await screen.findByText('Скопировано')).toBeInTheDocument()
})
