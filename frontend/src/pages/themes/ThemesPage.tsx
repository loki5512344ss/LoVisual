import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ThemeEditor } from '../../features/themes/editor/ThemeEditor'
import { fromShareHash } from '../../features/themes/editor/model/codec'
import { PRESETS } from '../../features/themes/presets.generated'
import '../../features/themes/editor/editor.css'

/** `/themes` — pick a preset (or load one from a `#t=` share link) and edit it live. */
export default function ThemesPage() {
  const { t } = useTranslation('themes')

  const initial = useMemo(() => {
    const shared = /#t=([^&]+)/.exec(window.location.hash)?.[1]
    if (shared) {
      const loaded = fromShareHash(shared)
      if (!('error' in loaded)) return loaded
    }
    return PRESETS[0]
  }, [])

  return (
    <ThemeEditor
      initial={initial}
      intro={
        <header className="flex flex-col gap-1">
          <h1 className="font-display text-3xl font-black uppercase text-snow">{t('editor.title')}</h1>
          <p className="text-sm text-frost">{t('editor.lead')}</p>
        </header>
      }
    />
  )
}
