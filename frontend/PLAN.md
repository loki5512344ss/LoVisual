# Frontend: LoVisual site (Подсистема 1) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The account hub for LoVisual players: sign up / log in, link the game (device code), manage avatar and linked devices, keep 4 cloud config slots with share codes, and browse/copy configs from the public showcase. Plus a landing page that explains all of that in one screen.

**Architecture:** React 19 SPA (Vite 8), feature folders per `frontend/ARCHITECTURE.md`. Server state via TanStack Query; one fetch wrapper (`shared/api/client.ts`) owns the access token (memory only), silent refresh through the httpOnly `lv_refresh` cookie, and error normalization. Routing via React Router 8 data router. The browser only ever talks to `/api/*` (Vite dev proxy → gateway on :8080; same path behind nginx in prod).

**Tech Stack (installed and verified 2026-09-24, commit "chore(frontend): add router, react-query, fonts and vitest"):** React 19.2, Vite 8.3, TypeScript 7.0, Tailwind CSS 4.3 (`@tailwindcss/vite`), react-router 8.4 (`createBrowserRouter`/`createMemoryRouter`/`RouterProvider`/`Link`/`NavLink`/`Outlet`/`Navigate`/`useNavigate`/`useParams`/`useSearchParams` all exported from `react-router`), @tanstack/react-query 5.103, Vitest 5 + jsdom 30 + Testing Library (React 16, user-event 14, jest-dom 7 via `@testing-library/jest-dom/vitest`), fonts `@fontsource/comfortaa`, `@fontsource-variable/inter`, `@fontsource/iosevka`. Bun is the package manager (`bun add`, `bun run test`, `bun run build`, `bun run lint`).

## Global Constraints

