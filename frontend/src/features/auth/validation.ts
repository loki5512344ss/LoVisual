export function validateEmail(email: string): string | undefined {
  const v = email.trim()
  if (!v) return 'Введи почту.'
  if (v.length > 254 || !/^[^@\s]+@[^@\s]+$/.test(v)) return 'Похоже, в почте опечатка.'
}

export function validatePassword(password: string): string | undefined {
  if ([...password].length < 8) return 'Минимум 8 символов.'
  if (new TextEncoder().encode(password).length > 256) return 'Слишком длинный пароль.'
}

export function validateNick(nick: string): string | undefined {
  const len = [...nick.trim()].length
  if (len === 0) return 'Придумай ник.'
  if (len > 32) return 'Не длиннее 32 символов.'
}
