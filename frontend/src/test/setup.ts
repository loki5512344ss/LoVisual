import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'
import { initI18n } from '../app/i18n'

initI18n('ru')

afterEach(() => cleanup())
afterEach(() => vi.unstubAllGlobals())

// jsdom has no canvas backend; effects treat a null context as "nothing to draw".
HTMLCanvasElement.prototype.getContext = (() => null) as typeof HTMLCanvasElement.prototype.getContext
