import type { TFunction } from 'i18next'

export function validateEmail(email: string, t: TFunction<'auth'>): string | undefined {
  const v = email.trim()
  if (!v) return t('validation.emailRequired')
  if (v.length > 254 || !/^[^@\s]+@[^@\s]+$/.test(v)) return t('validation.emailInvalid')
}

export function validatePassword(password: string, t: TFunction<'auth'>): string | undefined {
  if ([...password].length < 8) return t('validation.passwordShort')
  if (new TextEncoder().encode(password).length > 256) return t('validation.passwordTooLong')
}

export function validateNick(nick: string, t: TFunction<'auth'>): string | undefined {
  const len = [...nick.trim()].length
  if (len === 0) return t('validation.nickRequired')
  if (len > 32) return t('validation.nickTooLong')
}
