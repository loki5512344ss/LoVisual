import type { TFunction } from '@shared/i18n/react'

const RU_PLURAL = new Intl.PluralRules('ru')

/** Russian pluralization for "Скопировали N раз/раза/раз", exported for a focused unit test. */
export function copiesLabel(count: number): string {
  const form = RU_PLURAL.select(count)
  const word = form === 'one' ? 'раз' : form === 'few' ? 'раза' : 'раз'
  return `Скопировали ${count} ${word}`
}

export function copiesText(t: TFunction<'showcase'>, lang: string, count: number): string {
  if (lang === 'ru') return copiesLabel(count)
  const form = RU_PLURAL.select(count)
  const key = form === 'one' ? 'item.copiesOne' : form === 'few' ? 'item.copiesFew' : 'item.copiesMany'
  return t(key, { count })
}
