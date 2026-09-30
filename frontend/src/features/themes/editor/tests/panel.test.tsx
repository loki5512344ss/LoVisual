import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { PRESETS } from '../../presets.generated'
import { ThemeEditor } from '../ThemeEditor'
import { fromShareHash, toProfileJson } from '../model/codec'

const nameBox = () => screen.getByRole('textbox', { name: 'Название темы' })

test('undo and redo walk the history from the header buttons and from Ctrl+Z / Ctrl+Shift+Z', async () => {
  const user = userEvent.setup()
  render(<ThemeEditor initial={PRESETS[0]} />)
  const undo = screen.getByRole('button', { name: 'Отменить' })
  const redo = screen.getByRole('button', { name: 'Вернуть' })
  expect(undo).toBeDisabled()

  // A preset pick starts a fresh history; an edit (typing, one coalesced step) is what undo reverts.
  const edited = `${PRESETS[0].name} 2`
  await user.type(nameBox(), ' 2')
  expect(nameBox()).toHaveValue(edited)
  expect(undo).toBeEnabled()

  await user.click(undo)
  expect(nameBox()).toHaveValue(PRESETS[0].name)
  await user.click(redo)
  expect(nameBox()).toHaveValue(edited)

  await user.keyboard('{Control>}z{/Control}')
  expect(nameBox()).toHaveValue(PRESETS[0].name)
  await user.keyboard('{Control>}{Shift>}z{/Shift}{/Control}')
  expect(nameBox()).toHaveValue(edited)
})

test('reset names the theme it returns to and restores it', async () => {
  const user = userEvent.setup()
  render(<ThemeEditor initial={PRESETS[0]} />)
  await user.type(nameBox(), ' edited')
  await user.click(screen.getByRole('button', { name: `Сбросить до «${PRESETS[0].name}»` }))
  expect(nameBox()).toHaveValue(PRESETS[0].name)
})

test('renaming the theme updates the exported name and id', async () => {
  const user = userEvent.setup()
  render(<ThemeEditor initial={PRESETS[0]} />)
  await user.clear(nameBox())
  await user.type(nameBox(), 'My Night')
  await user.click(screen.getByRole('button', { name: 'Скопировать для мода' }))
  expect(JSON.parse(await navigator.clipboard.readText()).name).toBe('My Night')
})

test('copy puts the profile JSON on the clipboard and the button confirms', async () => {
  const user = userEvent.setup()
  render(<ThemeEditor initial={PRESETS[1]} />)
  await user.click(screen.getByRole('button', { name: 'Скопировать для мода' }))
  expect(await navigator.clipboard.readText()).toBe(toProfileJson(PRESETS[1]))
  expect(screen.getByRole('button', { name: 'Скопировано' })).toBeInTheDocument()
  expect(screen.getByRole('status')).toHaveTextContent('Скопировано')
})

test('share copies a link whose hash decodes back to the draft', async () => {
  const user = userEvent.setup()
  render(<ThemeEditor initial={PRESETS[4]} />)
  await user.click(screen.getByRole('button', { name: 'Скопировать ссылку' }))
  const link = await navigator.clipboard.readText()
  const hash = /#t=(.+)$/.exec(link)?.[1] ?? ''
  expect(fromShareHash(hash)).toMatchObject({ name: PRESETS[4].name, theme: PRESETS[4].theme })
  expect(screen.getByRole('button', { name: 'Ссылка скопирована' })).toBeInTheDocument()
})

test('a blocked clipboard says what to do instead', async () => {
  const user = userEvent.setup()
  render(<ThemeEditor initial={PRESETS[0]} />)
  vi.spyOn(navigator.clipboard, 'writeText').mockRejectedValueOnce(new Error('denied'))
  await user.click(screen.getByRole('button', { name: 'Скопировать для мода' }))
  expect(await screen.findByRole('status')).toHaveTextContent('Скачай .json')
  expect(screen.getByRole('button', { name: 'Скопировать для мода' })).toBeInTheDocument()
})

test('download saves the profile JSON as <id>.json', async () => {
  const user = userEvent.setup()
  const createUrl = vi.fn(() => 'blob:theme')
  Object.assign(URL, { createObjectURL: createUrl, revokeObjectURL: vi.fn() })
  const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
    expect(this.download).toBe(`${PRESETS[0].id}.json`)
  })
  render(<ThemeEditor initial={PRESETS[0]} />)
  await user.click(screen.getByRole('button', { name: 'Скачать .json' }))
  expect(click).toHaveBeenCalledOnce()
  const blob = (createUrl.mock.calls[0] as unknown as [Blob])[0]
  expect(await blob.text()).toBe(toProfileJson(PRESETS[0]))
  click.mockRestore()
})

test('loading a file imports a valid theme and explains a broken one', async () => {
  const user = userEvent.setup()
  const { container } = render(<ThemeEditor initial={PRESETS[0]} />)
  const input = container.querySelector<HTMLInputElement>('input[type="file"]')!

  // The import itself is applied inside startTransition (same deferred path as picking a
  // preset), so the name box can lag one tick behind the (urgent) status message -- wait for
  // the field's own value instead of asserting it in the same tick as the status text.
  await user.upload(input, new File([toProfileJson(PRESETS[5])], 'theme.json', { type: 'application/json' }))
  expect(await screen.findByRole('status')).toHaveTextContent(`Загружена тема «${PRESETS[5].name}»`)
  await waitFor(() => expect(nameBox()).toHaveValue(PRESETS[5].name))

  await user.upload(input, new File(['not json'], 'broken.json', { type: 'application/json' }))
  expect(await screen.findByText(/Это не тема LoVisual/)).toBeInTheDocument()
  await waitFor(() => expect(nameBox()).toHaveValue(PRESETS[5].name))
})
