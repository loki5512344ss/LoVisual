import type { TFunction } from '@shared/i18n/react'
import { ApiError } from './client'

const BY_STATUS = {
  400: 'errors.badRequest',
  401: 'errors.unauthorized',
  403: 'errors.forbidden',
  404: 'errors.notFound',
  409: 'errors.conflict',
} as const

export function describeError(t: TFunction<'common'>, err: unknown, overrides: Partial<Record<number, string>> = {}): string {
  if (err instanceof ApiError) {
    if (err.status === 429) {
      return err.retryAfter
        ? t('errors.rateLimited', { seconds: err.retryAfter })
        : t('errors.rateLimitedGeneric')
    }
    if (err.status >= 500) return t('errors.serverError')
    const key = BY_STATUS[err.status as keyof typeof BY_STATUS]
    return overrides[err.status] ?? (key ? t(key) : t('errors.badRequest'))
  }
  return t('errors.network')
}
