import { ApiError } from './client'

const BY_STATUS: Record<number, string> = {
  400: 'Проверь введённые данные.',
  401: 'Нужно войти в аккаунт.',
  403: 'Недостаточно прав.',
  404: 'Не найдено.',
  409: 'Уже существует.',
}

export function describeError(err: unknown, overrides: Partial<Record<number, string>> = {}): string {
  if (err instanceof ApiError) {
    if (err.status === 429) {
      return err.retryAfter
        ? `Слишком много попыток. Подожди ${err.retryAfter} с.`
        : 'Слишком много попыток. Подожди немного.'
    }
    if (err.status >= 500) return 'Сервер не ответил. Попробуй через минуту.'
    return overrides[err.status] ?? BY_STATUS[err.status] ?? 'Проверь введённые данные.'
  }
  return 'Нет связи с сервером. Проверь интернет и попробуй ещё раз.'
}
