import { useEffect, useRef, useState } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { parseArgb, type ThemeEntry } from '../../types'
import { fromProfileJson, toProfileJson, toShareHash } from '../model/codec'

type Props = { draft: ThemeEntry; onImport: (entry: ThemeEntry) => void }
/** Which button just worked (its label flips to the confirmation) or what to tell the player. */
type Status = { done?: 'copy' | 'share'; message?: string; error?: boolean }

/** Dark or light ink for text sitting on the theme's accent, whichever reads better. */
function inkOn(accent: string): string {
  const { r, g, b } = parseArgb(accent)
  const luma = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255
  return luma > 0.55 ? '#0d1416' : '#fff'
}

/** The panel footer: get the theme out (clipboard, file, link) or bring one in from a file. */
export function ExportActions({ draft, onImport }: Props) {
  const { t } = useTranslation('themes')
  const [status, setStatus] = useState<Status>({})
  const fileRef = useRef<HTMLInputElement>(null)
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined)

  useEffect(() => () => clearTimeout(timer.current), [])

  const show = (next: Status, ms = 2000) => {
    setStatus(next)
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setStatus({}), ms)
  }

  const toClipboard = async (text: string, done: 'copy' | 'share') => {
    try {
      if (!navigator.clipboard) throw new Error('no clipboard')
      await navigator.clipboard.writeText(text)
      show({ done, message: t(done === 'copy' ? 'editor.copied' : 'editor.shared') })
    } catch {
      show({ message: t('editor.clipboardError'), error: true }, 6000)
    }
  }

  const download = () => {
    const url = URL.createObjectURL(new Blob([toProfileJson(draft)], { type: 'application/json' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `${draft.id}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  const onFile = (file?: File) => {
    if (!file) return
    file.text().then((text) => {
      const result = fromProfileJson(text)
      if ('error' in result) {
        show({ message: t('editor.importError'), error: true }, 6000)
        return
      }
      onImport(result)
      show({ message: t('editor.imported', { name: result.name }) })
    })
  }

  return (
    <div className="lv-editor-foot">
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-2">
        <button
          type="button"
          onClick={() => toClipboard(toProfileJson(draft), 'copy')}
          className="lv-editor-act"
          data-primary="true"
          data-done={status.done === 'copy'}
          style={{ color: inkOn(draft.theme.accent) }}
        >
          {status.done === 'copy' ? t('editor.copied') : t('editor.copy')}
        </button>
        <button type="button" onClick={download} className="lv-editor-act">
          {t('editor.download')}
        </button>
        <button type="button" onClick={() => fileRef.current?.click()} className="lv-editor-act">
          {t('editor.import')}
        </button>
        <button
          type="button"
          onClick={() => toClipboard(`${location.origin}${location.pathname}#t=${toShareHash(draft)}`, 'share')}
          className="lv-editor-act"
          data-done={status.done === 'share'}
        >
          {status.done === 'share' ? t('editor.shared') : t('editor.share')}
        </button>
        <input
          ref={fileRef}
          type="file"
          accept="application/json,.json"
          className="hidden"
          onChange={(e) => {
            onFile(e.target.files?.[0])
            e.target.value = '' // so picking the same file again still fires
          }}
        />
      </div>
      {/* Copy and share confirm on the button itself; the line only speaks for them to screen readers. */}
      <p role="status" className={`lv-editor-status ${status.done ? 'sr-only' : ''}`} data-error={status.error ?? false}>
        {status.message ?? ''}
      </p>
    </div>
  )
}