- **Bilingual (ru default, en).** From Task 5A on, no user-facing string is hard-coded: every string goes through i18next (`useTranslation`), with Russian as the source dictionary and English kept key-for-key identical (enforced by types). Sentence case, active voice in both. Code identifiers/comments in English.
- **The mod is free.** No pricing, subscriptions, keys or HWID anywhere on the site. «Скачать» is the primary action everywhere.
- **Visually rich, not minimal** (owner's explicit requirement after reviewing the first landing): every public page has a real visual centerpiece that shows the product (live ClickGui/HUD replicas, particle/bloom effects matching the mod, procedural scenes), orchestrated motion via `motion` (respecting `prefers-reduced-motion`), and depth. Text-and-boxes pages are rejected.
- ≤250 lines per file, ≤4 files per folder (subfolders don't count). Tests live in a `tests/` subfolder of the feature/shared folder they cover (colocating `X.test.tsx` next to `X.tsx` would halve every folder's capacity under the 4-file rule) — Task 2 updates `ARCHITECTURE.md` accordingly.
- No `any`. DTOs typed from the backend contracts (`backend/PLAN.md`, `backend/gateway/PLAN.md` Tasks 3–6, `backend/configs-service/PLAN.md`).
- No barrel `index.ts` files. No business logic in `pages/`.
- Access token lives in memory only (never localStorage). The refresh token is never visible to JS.
- Every task: `bun run test`, `bun run build` (includes `tsc -b`) and `bun run lint` pass before commit.
- Commits: English, conventional, explicit paths (`git add frontend/...`), never anything under `mod/`. End with `Claude-Session: https://claude.ai/code/session_01F1M1Jic1wTSn4igUENynmZ`.
- `erasableSyntaxOnly` is on: no TS parameter properties, no enums — use explicit fields and string-literal unions.

---

## Design

**Subject:** LoVisual is a legit visuals mod for Minecraft (HUD, particles, themes). The site's audience is its players; its main job is managing cloud configs and linking the game, with the landing explaining those two ideas.

**Palette** — lifted from the mod's own default ClickGui theme (`mod/.../theme/presets/ThemePresets1.java`: window `#12191B`, header `#142226`, accent `#5CC8E7`, muted `#C6D2CD`), so the site feels like the in-game UI rather than a generic dark template:

| Token | Hex | Role |
|---|---|---|
| `abyss` | `#0D1416` | page background (teal-tinted, not neutral black) |
| `slate` | `#142226` | panels, top bar (the mod's header colour) |
| `shoal` | `#1F3238` | borders, inputs, raised controls |
| `frost` | `#9FB3B0` | secondary text (≈8:1 on abyss) |
| `snow` | `#EEF5F3` | primary text |
| `ice` | `#5CC8E7` | the one accent: primary actions, focus, share codes |
| `ember` | `#E8835A` | destructive actions and errors only |

**Type (revised 2026-09-25 — Cyrillic-first, commit "Cyrillic-first fonts"):** Unbounded Variable (wide geometric display face with full Cyrillic; headings, big numbers, logo) — `font-display`; Onest Variable (designed for Cyrillic; all body/UI text) — `font-sans`; JetBrains Mono Variable (has Cyrillic, unlike Iosevka; share codes, commands, chat lines, device codes) — `font-code`. Scale (≈1.25): 14 / 16 / 20 / 25 / 31 / 39 px, hero clamp(2.5rem, 6vw, 5rem). Body line length ≤ 70ch.

**Visual direction (revised 2026-09-25):** the site looks like the mod running. Signature pieces, all built in code (no stock images; no AI-image API keys are available):
- **ClickGui replica** — an HTML/CSS reproduction of the in-game ClickGui window (category column Combat / Visuals / Player / Misc, module cards with toggles and setting sliders, the theme's gradients and accent bloom), themable at runtime from the mod's real presets, with subtle 3D tilt following the pointer.
- **HUD replicas** — TargetHUD, Keybinds, Potions, Watermark, Armor widgets drawn like the mod's HUD, draggable with the pointer.
- **Particle field** — a `<canvas>` of soft glowing particles (the mod's AmbientParticles/bloom look) tinted by the active theme accent; pauses when off-screen or with reduced motion.
- **Voxel scene** — a procedural blocky landscape/sky (canvas or CSS 3D) as the backdrop the HUD sits on, so no copyrighted screenshots are needed. Real in-game screenshots from the owner can replace it later.
- Gradients are allowed where they reproduce the mod's own theme gradients; decorative gradient washes are still out.

**Layout:** Left-aligned, max width 1120px, 24px gutters (16px on phones). Top bar: logo left, three nav links (Витрина, Мои конфиги, Привязать игру), account button right. Panels (slate fill, 1px shoal border, 10px radius — the ClickGui window shape) are used only for things that *are* panels in the game: config slots, showcase entries, forms. Everything else sits directly on the page.

```
┌ LoVisual ─────── Витрина  Мои конфиги  Привязать игру ───── [Войти] ┐
│                                                                      │
│  Твои настройки LoVisual —                                           │
│  на любом компьютере                                                 │
│  Четыре облачных слота, код для друзей и витрина готовых конфигов.   │
│  [Создать аккаунт]  Смотреть витрину                                 │
│                                                                      │
│  ┌ chat ─────────────────────────────────────────┐                   │
│  │ %config load 7KQ3M9XA▌                        │  ← signature      │
│  │ [LoVisual] Конфиг «pvp» загружен              │                   │
│  └───────────────────────────────────────────────┘                   │
│                                                                      │
│  Четыре слота          ┌slot1┐┌slot2┐                                │
│  ...                   └─────┘└─────┘ (2×2)                          │
│  Привязка игры: 1 → 2 → 3 (real sequence, so numbered)               │
└──────────────────────────────────────────────────────────────────────┘
```

**Principles:**
1. Speak the game's language: codes and commands appear exactly as typed in-game, in Iosevka.
2. One signature moment: the hero's chat line types `%config load …` once on load, then the confirmation line appears. Nothing else animates on its own (`prefers-reduced-motion` → both lines shown immediately).
3. Panels only where the game has panels; no card grids as decoration.
4. Errors say what happened and what to do, never "Что-то пошло не так".

*Review against generic defaults:* dark + single accent is on the list of AI defaults. It is kept deliberately because it is the product's real in-game palette; the teal-tinted base (not #111), the ClickGui panel shape, Comfortaa, and the chat-line hero are what make it specific. Rejected alternatives: a stat-number hero ("10 000 игроков") — no real numbers exist; an all-caps eyebrow over each section — the section headings carry that job.

---

## File Structure

```
frontend/
  index.html                       # title, lang="ru", favicon
  src/
    main.tsx  App.tsx  routes.tsx  index.css
    test/        setup.ts  render.tsx  fetch.ts  smoke.test.tsx
    shared/
      types.ts                     # Me (used by auth + account)
      api/       client.ts  errors.ts  tests/client.test.ts
      ui/        Button.tsx  TextField.tsx  ShareCode.tsx  Notice.tsx  tests/ui.test.tsx
      layout/    AppShell.tsx  TopBar.tsx  tests/layout.test.tsx
    features/
      auth/      api.ts  session.tsx  RequireAuth.tsx  validation.ts
                 forms/ LoginForm.tsx  RegisterForm.tsx
                 tests/ session.test.tsx  forms.test.tsx
      landing/   CommandHero.tsx  HowItWorks.tsx  tests/landing.test.tsx
      device-link/ api.ts  LinkDeviceForm.tsx  tests/link.test.tsx
      account/   api.ts  components/ ProfileCard.tsx AvatarUpload.tsx DeviceList.tsx  tests/account.test.tsx
      configs/   api.ts  types.ts  components/ SlotGrid.tsx SlotPanel.tsx PublishForm.tsx  tests/configs.test.tsx
      showcase/  api.ts  types.ts  components/ ListingList.tsx ListingItem.tsx ListingDetail.tsx CopyToSlot.tsx  tests/showcase.test.tsx
    pages/
      public/    HomePage.tsx  LoginPage.tsx  RegisterPage.tsx  NotFoundPage.tsx
      app/       LinkPage.tsx  AccountPage.tsx  ConfigsPage.tsx
      showcase/  ShowcasePage.tsx  ListingPage.tsx
```

---

### Task 1: Tooling — DONE

Installed router/query/fonts/test tooling, Vitest config in `vite.config.ts` (jsdom, `src/test/setup.ts`), dev proxy `/api` → `http://localhost:8080` with `cookiePathRewrite: { '/auth': '/api/auth' }` (the backend scopes `lv_refresh` to `Path=/auth`), scaffold demo removed, smoke test. Nothing to do.

---

### Task 2: Design tokens, fonts, shared UI kit

**Files:**
- Modify: `src/index.css`, `index.html`, `frontend/ARCHITECTURE.md` (tests in `tests/` subfolders)
- Create: `src/shared/ui/{Button,TextField,ShareCode,Notice}.tsx`, `src/shared/ui/tests/ui.test.tsx`

**Interfaces (produces):**
- `Button` props: `ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'quiet' | 'danger'; busy?: boolean }` — `busy` disables and sets `aria-busy`.
- `TextField` props: `InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string; hint?: string }` — label wired via `useId`, error via `aria-invalid` + `aria-describedby`.
- `ShareCode` props: `{ code: string; label?: string }` — shows the code in Iosevka + a «Скопировать» button (clipboard, then «Скопировано» for 2s, announced via `aria-live="polite"`).
- `Notice` props: `{ tone: 'info' | 'error'; children: ReactNode }` — `role="alert"` for error, `role="status"` for info.

- [ ] **Step 1: Tokens and fonts — `src/index.css`**

```css
@import "tailwindcss";
@import "@fontsource-variable/inter";
@import "@fontsource/comfortaa/400.css";
@import "@fontsource/comfortaa/700.css";
@import "@fontsource/iosevka/400.css";
@import "@fontsource/iosevka/700.css";

@theme {
  --color-abyss: #0d1416;
  --color-slate: #142226;
  --color-shoal: #1f3238;
  --color-frost: #9fb3b0;
  --color-snow: #eef5f3;
  --color-ice: #5cc8e7;
  --color-ember: #e8835a;

  --font-display: "Comfortaa", ui-rounded, system-ui, sans-serif;
  --font-sans: "Inter Variable", system-ui, sans-serif;
  --font-code: "Iosevka", ui-monospace, monospace;

  --text-hero: 3.8125rem;
  --text-hero--line-height: 1.05;
  --radius-panel: 10px;
}

@layer base {
  html { color-scheme: dark; }
  body {
    @apply bg-abyss text-snow font-sans antialiased;
    min-height: 100dvh;
  }
  h1, h2, h3 { @apply font-display font-bold; }
  :focus-visible { outline: 2px solid var(--color-ice); outline-offset: 2px; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
}
```
`index.html`: `lang="ru"`, `<title>LoVisual — аккаунт и облачные конфиги</title>`, `<meta name="theme-color" content="#0d1416">`, `<meta name="description" content="Облачные конфиги, коды для друзей и витрина настроек для мода LoVisual.">`.

- [ ] **Step 2: Failing tests — `src/shared/ui/tests/ui.test.tsx`**

```tsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { Button } from '../Button'
import { TextField } from '../TextField'
import { ShareCode } from '../ShareCode'
import { Notice } from '../Notice'

test('busy button is disabled and announces busy', () => {
  render(<Button busy>Сохранить</Button>)
  const b = screen.getByRole('button', { name: 'Сохранить' })
  expect(b).toBeDisabled()
  expect(b).toHaveAttribute('aria-busy', 'true')
})

test('text field links label and error', () => {
  render(<TextField label="Почта" error="Введи почту" />)
  const input = screen.getByLabelText('Почта')
  expect(input).toHaveAttribute('aria-invalid', 'true')
  expect(input).toHaveAccessibleDescription('Введи почту')
})

test('share code copies to clipboard and confirms', async () => {
  const user = userEvent.setup()
  const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue()
  render(<ShareCode code="7KQ3M9XA" />)
  expect(screen.getByText('7KQ3M9XA')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Скопировать' }))
  expect(writeText).toHaveBeenCalledWith('7KQ3M9XA')
  expect(await screen.findByText('Скопировано')).toBeInTheDocument()
})

test('error notice is an alert', () => {
  render(<Notice tone="error">Неверный код</Notice>)
  expect(screen.getByRole('alert')).toHaveTextContent('Неверный код')
})
```
(`userEvent.setup()` installs a clipboard stub on `navigator.clipboard`, which is why `spyOn` works after it.)

Run: `bun run test` → FAIL.

- [ ] **Step 3: Implement the kit**

`Button.tsx`:
```tsx
import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'quiet' | 'danger'

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-ice text-abyss hover:bg-ice/90',
  quiet: 'bg-transparent text-snow border border-shoal hover:border-frost',
  danger: 'bg-transparent text-ember border border-ember/60 hover:border-ember',
}

export function Button({
  variant = 'primary',
  busy = false,
  disabled,
  className = '',
  type = 'button',
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; busy?: boolean }) {
  return (
    <button
      type={type}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      className={`inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${VARIANTS[variant]} ${className}`}
      {...rest}
    />
  )
}
```
`TextField.tsx`:
```tsx
import { useId, type InputHTMLAttributes } from 'react'

export function TextField({
  label,
  error,
  hint,
  className = '',
  ...rest
}: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string; hint?: string }) {
  const id = useId()
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <label htmlFor={id} className="text-sm text-frost">{label}</label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className="rounded-md border border-shoal bg-abyss px-3 py-2 text-snow placeholder:text-frost/60 focus:border-ice aria-invalid:border-ember"
        {...rest}
      />
      {error ? (
        <p id={`${id}-error`} className="text-sm text-ember">{error}</p>
      ) : hint ? (
        <p id={`${id}-hint`} className="text-sm text-frost">{hint}</p>
      ) : null}
    </div>
  )
}
```
`ShareCode.tsx`:
```tsx
import { useEffect, useState } from 'react'
import { Button } from './Button'

export function ShareCode({ code, label }: { code: string; label?: string }) {
  const [copied, setCopied] = useState(false)
  useEffect(() => {
    if (!copied) return
    const t = setTimeout(() => setCopied(false), 2000)
    return () => clearTimeout(t)
  }, [copied])

  async function copy() {
    await navigator.clipboard.writeText(code)
    setCopied(true)
  }

  return (
    <div className="flex flex-wrap items-center gap-3">
      {label ? <span className="text-sm text-frost">{label}</span> : null}
      <code className="font-code text-lg tracking-widest text-ice">{code}</code>
      <Button variant="quiet" onClick={copy}>Скопировать</Button>
      <span aria-live="polite" className="text-sm text-frost">{copied ? 'Скопировано' : ''}</span>
    </div>
  )
}
```
`Notice.tsx`:
```tsx
import type { ReactNode } from 'react'

export function Notice({ tone, children }: { tone: 'info' | 'error'; children: ReactNode }) {
  const styles = tone === 'error' ? 'border-ember/60 text-ember' : 'border-shoal text-frost'
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`rounded-md border px-3 py-2 text-sm ${styles}`}>
      {children}
    </div>
  )
}
```

- [ ] **Step 4: ARCHITECTURE.md**

Replace the "Тестирование" section's colocation rule with: tests live in a `tests/` subfolder inside the feature (or `shared/*`) folder they cover — still deleted together with the feature, but they don't eat into the 4-files-per-folder budget. Update the example tree accordingly.

- [ ] **Step 5: test/build/lint pass; commit**

```bash
git add frontend/src/index.css frontend/index.html frontend/ARCHITECTURE.md frontend/src/shared
git commit -m "feat(frontend): design tokens from the mod theme, fonts and shared UI kit"
```

---

### Task 3: API client + test helpers

**Files:**
- Create: `src/shared/api/client.ts`, `src/shared/api/errors.ts`, `src/shared/api/tests/client.test.ts`, `src/shared/types.ts`, `src/test/fetch.ts`, `src/test/render.tsx`

**Interfaces (produces):**
- `class ApiError extends Error { status: number; retryAfter: number | null }`.
- `createApiClient(baseUrl?: string, fetchImpl?: typeof fetch): ApiClient`, `ApiClient = { request<T>(path, options?: RequestOptions): Promise<T>; refresh(): Promise<boolean>; setAccessToken(t: string | null): void; hasAccessToken(): boolean }`, `RequestOptions = { method?: string; json?: unknown; body?: BodyInit; signal?: AbortSignal }`. Singleton `api = createApiClient()`.
- Behaviour: JSON body → `content-type: application/json`; token → `authorization: Bearer`; `credentials: 'include'` always; on 401 for a non-`/auth/*` path → one shared (single-flight) `POST /auth/refresh`, then one retry; non-2xx → `ApiError` with the backend's `{error}` message and `Retry-After` seconds; 204/empty body → `undefined`.
- `errors.ts`: `describeError(err: unknown, overrides?: Partial<Record<number, string>>): string` — Russian message per status (429 includes the wait: «Слишком много попыток. Подожди N с.»; network failure → «Нет связи с сервером. Проверь интернет и попробуй ещё раз.»; 5xx → «Сервер не ответил. Попробуй через минуту.»; 404 → «Не найдено.»; default 400 → «Проверь введённые данные.»).
- `shared/types.ts`: `Me = { id: string; email: string; display_nick: string; role: 'user' | 'admin'; avatar_url: string | null; created_at: string }`.
- `test/fetch.ts`: `mockFetch(routes: Record<string, (init: RequestInit) => Response | Promise<Response>>)` — key `"METHOD /path"` (path without `/api`, query ignored); unknown routes → 404; returns the `vi.fn` for call assertions; installs via `vi.stubGlobal('fetch', …)`. `json(body, status = 200, headers?)` helper.
- `test/render.tsx`: `renderApp(routes: RouteObject[], initialPath: string)` → wraps in a fresh `QueryClient` (`retry: false`) + `SessionProvider` (Task 4 — until then only QueryClient) + `createMemoryRouter`; returns Testing Library result + `router`.

- [ ] **Step 1: Test helpers first**

`src/test/fetch.ts`:
```ts
import { vi } from 'vitest'

type Handler = (init: RequestInit) => Response | Promise<Response>

export function json(body: unknown, status = 200, headers: Record<string, string> = {}): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json', ...headers },
  })
}

export function mockFetch(routes: Record<string, Handler>) {
  const fn = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const url = new URL(String(input), 'http://localhost')
    const key = `${(init.method ?? 'GET').toUpperCase()} ${url.pathname.replace(/^\/api/, '')}`
    const handler = routes[key]
    return handler ? handler(init) : json({ error: `no mock for ${key}` }, 404)
  })
  vi.stubGlobal('fetch', fn)
  return fn
}
```
Add `afterEach(() => vi.unstubAllGlobals())` to `src/test/setup.ts`.

- [ ] **Step 2: Failing client tests — `src/shared/api/tests/client.test.ts`**

```ts
import { expect, test } from 'vitest'
import { ApiError, createApiClient } from '../client'
import { json, mockFetch } from '../../../test/fetch'

test('sends json with bearer token and cookies', async () => {
  const fetch = mockFetch({ 'PUT /configs/1': () => json({ ok: true }) })
  const api = createApiClient()
  api.setAccessToken('tok')
  await api.request('/configs/1', { method: 'PUT', json: { a: 1 } })
  const [, init] = fetch.mock.calls[0]
  const headers = new Headers(init?.headers)
  expect(headers.get('authorization')).toBe('Bearer tok')
  expect(headers.get('content-type')).toBe('application/json')
  expect(init?.credentials).toBe('include')
  expect(init?.body).toBe('{"a":1}')
})

test('401 triggers one refresh and one retry', async () => {
  let calls = 0
  const fetch = mockFetch({
    'GET /me': () => (++calls === 1 ? json({ error: 'unauthorized' }, 401) : json({ id: 'x' })),
    'POST /auth/refresh': () => json({ access_token: 'new' }),
  })
  const api = createApiClient()
  await expect(api.request('/me')).resolves.toEqual({ id: 'x' })
  expect(fetch.mock.calls.filter(([u]) => String(u).endsWith('/auth/refresh'))).toHaveLength(1)
  expect(new Headers(fetch.mock.calls[2][1]?.headers).get('authorization')).toBe('Bearer new')
})

test('concurrent 401s share a single refresh', async () => {
  let refreshes = 0
  mockFetch({
    'GET /a': (init) => (new Headers(init.headers).get('authorization') ? json(1) : json({}, 401)),
    'GET /b': (init) => (new Headers(init.headers).get('authorization') ? json(2) : json({}, 401)),
    'POST /auth/refresh': () => { refreshes++; return json({ access_token: 't' }) },
  })
  const api = createApiClient()
  await Promise.all([api.request('/a'), api.request('/b')])
  expect(refreshes).toBe(1)
})

test('failed refresh surfaces the original 401 and clears the token', async () => {
  mockFetch({
    'GET /me': () => json({ error: 'unauthorized' }, 401),
    'POST /auth/refresh': () => json({ error: 'unauthorized' }, 401),
  })
  const api = createApiClient()
  api.setAccessToken('stale')
  await expect(api.request('/me')).rejects.toMatchObject({ status: 401 })
  expect(api.hasAccessToken()).toBe(false)
})

test('auth endpoints never trigger refresh', async () => {
  const fetch = mockFetch({ 'POST /auth/login': () => json({ error: 'unauthorized' }, 401) })
  await expect(createApiClient().request('/auth/login', { method: 'POST', json: {} })).rejects.toBeInstanceOf(ApiError)
  expect(fetch).toHaveBeenCalledTimes(1)
})

test('429 carries retry-after; 204 resolves undefined', async () => {
  mockFetch({
    'POST /auth/login': () => json({ error: 'too many requests' }, 429, { 'retry-after': '42' }),
    'DELETE /device/links/1': () => new Response(null, { status: 204 }),
  })
  const api = createApiClient()
  await expect(api.request('/auth/login', { method: 'POST' })).rejects.toMatchObject({ status: 429, retryAfter: 42 })
  await expect(api.request('/device/links/1', { method: 'DELETE' })).resolves.toBeUndefined()
})
```

- [ ] **Step 3: Implement `client.ts`**

```ts
export class ApiError extends Error {
  readonly status: number
  readonly retryAfter: number | null

  constructor(status: number, message: string, retryAfter: number | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.retryAfter = retryAfter
  }
}

export type RequestOptions = { method?: string; json?: unknown; body?: BodyInit; signal?: AbortSignal }

export type ApiClient = {
  request<T>(path: string, options?: RequestOptions): Promise<T>
  refresh(): Promise<boolean>
  setAccessToken(token: string | null): void
  hasAccessToken(): boolean
}

async function toError(res: Response): Promise<ApiError> {
  let message = `HTTP ${res.status}`
  try {
    const body: unknown = await res.json()
    if (body && typeof body === 'object' && 'error' in body && typeof body.error === 'string') message = body.error
  } catch {
    // non-JSON error body: keep the status text
  }
  const retry = Number(res.headers.get('retry-after'))
  return new ApiError(res.status, message, Number.isFinite(retry) && retry > 0 ? retry : null)
}

export function createApiClient(
  baseUrl = '/api',
  // Resolved at call time so tests can stub the global fetch.
  fetchImpl: typeof fetch = (input, init) => fetch(input, init),
): ApiClient {
  let accessToken: string | null = null
  let refreshing: Promise<boolean> | null = null

  async function refreshOnce(): Promise<boolean> {
    try {
      const res = await fetchImpl(`${baseUrl}/auth/refresh`, { method: 'POST', credentials: 'include' })
      if (!res.ok) {
        accessToken = null
        return false
      }
      const body = (await res.json()) as { access_token: string }
      accessToken = body.access_token
      return true
    } catch {
      return false
    }
  }

  function refresh(): Promise<boolean> {
    refreshing ??= refreshOnce().finally(() => {
      refreshing = null
    })
    return refreshing
  }

  function send(path: string, options: RequestOptions): Promise<Response> {
    const headers = new Headers()
    let body = options.body
    if (options.json !== undefined) {
      headers.set('content-type', 'application/json')
      body = JSON.stringify(options.json)
    }
    if (accessToken) headers.set('authorization', `Bearer ${accessToken}`)
    return fetchImpl(`${baseUrl}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body,
      credentials: 'include',
      signal: options.signal,
    })
  }

  async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    let res = await send(path, options)
    if (res.status === 401 && !path.startsWith('/auth/') && (await refresh())) {
      res = await send(path, options)
    }
    if (!res.ok) throw await toError(res)
    const text = await res.text()
    return (text ? JSON.parse(text) : undefined) as T
  }

  return {
    request,
    refresh,
    setAccessToken: (token) => {
      accessToken = token
    },
    hasAccessToken: () => accessToken !== null,
  }
}

