import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { I18nextProvider, useTranslation } from 'react-i18next'
import { expect, test } from 'vitest'
import { initI18n } from '@app/i18n'
import { LanguageSwitch } from '../LanguageSwitch'

function Probe() {
  const { t } = useTranslation('auth')
  return <p>{t('login.submit')}</p>
}

test('switching language re-renders strings and sets <html lang>', async () => {
  const user = userEvent.setup()
  const i18n = initI18n('ru')
  render(
    <I18nextProvider i18n={i18n}>
      <Probe />
      <LanguageSwitch />
    </I18nextProvider>,
  )
  expect(screen.getByText('Войти')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'EN' }))
  expect(screen.getByText('Log in')).toBeInTheDocument()
  expect(document.documentElement.lang).toBe('en')
})

test('russian plurals', () => {
  const i18n = initI18n('ru')
  expect(i18n.t('common:time.days', { count: 1 })).toBe('1 день')
  expect(i18n.t('common:time.days', { count: 3 })).toBe('3 дня')
  expect(i18n.t('common:time.days', { count: 5 })).toBe('5 дней')
})
