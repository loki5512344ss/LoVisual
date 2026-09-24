import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { Button } from '../Button'
import { TextField } from '../TextField'
import { ShareCode } from '../ShareCode'
import { Notice } from '../Notice'

test('busy button is disabled and announces busy', () => {
  render(<Button busy>Сохранить</Button>)
  const b = screen.getByRole('button', { name: 'Сохранить' })
  expect(b).toBeDisabled()
  expect(b).toHaveAttribute('aria-busy', 'true')
})

test('text field links label and error', () => {
  render(<TextField label="Почта" error="Введи почту" />)
  const input = screen.getByLabelText('Почта')
  expect(input).toHaveAttribute('aria-invalid', 'true')
  expect(input).toHaveAccessibleDescription('Введи почту')
})

test('share code copies to clipboard and confirms', async () => {
  const user = userEvent.setup()
  const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue()
  render(<ShareCode code="7KQ3M9XA" />)
  expect(screen.getByText('7KQ3M9XA')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Скопировать' }))
  expect(writeText).toHaveBeenCalledWith('7KQ3M9XA')
  expect(await screen.findByText('Скопировано')).toBeInTheDocument()
})

test('error notice is an alert', () => {
  render(<Notice tone="error">Неверный код</Notice>)
  expect(screen.getByRole('alert')).toHaveTextContent('Неверный код')
})
