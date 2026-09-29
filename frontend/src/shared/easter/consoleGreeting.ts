import { LOGO_ART } from './art.generated'

export function printConsoleGreeting(lang: string): void {
  const ru = lang.startsWith('ru')
  console.log(`%c${LOGO_ART}`, 'color:#34d399;font-family:monospace;font-weight:bold')
  console.log(
    `%c${ru ? 'Открыл DevTools? Значит, тебе тут рады.' : 'Opened DevTools? You are welcome here.'}`,
    'color:#eef5f3;font-size:13px',
  )
  console.log(
    `%c${ru ? 'Совет: попробуй ↑ ↑ ↓ ↓ ← → ← → B A.' : 'Hint: try ↑ ↑ ↓ ↓ ← → ← → B A.'}`,
    'color:#9fb3b0;font-size:11px',
  )
}