export const api = createApiClient()
```

- [ ] **Step 4: `errors.ts`, `shared/types.ts`, `test/render.tsx`**

```ts
// errors.ts
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
      return err.retryAfter ? `Слишком много попыток. Подожди ${err.retryAfter} с.` : 'Слишком много попыток. Подожди немного.'
    }
    if (err.status >= 500) return 'Сервер не ответил. Попробуй через минуту.'
    return overrides[err.status] ?? BY_STATUS[err.status] ?? 'Запрос не прошёл. Попробуй ещё раз.'
  }
  return 'Нет связи с сервером. Проверь интернет и попробуй ещё раз.'
}
```
```ts
// shared/types.ts
export type Me = {
  id: string
  email: string
  display_nick: string
  role: 'user' | 'admin'
  avatar_url: string | null
  created_at: string
}
```
```tsx
// test/render.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router'

export function renderApp(routes: RouteObject[], initialPath = '/') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [initialPath] })
  const result = render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { ...result, router, client }
}
```
Add one test for `describeError` (429 with and without `retryAfter`, a `TypeError` → network message) to `client.test.ts`.

- [ ] **Step 5: pass; commit**

```bash
git add frontend/src/shared frontend/src/test
git commit -m "feat(frontend): API client with silent refresh, error descriptions and test helpers"
```

---

### Task 4: Session, login, register, route guard

**Files:**
- Create: `src/features/auth/{api.ts,session.tsx,RequireAuth.tsx,validation.ts}`, `src/features/auth/forms/{LoginForm,RegisterForm}.tsx`, `src/features/auth/tests/{session,forms}.test.tsx`, `src/pages/public/{LoginPage,RegisterPage}.tsx`
- Modify: `src/test/render.tsx` (wrap in `SessionProvider`)

**Interfaces:**
- `auth/api.ts`: `login(email, password): Promise<void>` (stores token via `api.setAccessToken`), `register(email, password, nick): Promise<void>`, `logout(): Promise<void>` (POST `/auth/logout`, clears token even if it fails), `fetchMe(): Promise<Me>`.
- `auth/session.tsx`: `SessionProvider`, `useSession(): { status: 'loading' | 'anonymous' | 'authenticated'; me: Me | null; signIn(email, password): Promise<void>; signUp(email, password, nick): Promise<void>; signOut(): Promise<void> }`. On mount: `api.refresh()`; if true → load `['me']`. `signIn` → login + refetch me. `signUp` → register then signIn. `signOut` → logout, `queryClient.clear()`.
- `auth/validation.ts` (mirrors the backend so 400s are rare): `validateEmail`, `validatePassword` (8+ chars, ≤256 bytes via `new TextEncoder().encode(p).length`), `validateNick` (1..32 chars after trim) — each returns `string | undefined` (Russian message).
- `RequireAuth`: loading → quiet placeholder `<p className="text-frost">Проверяем вход…</p>`; anonymous → `<Navigate to="/login" replace state={{ from: location.pathname }} />`; else `<Outlet />`.
- Login form: fields «Почта», «Пароль»; submit «Войти»; 401 → «Неверная почта или пароль.»; success → navigate to `state.from ?? '/configs'`. Register form: «Почта», «Ник на сайте» (hint «Не обязательно совпадает с ником в Minecraft.»), «Пароль» (hint «Минимум 8 символов.»); submit «Создать аккаунт»; 409 → «Эта почта уже занята. Войди или используй другую.»

- [ ] **Step 1: Failing tests**

`tests/session.test.tsx`:
```tsx
import { screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import { RequireAuth } from '../RequireAuth'
import { useSession } from '../session'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

const me = { id: '1', email: 'a@b.c', display_nick: 'Rider', role: 'user', avatar_url: null, created_at: '2026-09-24T00:00:00Z' }

function Whoami() {
  const { me } = useSession()
  return <p>Привет, {me?.display_nick}</p>
}

const routes = [
  { path: '/login', element: <p>Страница входа</p> },
  { element: <RequireAuth />, children: [{ path: '/configs', element: <Whoami /> }] },
]

test('restores the session from the refresh cookie', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({ access_token: 't' }), 'GET /me': () => json(me) })
  renderApp(routes, '/configs')
  expect(await screen.findByText('Привет, Rider')).toBeInTheDocument()
})

test('anonymous visitor is sent to login', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({ error: 'unauthorized' }, 401) })
  renderApp(routes, '/configs')
  expect(await screen.findByText('Страница входа')).toBeInTheDocument()
})
```
`tests/forms.test.tsx`:
```tsx
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { LoginForm } from '../forms/LoginForm'
import { RegisterForm } from '../forms/RegisterForm'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

const me = { id: '1', email: 'a@b.c', display_nick: 'Rider', role: 'user', avatar_url: null, created_at: '2026-09-24T00:00:00Z' }
const anon = { 'POST /auth/refresh': () => json({ error: 'unauthorized' }, 401) }

