import '@testing-library/jest-dom/vitest'
import 'fake-indexeddb/auto'
import { cleanup, configure } from '@testing-library/react'
import { afterEach, vi } from 'vitest'
import { initI18n } from '@app/i18n'

initI18n('ru')

// Route pages load via dynamic import() (see src/app/routes.tsx), and under a loaded
// test run (many jsdom workers in parallel) that first chunk resolution can take longer
// than the default 1000ms findBy/waitFor timeout. Give async queries more headroom.
configure({ asyncUtilTimeout: 5000 })

afterEach(() => cleanup())
afterEach(() => vi.unstubAllGlobals())

// jsdom has no canvas backend; effects treat a null context as "nothing to draw".
HTMLCanvasElement.prototype.getContext = (() => null) as typeof HTMLCanvasElement.prototype.getContext
