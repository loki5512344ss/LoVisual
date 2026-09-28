import { readFileSync } from 'node:fs'
import { expect, test } from 'vitest'

/**
 * Style invariants. These are what a visual QA pass keeps re-finding, because nothing
 * else stops them from regressing silently: a full-bleed section turning into a
 * horizontal scrollbar, and controls drifting onto a corner radius different from the
 * button sitting next to them. Reads the sources, so it is a rule about the CSS we ship,
 * not about one browser's computed output.
 */
// vitest runs from the package root, the same place `bun run build` does.
const read = (rel: string) => readFileSync(rel, 'utf8')

const indexCss = read('src/index.css')
const landingCss = read('src/features/landing/styles/landing.css')
const button = read('src/shared/ui/Button.tsx')
const textField = read('src/shared/ui/text-field/TextField.tsx')

test('full-bleed 100vw sections cannot produce a horizontal scrollbar', () => {
  expect(landingCss).toMatch(/\.lv-bleed\s*{[^}]*width:\s*100vw/)
  // Clipping body alone is not enough: <html> still pans by the width the 100vw
  // sections overshoot the client width by.
  expect(indexCss).toMatch(/html\s*{[^}]*overflow-x:\s*clip/)
  expect(indexCss).toMatch(/body\s*{[^}]*overflow-x:\s*clip/)
})

test('every control shares one corner radius', () => {
  expect(indexCss).toMatch(/--radius-ctl:\s*10px/)
  expect(button).toContain('rounded-ctl')
  expect(textField).toContain('rounded-ctl')
  // A hard-coded radius here is how the CTAs ended up at 9.6px while buttons sat at 6px.
  expect(landingCss).not.toMatch(/\.lv-cta[^{]*\{[^}]*border-radius:\s*(?!var\(--radius-ctl)\d/)
})
