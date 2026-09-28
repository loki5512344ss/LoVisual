import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import { Button } from '../../Button'
import { Notice } from '../../Notice'
import { TextField } from '../TextField'

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

test('error notice is an alert', () => {
  render(<Notice tone="error">Неверный код</Notice>)
  expect(screen.getByRole('alert')).toHaveTextContent('Неверный код')
})