test('wrong password shows a clear error', async () => {
  const user = userEvent.setup()
  mockFetch({ ...anon, 'POST /auth/login': () => json({ error: 'unauthorized' }, 401) })
  renderApp([{ path: '/login', element: <LoginForm /> }], '/login')
  await user.type(await screen.findByLabelText('Почта'), 'a@b.c')
  await user.type(screen.getByLabelText('Пароль'), 'wrong-password')
  await user.click(screen.getByRole('button', { name: 'Войти' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Неверная почта или пароль.')
})

test('successful login goes to configs', async () => {
  const user = userEvent.setup()
  mockFetch({ ...anon, 'POST /auth/login': () => json({ access_token: 't' }), 'GET /me': () => json(me) })
  renderApp([{ path: '/login', element: <LoginForm /> }, { path: '/configs', element: <p>Мои конфиги</p> }], '/login')
  await user.type(await screen.findByLabelText('Почта'), 'a@b.c')
  await user.type(screen.getByLabelText('Пароль'), 'correct-horse')
  await user.click(screen.getByRole('button', { name: 'Войти' }))
  expect(await screen.findByText('Мои конфиги')).toBeInTheDocument()
})

test('register validates before calling the server', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch(anon)
  renderApp([{ path: '/register', element: <RegisterForm /> }], '/register')
  await user.type(await screen.findByLabelText('Почта'), 'not-an-email')
  await user.type(screen.getByLabelText('Пароль'), 'short')
  await user.click(screen.getByRole('button', { name: 'Создать аккаунт' }))
  expect(screen.getByLabelText('Почта')).toHaveAttribute('aria-invalid', 'true')
  expect(screen.getByLabelText('Пароль')).toHaveAccessibleDescription('Минимум 8 символов.')
  expect(fetch.mock.calls.some(([u]) => String(u).includes('/auth/register'))).toBe(false)
})

test('taken email explains what to do', async () => {
  const user = userEvent.setup()
  mockFetch({ ...anon, 'POST /auth/register': () => json({ error: 'email already registered' }, 409) })
  renderApp([{ path: '/register', element: <RegisterForm /> }], '/register')
  await user.type(await screen.findByLabelText('Почта'), 'a@b.c')
  await user.type(screen.getByLabelText('Ник на сайте'), 'Rider')
  await user.type(screen.getByLabelText('Пароль'), 'correct-horse')
  await user.click(screen.getByRole('button', { name: 'Создать аккаунт' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Эта почта уже занята')
})
```

- [ ] **Step 2: Implement `api.ts`, `validation.ts`**

```ts
// api.ts
import { api } from '../../shared/api/client'
import type { Me } from '../../shared/types'

export async function login(email: string, password: string): Promise<void> {
  const res = await api.request<{ access_token: string }>('/auth/login', { method: 'POST', json: { email, password } })
  api.setAccessToken(res.access_token)
}

export async function register(email: string, password: string, nick: string): Promise<void> {
  await api.request('/auth/register', { method: 'POST', json: { email, password, nick } })
}

export async function logout(): Promise<void> {
  try {
    await api.request('/auth/logout', { method: 'POST' })
  } finally {
    api.setAccessToken(null)
  }
}

export const fetchMe = () => api.request<Me>('/me')
```
```ts
// validation.ts
export function validateEmail(email: string): string | undefined {
  const v = email.trim()
  if (!v) return 'Введи почту.'
  if (v.length > 254 || !/^[^@\s]+@[^@\s]+$/.test(v)) return 'Похоже, в почте опечатка.'
}

export function validatePassword(password: string): string | undefined {
  if ([...password].length < 8) return 'Минимум 8 символов.'
  if (new TextEncoder().encode(password).length > 256) return 'Слишком длинный пароль.'
}

export function validateNick(nick: string): string | undefined {
  const len = [...nick.trim()].length
  if (len === 0) return 'Придумай ник.'
  if (len > 32) return 'Не длиннее 32 символов.'
}
```

- [ ] **Step 3: Implement `session.tsx`**

```tsx
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useMemo, type ReactNode } from 'react'
import { api } from '../../shared/api/client'
import type { Me } from '../../shared/types'
import { fetchMe, login, logout, register } from './api'

type Session = {
  status: 'loading' | 'anonymous' | 'authenticated'
  me: Me | null
  signIn(email: string, password: string): Promise<void>
  signUp(email: string, password: string, nick: string): Promise<void>
  signOut(): Promise<void>
}

const SessionContext = createContext<Session | null>(null)

export function SessionProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient()
  // Restores the session after a reload: the refresh cookie survives, the
  // in-memory access token does not.
  const restored = useQuery({ queryKey: ['session-restore'], queryFn: () => api.refresh(), staleTime: Infinity })
  const meQuery = useQuery({ queryKey: ['me'], queryFn: fetchMe, enabled: restored.data === true })

  const signIn = useCallback(async (email: string, password: string) => {
    await login(email, password)
    client.setQueryData(['session-restore'], true)
    await client.fetchQuery({ queryKey: ['me'], queryFn: fetchMe })
  }, [client])

  const signUp = useCallback(async (email: string, password: string, nick: string) => {
    await register(email, password, nick)
    await signIn(email, password)
  }, [signIn])

  const signOut = useCallback(async () => {
    await logout()
    client.clear()
    client.setQueryData(['session-restore'], false)
  }, [client])

  const value = useMemo<Session>(() => {
    const me = meQuery.data ?? null
    const status = restored.isPending || (restored.data && meQuery.isPending) ? 'loading' : me ? 'authenticated' : 'anonymous'
    return { status, me, signIn, signUp, signOut }
  }, [restored.isPending, restored.data, meQuery.isPending, meQuery.data, signIn, signUp, signOut])

  return <SessionContext value={value}>{children}</SessionContext>
}

export function useSession(): Session {
  const session = useContext(SessionContext)
  if (!session) throw new Error('useSession outside SessionProvider')
  return session
}
```
(React 19: a context object renders directly as a provider.) Update `test/render.tsx` to wrap `RouterProvider` in `<SessionProvider>` (inside `QueryClientProvider`).

- [ ] **Step 4: `RequireAuth.tsx`, forms, pages**

```tsx
// RequireAuth.tsx
import { Navigate, Outlet, useLocation } from 'react-router'
import { useSession } from './session'

export function RequireAuth() {
  const { status } = useSession()
  const location = useLocation()
  if (status === 'loading') return <p className="text-frost">Проверяем вход…</p>
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
```
```tsx
// forms/LoginForm.tsx
import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { describeError } from '../../../shared/api/errors'
import { Button } from '../../../shared/ui/Button'
import { Notice } from '../../../shared/ui/Notice'
import { TextField } from '../../../shared/ui/TextField'
import { useSession } from '../session'

export function LoginForm() {
  const { signIn } = useSession()
  const navigate = useNavigate()
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/configs'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(undefined)
    try {
      await signIn(email.trim(), password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(describeError(err, { 401: 'Неверная почта или пароль.' }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="flex w-full max-w-sm flex-col gap-4" noValidate>
      <TextField label="Почта" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      <TextField label="Пароль" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
      {error ? <Notice tone="error">{error}</Notice> : null}
      <Button type="submit" busy={busy}>Войти</Button>
      <p className="text-sm text-frost">
        Нет аккаунта? <Link to="/register" className="text-ice underline-offset-4 hover:underline">Создать</Link>
      </p>
    </form>
  )
}
```
`RegisterForm.tsx` follows the same shape: three fields, per-field `error` from `validation.ts` computed on submit (fields keep their `hint` otherwise), no request when any field is invalid, `describeError(err, { 409: 'Эта почта уже занята. Войди или используй другую.' })`, success → `navigate('/configs', { replace: true })`, footer «Уже есть аккаунт? Войти». Password field: `hint="Минимум 8 символов."`, and when its validation fails show the validation message as `error` (so the test's accessible description `Минимум 8 символов.` holds either way).

Pages (thin): `LoginPage.tsx`
```tsx
import { LoginForm } from '../../features/auth/forms/LoginForm'

export default function LoginPage() {
  return (
    <section className="flex flex-col gap-6 py-12">
      <h1 className="text-3xl">Вход</h1>
      <LoginForm />
    </section>
  )
}
```
`RegisterPage.tsx` the same with «Новый аккаунт» and a one-line lead: «Аккаунт нужен для облачных конфигов и привязки игры. Пароль в мод вводить не придётся.»

- [ ] **Step 5: pass; commit**

```bash
git add frontend/src/features/auth frontend/src/pages frontend/src/test
git commit -m "feat(frontend): session restore, login and registration with client-side validation"
```

---

### Task 5: App shell, routes, landing page

**Files:**
- Create: `src/routes.tsx`, `src/shared/layout/{AppShell,TopBar}.tsx`, `src/shared/layout/tests/layout.test.tsx`, `src/features/landing/{CommandHero,HowItWorks}.tsx`, `src/features/landing/tests/landing.test.tsx`, `src/pages/public/{HomePage,NotFoundPage}.tsx`
- Modify: `src/App.tsx`, `src/test/smoke.test.tsx` (delete — superseded by layout tests)

**Interfaces:**
- `routes: RouteObject[]` in `routes.tsx` — root `{ element: <AppShell />, children: [...] }`. Pages from Tasks 6–9 are added there by those tasks; lazy-load them with `lazy: () => import('./pages/app/ConfigsPage').then((m) => ({ Component: m.default }))`.
- `App.tsx`: `QueryClientProvider` (singleton `QueryClient`, `staleTime: 30_000`) → `SessionProvider` → `RouterProvider router={createBrowserRouter(routes)}`.
- `TopBar`: logo «LoVisual» (Comfortaa, links to `/`); `NavLink`s «Витрина» `/showcase`, «Мои конфиги» `/configs`, «Привязать игру» `/link` (active link: `text-snow`, inactive `text-frost`); right side: anonymous → «Войти» (quiet button-styled link to `/login`); authenticated → avatar (or first letter of nick in a shoal circle) + nick linking to `/account`. On < 640px nav collapses under a «Меню» disclosure button (`aria-expanded`).
- `CommandHero`: headline «Твои настройки LoVisual — на любом компьютере», lead «Четыре облачных слота, код, которым делишься с друзьями, и витрина готовых конфигов.», CTAs «Создать аккаунт» (→ `/register`, primary; replaced by «Мои конфиги» → `/configs` when authenticated) and «Смотреть витрину» (quiet). Below: the chat bar (`bg-black/55`, Iosevka, like Minecraft's chat) that types `%config load 7KQ3M9XA` (60ms/char, once, via `setInterval` in an effect) then shows `[LoVisual] Конфиг «pvp» загружен` in ice. With `matchMedia('(prefers-reduced-motion: reduce)').matches` both lines render complete immediately. The whole bar has `aria-label="Пример: загрузка конфига по коду в чате игры"` and the animated text is `aria-hidden`, so screen readers get one stable sentence.
- `HowItWorks`: section «Четыре слота в облаке» (2×2 grid of small slate panels: «pvp», «стрим», «ленивый», «пусто» in frost italics) with one sentence each side; section «Привязка игры» as an `<ol>` of three steps: «В моде открой вкладку аккаунта — там появится код.» / «Введи код на странице «Привязать игру».» / «Готово: мод входит в аккаунт сам, пароль ему не нужен.»; closing line «Любой конфиг из витрины можно скопировать в свободный слот одной кнопкой.» with a link «Открыть витрину».
- `NotFoundPage`: «Такой страницы нет» + link «На главную».

- [ ] **Step 1: Failing tests**

`layout.test.tsx`:
```tsx
import { screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import { routes } from '../../../routes'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

test('anonymous visitor sees nav and a login link', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/')
  expect(await screen.findByRole('link', { name: 'Войти' })).toHaveAttribute('href', '/login')
  expect(screen.getByRole('link', { name: 'Витрина' })).toBeInTheDocument()
  expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('на любом компьютере')
})

test('signed-in visitor sees their nick instead of login', async () => {
  mockFetch({
    'POST /auth/refresh': () => json({ access_token: 't' }),
    'GET /me': () => json({ id: '1', email: 'a@b.c', display_nick: 'Rider', role: 'user', avatar_url: null, created_at: '' }),
  })
  renderApp(routes, '/')
  expect(await screen.findByRole('link', { name: /Rider/ })).toHaveAttribute('href', '/account')
  expect(screen.queryByRole('link', { name: 'Войти' })).not.toBeInTheDocument()
})

test('unknown path shows not found', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  renderApp(routes, '/definitely-not-here')
  expect(await screen.findByRole('heading', { name: 'Такой страницы нет' })).toBeInTheDocument()
})
```
`landing.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react'
import { expect, test, vi } from 'vitest'
import { CommandHero } from '../CommandHero'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SessionProvider } from '../../auth/session'
import { json, mockFetch } from '../../../test/fetch'

test('reduced motion shows the full command at once', async () => {
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('reduce'), addEventListener() {}, removeEventListener() {} }))
  mockFetch({ 'POST /auth/refresh': () => json({}, 401) })
  const router = createMemoryRouter([{ path: '/', element: <CommandHero /> }])
  render(
    <QueryClientProvider client={new QueryClient()}>
      <SessionProvider><RouterProvider router={router} /></SessionProvider>
    </QueryClientProvider>,
  )
  expect(await screen.findByText('%config load 7KQ3M9XA')).toBeInTheDocument()
  expect(screen.getByText('[LoVisual] Конфиг «pvp» загружен')).toBeInTheDocument()
  expect(screen.getByLabelText('Пример: загрузка конфига по коду в чате игры')).toBeInTheDocument()
})
```
(jsdom has no `matchMedia`; `CommandHero` must guard `typeof window.matchMedia === 'function'` and treat "unknown" as reduced motion — safe default, also keeps other tests deterministic.)

- [ ] **Step 2: Implement** `AppShell` (TopBar + `<main className="mx-auto w-full max-w-[1120px] px-4 sm:px-6">` + `<Outlet />`), `TopBar`, `CommandHero`, `HowItWorks`, pages, `routes.tsx`, `App.tsx` per the interfaces above. `routes.tsx` for now:

```tsx
import type { RouteObject } from 'react-router'
import { RequireAuth } from './features/auth/RequireAuth'
import { AppShell } from './shared/layout/AppShell'
import HomePage from './pages/public/HomePage'
import LoginPage from './pages/public/LoginPage'
import RegisterPage from './pages/public/RegisterPage'
import NotFoundPage from './pages/public/NotFoundPage'

export const routes: RouteObject[] = [
  {
    element: <AppShell />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { element: <RequireAuth />, children: [] }, // Tasks 6–8 add /link, /account, /configs
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
```
`shared/layout` importing from `features/auth/session` is the one allowed shared→feature edge (the shell needs the session); note it in `ARCHITECTURE.md`.

The typing effect in `CommandHero`:
```tsx
const COMMAND = '%config load 7KQ3M9XA'

function prefersReducedMotion(): boolean {
  return typeof window.matchMedia !== 'function' || window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

// inside the component
const [typed, setTyped] = useState(() => (prefersReducedMotion() ? COMMAND.length : 0))
useEffect(() => {
  if (typed >= COMMAND.length) return
  const t = setInterval(() => setTyped((n) => Math.min(n + 1, COMMAND.length)), 60)
  return () => clearInterval(t)
}, [typed >= COMMAND.length])
const done = typed >= COMMAND.length
```
Render `{COMMAND.slice(0, typed)}` + a blinking caret span (`animate-pulse`, hidden once `done`), and the confirmation line only when `done`.

- [ ] **Step 3: pass; visual check**

`bun run dev`, open `http://localhost:5173/` (no backend needed — refresh fails → anonymous). Check 1280px and 375px widths: no horizontal scroll, nav collapses, hero line wraps cleanly, focus ring visible when tabbing. Fix anything off before committing.

- [ ] **Step 4: commit**

```bash
git add frontend/src
git commit -m "feat(frontend): app shell, routing and landing page with the chat-command hero"
```

---

### Task 5A: i18n foundation (ru/en) and migrating existing strings

**Why now:** every later task writes strings; converting after the fact doubles the work.

**Files:**
- Move: `src/App.tsx`, `src/routes.tsx` → `src/app/App.tsx`, `src/app/routes.tsx` (src/ top level becomes `main.tsx`, `index.css` + `app/`), update imports.
- Create: `src/app/i18n.ts` (init), `src/app/i18next.d.ts` (typed keys), `src/shared/i18n/{common.ru.ts,common.en.ts,LanguageSwitch.tsx}`, `src/shared/i18n/tests/i18n.test.tsx`
- Create per feature: `src/features/<feature>/i18n/{ru.ts,en.ts}` for `auth` and `landing` now; later tasks add their own.
- Modify: every component with hard-coded text (shared/ui ShareCode, shared/api/errors.ts, shared/layout, features/auth forms + validation, features/landing, pages/public/*), `src/test/setup.ts` (init i18n with `lng: 'ru'` synchronously), `index.html` (`lang` set at runtime).

**Interfaces:**
- One i18next namespace per feature (`common`, `auth`, `landing`, later `account`, `configs`, `showcase`, `download`, `themes`, `profile`, `link`). Russian dictionaries are `as const` objects; English ones are typed `satisfies Translation<typeof ru>` where `type Translation<T> = { [K in keyof T]: T[K] extends string ? string : Translation<T[K]> }` (exported from `shared/i18n/common.ru.ts`) — a missing or extra key in `en` is a compile error.
- `app/i18n.ts`: `initI18n(lng?: 'ru' | 'en'): i18n` — `i18next.use(LanguageDetector).use(initReactI18next).init({ resources: { ru: {...namespaces}, en: {...} }, fallbackLng: 'ru', supportedLngs: ['ru','en'], ns: [...], defaultNS: 'common', interpolation: { escapeValue: false }, detection: { order: ['localStorage','navigator'], caches: ['localStorage'], lookupLocalStorage: 'lv_lang' } })`; on `languageChanged` set `document.documentElement.lang`.
- `app/i18next.d.ts`: `declare module 'i18next' { interface CustomTypeOptions { defaultNS: 'common'; resources: { common: typeof commonRu; auth: typeof authRu; landing: typeof landingRu /* extended by later tasks */ } } }` so `t('auth:login.submit')` is type-checked.
- `LanguageSwitch`: two-state toggle «RU / EN» in the TopBar (and footer), `aria-pressed` on the active one, calls `i18n.changeLanguage`.
- `describeError(err, overrides)` takes the `t` function: `describeError(t, err, overrides)`; messages move to `common` namespace (`errors.rateLimited` with `{{seconds}}` interpolation, etc.).
- Plurals via i18next plural keys (`_one/_few/_many` for ru, `_one/_other` for en).

- [ ] **Step 1: Failing tests (`shared/i18n/tests/i18n.test.tsx`)**
```tsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { I18nextProvider, useTranslation } from 'react-i18next'
import { expect, test } from 'vitest'
import { initI18n } from '../../../app/i18n'
import { LanguageSwitch } from '../LanguageSwitch'

function Probe() {
  const { t } = useTranslation('auth')
  return <p>{t('login.submit')}</p>
}

test('switching language re-renders strings and sets <html lang>', async () => {
  const user = userEvent.setup()
  const i18n = initI18n('ru')
  render(<I18nextProvider i18n={i18n}><Probe /><LanguageSwitch /></I18nextProvider>)
  expect(screen.getByText('Войти')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'EN' }))
  expect(screen.getByText('Log in')).toBeInTheDocument()
  expect(document.documentElement.lang).toBe('en')
})

test('russian plurals', () => {
  const i18n = initI18n('ru')
  expect(i18n.t('common:time.days', { count: 1 })).toBe('1 день')
  expect(i18n.t('common:time.days', { count: 3 })).toBe('3 дня')
  expect(i18n.t('common:time.days', { count: 5 })).toBe('5 дней')
})
```
Existing tests keep asserting Russian text — they must still pass unchanged after the migration (that's the regression check). Add one English smoke test for the landing hero headline.

- [ ] **Step 2: Implement; migrate every existing string; `grep -rnP '[А-Яа-яЁё]' src --include=*.tsx --include=*.ts | grep -v '/i18n/' | grep -v '/tests/'` must print nothing.**
- [ ] **Step 3: test/build/lint; commit** — `feat(frontend): ru/en i18n with typed per-feature dictionaries and language switch`

---

### Task 5B: Landing v2 — the product on screen

Replaces the Task 5 landing body (keep `CommandHero`'s chat line — it becomes one section). Everything is drawn in code.

**Files:** new feature folders (each ≤4 files + subfolders):
- `src/features/clickgui/` — `ClickGuiWindow.tsx` (window chrome + category column + module list), `ModuleCard.tsx` (toggle, expandable settings: slider/checkbox/color dot), `themeVars.ts` (`themeToCssVars(entry: ThemeEntry): CSSProperties` — maps the mod's 12 colors + 5 gradients to CSS custom properties `--gui-window-bg` …), `demo.ts` (scripted demo timeline); `tests/`.
- `src/features/themes/` — `presets.generated.ts` (from the mod, see Task 5D Step 1 — generate it in THIS task, 5D reuses it), `types.ts` (`ThemeEntry`, `GradientSpec`, `argbToCss(hex: string): string` for `#AARRGGBB`), `useActiveTheme.ts` (context: active preset id, `setTheme`, persisted in `localStorage` key `lv_theme`); `tests/`.
- `src/features/hud/` — `HudWidget.tsx` (draggable wrapper: pointer events, clamped to parent, keyboard-movable with arrow keys when focused), `widgets/{TargetHud,Keybinds,Potions,Watermark}.tsx`; `tests/`.
- `src/features/landing/` — `CommandHero.tsx` (existing chat line, now a section), `HowItWorks.tsx` (existing, restyled), `sections/{Hero,ThemeStrip,HudPlayground,ModuleWall,ShowcaseTeaser,FaqDownload}.tsx`, `effects/{ParticleField.tsx,VoxelScene.tsx,useInView.ts}`; `tests/`.

**Page composition (top to bottom):**
1. **Hero** — full-viewport. Backdrop: `VoxelScene` (procedural blocky hills + dusk sky, parallax on scroll) under `ParticleField` (canvas, ~120 soft particles, accent-tinted, `requestAnimationFrame`, paused via `IntersectionObserver` and when `prefers-reduced-motion`). Left: headline (Unbounded, clamp size) «Визуалы, которые видно» / en «Visuals you can see», lead «Бесплатный легит-мод для Minecraft 26.2: 52 визуальных модуля, облачные конфиги и темы, которые ты собираешь сам.», primary «Скачать бесплатно» (direct release link, Task 5C `DOWNLOAD_URL`) + secondary «Создать аккаунт». Right: `ClickGuiWindow` in 3D (`perspective` + `rotateX/Y` from pointer, max 8°, spring via `motion`), running `demo.ts`: a fake cursor moves, toggles «Trails», opens its settings, drags a slider, then switches theme; loop every ~12s; pauses on hover (user takes over — cards are clickable).
2. **ThemeStrip** — «Сотни оттенков. Твой — один.»: horizontal row of every preset from `presets.generated.ts` as swatch pills (window bg + accent + gradient); clicking one re-themes the hero ClickGui and the page accent live (CSS vars on `:root` via `useActiveTheme`) with a 400ms cross-fade; CTA «Собрать свою тему» → `/themes`.
3. **HudPlayground** — `VoxelScene` crop with 4 draggable HUD widgets; caption «Перетащи — так же, как в игре.»; «Сбросить» button restores positions.
4. **Cloud configs** — the existing `CommandHero` chat line + `HowItWorks` 2×2 slots, now animated in sequence when scrolled into view (`useInView`): `%config save 1` → slot fills with a pulse → code appears → second chat line `%config load 7KQ3M9XA` → «Конфиг загружен».
5. **ModuleWall** — «88 модулей. 52 — про красоту.»: dense wall of module names (from a list in `landing/i18n` — names stay as in-game, untranslated) in 4 category columns; hovering a visuals module shows a one-line description tooltip; the wall slowly auto-scrolls vertically in each column at different speeds (CSS `@keyframes` translateY on duplicated lists, `animation-play-state: paused` on hover and under reduced motion).
6. **ShowcaseTeaser** — top 3 popular configs from `GET /showcase?sort=popular` (TanStack Query); on error/empty shows 3 skeleton-styled example cards labelled «Пример» (never a broken section).
7. **FaqDownload** — FAQ accordion (`<details>`; 5 questions: бесплатно ли, какие версии, нужен ли аккаунт, безопасно ли / легит, как перенести настройки) + final download band with version, Minecraft version and size (from Task 5C release data).
8. **Footer** — logo, nav, GitHub link, `LanguageSwitch`, «Не связано с Mojang или Microsoft.»

**Interfaces:**
- `themeToCssVars(entry)` output keys (used by ClickGui CSS): `--gui-window-bg, --gui-header, --gui-stroke, --gui-surface, --gui-surface-hover, --gui-card-on, --gui-card-off, --gui-text, --gui-text-muted, --gui-accent, --gui-accent-soft, --gui-stroke-soft`, plus `--gui-{window,header,surface,card,stroke}-gradient` as `linear-gradient(<angle>deg, start, end)` when enabled, else the flat color.
- `demo.ts`: `type DemoStep = { at: number /*ms*/; action: 'move' | 'click' | 'drag' | 'theme'; target: string; value?: number | string }`, `DEMO: DemoStep[]`, `useDemo(steps, { paused }): { cursor: {x,y}, state }`.
- `HudWidget` props: `{ id: string; initial: { x: number; y: number }; children: ReactNode; label: string }` — `aria-label={label}`, `tabIndex=0`, arrows move 8px.

- [ ] **Step 1: Failing tests** — `themeToCssVars` maps a preset exactly (ARGB `#F012191B` → `rgba(18, 25, 27, 0.941)`); clicking a ThemeStrip swatch changes `--gui-accent` on the ClickGui root; `HudWidget` moves with ArrowRight by 8px and stays inside its parent bounds; `ParticleField` renders nothing animated (no rAF scheduled) under reduced motion (mock `matchMedia`); ModuleWall lists all four category headings; ShowcaseTeaser falls back to example cards on fetch error; hero download link points to the release URL.
- [ ] **Step 2: Implement.** Performance budget: landing JS ≤ 220 kB gzip (check `bun run build` output), no layout shift from fonts (`font-display: swap` is the fontsource default; reserve hero height), 60fps particle field on a mid laptop (cap DPR at 2, ≤150 particles).
- [ ] **Step 3: Visual check** with headless Firefox screenshots at 1440 and 375 (`firefox --headless --screenshot` — note it captures before long animations settle, so also verify the reduced-motion static state), both languages. Fix overflow, contrast, clipping.
- [ ] **Step 4: commit** — `feat(frontend): landing v2 with live ClickGui, theme strip, HUD playground and module wall`

---

### Task 5C: Download (one click from GitHub Releases) + «Что нового»

**Decisions:** the download button is a plain link to `https://github.com/${VITE_GITHUB_REPO}/releases/latest/download/lovisual.jar` — GitHub redirects straight to the file, so one click downloads the latest build with no intermediate page. This requires every release to carry an asset named exactly `lovisual.jar` (Step 1 adds the CI that guarantees it). `VITE_GITHUB_REPO=loki5512344/LoVisual-` in `frontend/.env` (and `.env.example`). The repo is private today — until it is public, the link 404s for visitors; the page shows the changelog fallback message then.

**Files:**
- Create: `.github/workflows/release.yml` (repo root) — on tag `v*`: JDK 25 (match `mod/build.gradle` toolchain), `./gradlew build` in `mod/`, copy the remapped jar to `lovisual.jar`, create the release with both `lovisual-<version>.jar` and `lovisual.jar` via `softprops/action-gh-release@v2` (`generate_release_notes: true`).
- Create: `src/features/download/{api.ts,DownloadButton.tsx,ReleaseNotes.tsx}`, `src/features/download/i18n/{ru,en}.ts`, `src/features/download/tests/download.test.tsx`, `src/pages/public/…` → pages/public is full (4 files) — create `src/pages/download/DownloadPage.tsx`.
- Modify: `src/app/routes.tsx` (`/download`), TopBar (a «Скачать» primary button on the right, before login), Landing hero/FAQ band (use `DownloadButton`).
- Add dependency: `react-markdown` (release notes are Markdown; react-markdown does not render raw HTML by default — keep it that way, no `rehype-raw`).

**Interfaces:**
- `api.ts`: `DOWNLOAD_URL: string`; `type Release = { tag_name: string; name: string; published_at: string; body: string; html_url: string; assets: { name: string; size: number; download_count: number }[] }`; `fetchReleases(): Promise<Release[]>` → `GET https://api.github.com/repos/${repo}/releases?per_page=10` (unauthenticated, 60 req/h/IP is plenty; TanStack Query `staleTime: 10 min`).
- `DownloadButton` props: `{ size?: 'md' | 'lg' }` — `<a href={DOWNLOAD_URL} download>` styled as primary button, label «Скачать бесплатно», sub-label with latest version + size when releases loaded («v0.1.1 · 4,2 МБ»).
- `ReleaseNotes`: list of releases (version, date via `Intl.DateTimeFormat(i18n.language)`, Markdown body, «Все релизы на GitHub» link). Error/empty → «Список изменений появится, когда выйдет первый публичный релиз.»
- `DownloadPage` (`/download`): big button, requirements block (Minecraft 26.2, Fabric Loader 0.19.4+, Fabric API; optional Sodium/Iris — values in the download dictionary, sourced from `mod/gradle.properties`), 3-step install guide (real sequence: скачать → положить в `mods` → запустить с профилем Fabric), then `ReleaseNotes`.

- [ ] **Step 1: CI workflow** (no test; validate with `actionlint` if available, otherwise careful review). Tagging/pushing is the owner's action — do not push tags.
- [ ] **Step 2: Failing tests** — button href equals `https://github.com/loki5512344/LoVisual-/releases/latest/download/lovisual.jar` (with `vi.stubEnv('VITE_GITHUB_REPO', …)`); release notes render Markdown headings and do NOT render a raw `<script>`/`<img onerror>` from the body as HTML; fetch failure shows the fallback text.
- [ ] **Step 3: Implement; routes; TopBar button.**
- [ ] **Step 4: commit** — `feat(frontend): one-click download from GitHub releases, changelog page, release CI`

---

### Task 5D: Theme editor (`/themes`)

**Files:**
- Create: `frontend/scripts/extract-themes.ts` (bun script) + `package.json` script `"gen:themes": "bun scripts/extract-themes.ts"` — parses `mod/src/main/java/dev/loki/lovisual/features/theme/presets/ThemePresets{1,2,3}.java` (`new Theme(0x…, … 12 ints)` and `new ThemeEntry(id, name, builtin, THEME, new GradientSpec(bool, 0x…, 0x…, angle) ×5)`) into `src/features/themes/presets.generated.ts` (`export const PRESETS: ThemeEntry[]`, header comment "generated — do not edit"). If Task 5B already created it, reuse.
- Create: `src/features/themes/editor/{ThemeEditor.tsx,ColorField.tsx,GradientField.tsx,codec.ts}`, `src/features/themes/i18n/{ru,en}.ts`, tests; `src/pages/themes/ThemesPage.tsx`; route `/themes` (public).

**Interfaces:**
- `codec.ts`: `toProfileJson(entry: ThemeEntry): string` — exactly the mod's `ThemesProfileCodec.toProfileValues` map (keys `name, windowBg, windowHeader, windowStroke, surface, surfaceHover, cardEnabled, cardDisabled, textPrimary, textMuted, accent, accentSoft, strokeSoft`, and for each of `window, header, surface, card, stroke`: `<p>GradientEnabled` (bool), `<p>GradientStart`, `<p>GradientEnd` (`#AARRGGBB` uppercase), `<p>GradientAngle` (number)); `fromProfileJson(json: string): ThemeEntry | { error: string }` (tolerant like the mod: missing keys fall back to the Classic preset); `toShareHash(entry): string` (base64url of the compact JSON) and `fromShareHash(hash)`.
- Editor layout: left — live `ClickGuiWindow` + two HUD widgets on the `VoxelScene` (large); right — preset picker, 12 `ColorField`s (native `<input type="color">` + alpha slider + hex input accepting `#RRGGBB`/`#AARRGGBB`), 5 `GradientField`s (toggle, two colors, angle dial 0–360), name field. Actions: «Скопировать для мода» (JSON to clipboard), «Скачать .json», «Импорт» (paste/file), «Ссылка на тему» (URL with `#t=<hash>`; opening such a URL loads the theme). Undo/redo (Ctrl+Z / Ctrl+Shift+Z) over the last 50 edits.
- Mod side (NOT in this plan — added to the mod integration plan): `%theme import` reads the JSON from the clipboard via the same codec.

- [ ] **Step 1: Generator + test** that `PRESETS` contains Classic with accent `#FF5CC8E7` and that the count equals the number of `ThemeEntry` constants in the Java files.
- [ ] **Step 2: Failing tests** — codec round-trip (`fromProfileJson(toProfileJson(p))` deep-equals `p` for every preset); JSON keys match the mod's list exactly; editing a color updates the preview's `--gui-accent`; share hash round-trip; malformed import shows an error, keeps the current theme.
- [ ] **Step 3: Implement; commit** — `feat(frontend): theme editor with live ClickGui preview, mod-compatible export and share links`

---

### Task 5E: Public profiles (`/u/:id`) — after configs-service is live

**Backend prerequisite (added to backend plans as their own tasks):** accounts-service `GET /users/{id}` public → `{ id, display_nick, avatar_url, created_at, badges: string[] }` (badges computed: `early` for the first 1000 accounts, `sharer` when the user has ≥1 published config — the latter via configs gRPC later; start with `early` only); configs-service `GET /showcase?author={id}`.

**Files:** `src/features/profile/{api.ts,ProfileHeader.tsx,ProfileConfigs.tsx}`, `i18n/`, `tests/`, `src/pages/profile/ProfilePage.tsx`; showcase author names and TopBar nick link to `/u/:id`.

**Interfaces:** header — large avatar (or initial on an accent-gradient disc), nick (Unbounded), «С нами с {date}», badge chips with tooltips; below — the user's published configs using the showcase `ListingItem`; own profile shows «Редактировать» → `/account`. 404 → «Такого игрока нет».

- [ ] **Steps:** failing tests (renders header + configs, 404 message, own-profile edit link) → implement → commit `feat(frontend): public player profiles`

---

### Task 6: Link the game (`/link`)

**Files:** Create `src/features/device-link/{api.ts,LinkDeviceForm.tsx}`, `src/features/device-link/tests/link.test.tsx`, `src/pages/app/LinkPage.tsx`; modify `src/routes.tsx`.

**Interfaces:**
- `api.ts`: `confirmDevice(userCode: string): Promise<void>` → `POST /device/confirm {user_code}`; `normalizeUserCode(raw: string): string` → uppercase, strip spaces, insert the dash after 4 chars if missing (`abcd1234` → `ABCD-1234`).
- Form: single field «Код из игры» (Iosevka input, `autoComplete="off"`, `inputMode="text"`, `maxLength={9}`, placeholder `ABCD-1234`), button «Привязать». Success replaces the form with «Игра привязана. Можно возвращаться в Minecraft — мод уже вошёл в аккаунт.» 404 → «Код не найден или устарел. Коды живут 10 минут — возьми новый в моде.» Route `/link` inside `RequireAuth` (anonymous → login → back to `/link` via `state.from`).
- Page: h1 «Привязать игру», lead «Пароль в мод вводить не нужно: мод показывает короткий код, ты подтверждаешь его здесь.»

- [ ] **Step 1: Failing tests**

```tsx
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { LinkDeviceForm } from '../LinkDeviceForm'
import { normalizeUserCode } from '../api'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

test('normalizes typed codes', () => {
  expect(normalizeUserCode(' abcd1234 ')).toBe('ABCD-1234')
  expect(normalizeUserCode('abcd-1234')).toBe('ABCD-1234')
})

test('confirms a code and shows success', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch({ 'POST /auth/refresh': () => json({}, 401), 'POST /device/confirm': () => new Response(null, { status: 200 }) })
  renderApp([{ path: '/', element: <LinkDeviceForm /> }])
  await user.type(await screen.findByLabelText('Код из игры'), 'abcd1234')
  await user.click(screen.getByRole('button', { name: 'Привязать' }))
  expect(await screen.findByText(/Игра привязана/)).toBeInTheDocument()
  const body = fetch.mock.calls.find(([u]) => String(u).endsWith('/device/confirm'))?.[1]?.body
  expect(body).toBe('{"user_code":"ABCD-1234"}')
})

test('expired code explains what to do', async () => {
  const user = userEvent.setup()
  mockFetch({ 'POST /auth/refresh': () => json({}, 401), 'POST /device/confirm': () => json({ error: 'unknown or expired user_code' }, 404) })
  renderApp([{ path: '/', element: <LinkDeviceForm /> }])
  await user.type(await screen.findByLabelText('Код из игры'), 'ZZZZ-ZZZZ')
  await user.click(screen.getByRole('button', { name: 'Привязать' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Коды живут 10 минут')
})
```

- [ ] **Step 2: Implement** per interfaces (`useMutation` for the confirm call), add `{ path: '/link', lazy: … LinkPage }` under `RequireAuth` in `routes.tsx`.

- [ ] **Step 3: pass; commit** — `feat(frontend): link the game with a device code`

---

### Task 7: Account page (`/account`) — profile, avatar, devices

**Files:** Create `src/features/account/api.ts`, `src/features/account/components/{ProfileCard,AvatarUpload,DeviceList}.tsx`, `src/features/account/tests/account.test.tsx`, `src/pages/app/AccountPage.tsx`; modify `src/routes.tsx`.

**Interfaces:**
- `api.ts`: `type DeviceLink = { id: string; linked_at: string; last_seen: string | null }`; `uploadAvatar(file: File): Promise<{ avatar_url: string }>` (FormData field `file`, `POST /avatars`); `listDevices(): Promise<DeviceLink[]>` (`GET /device/links`); `revokeDevice(id: string): Promise<void>` (`DELETE /device/links/{id}`); `checkAvatarFile(file: File): string | undefined` — rejects non `image/png|jpeg|webp` («Подойдёт PNG, JPG или WebP.») and > 5 MB («Файл больше 5 МБ.»).
- `ProfileCard`: avatar (or initial), nick (Comfortaa 25px), email (frost), «С нами с {date}» (`Intl.DateTimeFormat('ru-RU', { dateStyle: 'long' })`), «Выйти» (quiet) → `signOut()` → navigate `/`.
- `AvatarUpload`: visually a button «Сменить аватар» wrapping a hidden `<input type="file" accept="image/png,image/jpeg,image/webp">` (label-based, keyboard reachable); client check → upload → `invalidateQueries(['me'])`; errors via `Notice`.
- `DeviceList`: heading «Привязанные игры»; each row: «Привязано {date}», «Последний вход: {relative or 'ещё не заходил'}», button «Отвязать» (danger) with a confirm step (button turns into «Точно отвязать?» + «Отмена» for that row); success removes the row (`invalidateQueries(['devices'])`). Empty: «Пока ни одной игры. Привяжи мод на странице «Привязать игру».» with a link.
- Page: two columns on ≥ 900px (profile left, devices right), stacked below.

- [ ] **Step 1: Failing tests** (`account.test.tsx`)

```tsx
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { checkAvatarFile } from '../api'
import { DeviceList } from '../components/DeviceList'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

test('avatar file checks', () => {
  expect(checkAvatarFile(new File(['x'], 'a.gif', { type: 'image/gif' }))).toBe('Подойдёт PNG, JPG или WebP.')
  const big = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'a.png', { type: 'image/png' })
  expect(checkAvatarFile(big)).toBe('Файл больше 5 МБ.')
  expect(checkAvatarFile(new File(['x'], 'a.png', { type: 'image/png' }))).toBeUndefined()
})

test('revoking a device asks for confirmation first', async () => {
  const user = userEvent.setup()
  let devices = [{ id: 'd1', linked_at: '2026-09-20T10:00:00Z', last_seen: null }]
  const fetch = mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /device/links': () => json(devices),
    'DELETE /device/links/d1': () => { devices = []; return new Response(null, { status: 204 }) },
  })
  renderApp([{ path: '/', element: <DeviceList /> }])
  const row = await screen.findByRole('listitem')
  await user.click(within(row).getByRole('button', { name: 'Отвязать' }))
  expect(fetch.mock.calls.some(([, i]) => i?.method === 'DELETE')).toBe(false)
  await user.click(within(row).getByRole('button', { name: 'Точно отвязать?' }))
  expect(await screen.findByText(/Пока ни одной игры/)).toBeInTheDocument()
})
```

- [ ] **Step 2: Implement; add `/account` route under `RequireAuth`.**
- [ ] **Step 3: pass; commit** — `feat(frontend): account page with avatar upload and linked games`

---

### Task 8: My configs (`/configs`)

**Contract:** `backend/configs-service/PLAN.md` Tasks 2–5. Until configs-service is running, the page is exercised through tests (mocked fetch) only.

**Files:** Create `src/features/configs/{api.ts,types.ts}`, `src/features/configs/components/{SlotGrid,SlotPanel,PublishForm}.tsx`, `src/features/configs/tests/configs.test.tsx`, `src/pages/app/ConfigsPage.tsx`; modify `src/routes.tsx`.

**Interfaces:**
- `types.ts`: `SlotSummary = { slot: 1 | 2 | 3 | 4; name: string; updated_at: string; share_code: string; published: boolean }`.
- `api.ts`: `listSlots(): Promise<SlotSummary[]>` (`GET /configs`), `regenerateCode(slot): Promise<{ share_code: string }>`, `publish(slot, title, description): Promise<{ listing_id: string }>`, `unpublish(slot): Promise<void>`. Query key `['configs']`; every mutation invalidates it.
- `SlotGrid`: always renders 4 `SlotPanel`s (2×2 ≥ 640px, 1 column below), filling empty ones from the list. Page lead: «Конфиги сохраняются из мода командой `%config save <слот>`. Здесь — коды и публикация.» (command in Iosevka).
- `SlotPanel` (filled): «Слот N», name (Comfortaa 20px), «Обновлён {relative}», `ShareCode` with label «Код для друзей», hint under it: «Друг вводит в игре: %config load {code}» (Iosevka), buttons «Новый код» (quiet; confirm step: «Старый код перестанет работать. Сменить?» + «Сменить» / «Отмена») and «Опубликовать» / «Убрать с витрины» (danger, confirm step) depending on `published`. Empty: «Слот N», «Пусто», one line «Сохрани сюда конфиг из мода: %config save N».
- `PublishForm` (inline in the panel, not a modal): «Название на витрине» (1..64), «Описание» textarea (≤500, counter «{n}/500»), «Опубликовать» / «Отмена». Success → Notice info «Опубликовано. Конфиг виден на витрине.»

- [ ] **Step 1: Failing tests** — cover: 4 panels always rendered with 1 filled; «Новый код» requires confirm and then shows the new code from the response; publish validates empty title client-side and posts `{title, description}`; published slot shows «Убрать с витрины».

```tsx
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test } from 'vitest'
import { SlotGrid } from '../components/SlotGrid'
import { renderApp } from '../../../test/render'
import { json, mockFetch } from '../../../test/fetch'

const filled = { slot: 2, name: 'pvp', updated_at: '2026-09-24T10:00:00Z', share_code: '7KQ3M9XA', published: false }

test('always shows four slots', async () => {
  mockFetch({ 'POST /auth/refresh': () => json({}, 401), 'GET /configs': () => json([filled]) })
  renderApp([{ path: '/', element: <SlotGrid /> }])
  expect(await screen.findAllByRole('article')).toHaveLength(4)
  expect(screen.getByText('7KQ3M9XA')).toBeInTheDocument()
  expect(screen.getAllByText('Пусто')).toHaveLength(3)
})

test('regenerating a code needs confirmation', async () => {
  const user = userEvent.setup()
  let code = '7KQ3M9XA'
  mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /configs': () => json([{ ...filled, share_code: code }]),
    'POST /configs/2/regenerate-code': () => { code = 'NEWC0DE2'; return json({ share_code: code }) },
  })
  renderApp([{ path: '/', element: <SlotGrid /> }])
  const panel = (await screen.findByText('pvp')).closest('article') as HTMLElement
  await user.click(within(panel).getByRole('button', { name: 'Новый код' }))
  expect(within(panel).getByText(/Старый код перестанет работать/)).toBeInTheDocument()
  await user.click(within(panel).getByRole('button', { name: 'Сменить' }))
  expect(await within(panel).findByText('NEWC0DE2')).toBeInTheDocument()
})

test('publish requires a title and sends the form', async () => {
  const user = userEvent.setup()
  const fetch = mockFetch({
    'POST /auth/refresh': () => json({}, 401),
    'GET /configs': () => json([filled]),
    'POST /configs/2/publish': () => json({ listing_id: 'l1' }),
  })
  renderApp([{ path: '/', element: <SlotGrid /> }])
  const panel = (await screen.findByText('pvp')).closest('article') as HTMLElement
  await user.click(within(panel).getByRole('button', { name: 'Опубликовать' }))
  await user.click(within(panel).getByRole('button', { name: 'Опубликовать' }))
  expect(within(panel).getByLabelText('Название на витрине')).toHaveAttribute('aria-invalid', 'true')
  await user.type(within(panel).getByLabelText('Название на витрине'), 'Лучший PvP')
  await user.click(within(panel).getByRole('button', { name: 'Опубликовать' }))
  expect(await within(panel).findByText(/Опубликовано/)).toBeInTheDocument()
  const body = fetch.mock.calls.find(([u]) => String(u).endsWith('/publish'))?.[1]?.body
  expect(JSON.parse(String(body))).toEqual({ title: 'Лучший PvP', description: '' })
})
```
(Each `SlotPanel` renders as `<article aria-labelledby=…>`. While `PublishForm` is open, the panel's own «Опубликовать» button is hidden so the form's submit is the only one with that name.)

- [ ] **Step 2: Implement; `/configs` route under `RequireAuth`.**
- [ ] **Step 3: pass; commit** — `feat(frontend): my configs with share codes and publishing`

---

### Task 9: Showcase (`/showcase`, `/showcase/:id`)

**Contract:** `backend/configs-service/PLAN.md` Task 5.

**Files:** Create `src/features/showcase/{api.ts,types.ts}`, `src/features/showcase/components/{ListingList,ListingItem,ListingDetail,CopyToSlot}.tsx`, `src/features/showcase/tests/showcase.test.tsx`, `src/pages/showcase/{ShowcasePage,ListingPage}.tsx`; modify `src/routes.tsx`.

**Interfaces:**
- `types.ts`: `Author = { nick: string; avatar_url: string | null }`; `Listing = { id: string; title: string; description: string; config_name: string; copies_count: number; published_at: string; author: Author | null }`; `ListingDetail = Listing & { data: unknown }`; `ShowcasePage = { items: Listing[]; page: number; has_more: boolean }`.
- `api.ts`: `browse(sort: 'new' | 'popular', page: number)` (key `['showcase', sort, page]`), `getListing(id)` (key `['listing', id]`), `copyToSlot(id, slot)` → `POST /showcase/{id}/copy {slot}`.
- `ShowcasePage`: h1 «Витрина», lead «Конфиги, которыми поделились игроки. Понравился — скопируй в свой слот.», sort toggle «Новые» / «Популярные» (two buttons, `aria-pressed`, synced to `?sort=`), list, pager «Назад» / «Дальше» (synced to `?page=`, «Дальше» disabled when `!has_more`). Anonymous access allowed.
- `ListingItem` (`<article>`, links to detail): title, author nick (or «Автор скрыт» when `author` is null), config name, «Скопировали {n} раз» with correct Russian plural (`Intl.PluralRules('ru')`: раз/раза/раз), date.
- `ListingDetail`: title, description (preserve line breaks, `whitespace-pre-line`), author, stats; `CopyToSlot` below.
- `CopyToSlot`: anonymous → «Войди, чтобы скопировать конфиг к себе» (link to `/login` with `state.from`); authenticated → four slot buttons «Слот 1»…«Слот 4», occupied slots (from `['configs']` query) disabled with `title="Занят"`; success → Notice «Скопировано в слот N» + link «Открыть мои конфиги»; 409 → «Этот слот уже занят. Выбери свободный или освободи слот в моде.»
- Empty list: «На витрине пока пусто. Опубликуй свой конфиг на странице «Мои конфиги».»

- [ ] **Step 1: Failing tests** — cover: list renders items + pluralization (1 раз / 2 раза / 5 раз); sort toggle updates `?sort=popular` and refetches; anonymous detail shows the login prompt; authenticated copy disables occupied slots and posts `{slot}`; 409 message.

```tsx
import { expect, test } from 'vitest'
import { copiesLabel } from '../components/ListingItem'

test('russian plural for copies', () => {
  expect(copiesLabel(1)).toBe('Скопировали 1 раз')
  expect(copiesLabel(3)).toBe('Скопировали 3 раза')
  expect(copiesLabel(5)).toBe('Скопировали 5 раз')
  expect(copiesLabel(21)).toBe('Скопировали 21 раз')
})
```
(plus rendering tests in the same file, same `renderApp` + `mockFetch` pattern as Tasks 7–8; export `copiesLabel` from `ListingItem.tsx`.)

- [ ] **Step 2: Implement; routes `/showcase` and `/showcase/:id` (public, lazy).**
- [ ] **Step 3: pass; commit** — `feat(frontend): public showcase with sorting, detail and copy to slot`

---

### Task 10: Visual QA pass + docs

**Files:** any `src/**` fixes; `public/favicon.svg` (replace Vite's), `frontend/ARCHITECTURE.md`, `TODO.md` (Фаза 10 status).

- [ ] **Step 1:** `bun run dev`; with Chrome DevTools MCP (or a browser) take screenshots of `/`, `/login`, `/register`, `/showcase` (mocked empty state is fine — with no backend running, refresh fails → anonymous; showcase shows the network error Notice, which must read well too) at 1280×800 and 375×812.
- [ ] **Step 2:** Critique against the Design section: one accent only, panels only where specified, no horizontal scroll at 375px, visible focus on every interactive element (tab through each page), contrast of `frost` text on `slate` panels ≥ 4.5:1, hero typing runs once. Fix and re-screenshot.
- [ ] **Step 3:** Favicon: simple SVG — ice rounded square (10px radius shape scaled) with a snow «L» in Comfortaa-like geometry; `theme-color` already set in Task 2.
- [ ] **Step 4:** `TODO.md`: frontend Подсистема 1 done; next = mod integration (device link + `%config` commands against the gateway).
- [ ] **Step 5:** test/build/lint; commit — `feat(frontend): visual QA fixes, favicon, docs`

---

### Task 11: Easter eggs (owner-approved, see TODO.md «Пасхалки»)

**Files:** `src/features/easter/{konami.ts,devtoolsBanner.ts,BreakableBlock.tsx}`, `src/features/easter/i18n/{ru,en}.ts`, `src/features/easter/tests/easter.test.tsx`; modify `pages/public/NotFoundPage.tsx`, landing Hero/ClickGui (Konami hook), `src/main.tsx` (banner).

**Interfaces / behaviour:**
- `useKonami(onTrigger)`: listens to `keydown` for ↑↑↓↓←→←→BA (`KeyB`, `KeyA`), ignores keys typed inside inputs/textareas; on trigger the landing ClickGui toggles `TrollfaceMask` on, switches to a cycling rainbow accent (hue rotates over 6s, static under reduced motion) and shows a toast «Режим тролля включён» (ru) / «Troll mode on» (en); a second Konami reverts.
- `printDevtoolsBanner()`: called once from `main.tsx` in production and dev (not in tests): `console.log('%c<ASCII LoVisual logo>', 'color:#5cc8e7;font-family:monospace')` + `console.log('%cШаришь? Исходники аддонов — на витрине: <origin>/addons', …)` (en variant by current language).
- 404 page: an empty dusk voxel world (reuse `VoxelScene`) where one grass block falls from the top with a small bounce (`motion`), then sits; clicking/pressing Enter on it plays a crack animation over 3 clicks (Minecraft-style break stages drawn with CSS), then bursts into square particles and a new block falls. Text «Такой страницы нет. Зато есть блок.» + link «На главную». Keyboard accessible (`button`), reduced motion → no fall/burst, just crack stages.
- `IDDQD`: the configs page / share-code input and showcase «загрузить по коду» (when added) accept `IDDQD` (case-insensitive) — backend serves it (configs-service Task 7); no frontend special-casing needed beyond not rejecting 5-char codes client-side.

- [ ] Tests: Konami sequence triggers the callback, inputs are ignored; banner prints two `console.log` calls with `%c`; 404 block breaks after 3 activations. Implement; commit `feat(frontend): easter eggs — konami troll mode, devtools banner, breakable 404 block`.

---

## Deferred

- Admin panel (`/admin`) — Подсистема 3 plan.
- Friends/chat/presence UI — Подсистема 2 plan.
- Showcase search/filters, config diff preview — after real usage.
- i18n (English UI) — not requested.
