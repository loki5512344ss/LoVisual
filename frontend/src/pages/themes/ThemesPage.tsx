import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ThemeEditor } from '@features/themes/editor/ThemeEditor'
import { fromShareHash } from '@features/themes/editor/model/codec'
import { PRESETS } from '@features/themes/presets.generated'
import { PageTitle } from '@shared/layout/PageTitle'
import '@features/themes/editor/editor.css'

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
        <PageTitle lead={t('editor.lead')}>{t('editor.title')}</PageTitle>
      }
    />
  )
}
