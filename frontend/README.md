# LoVisual Website

The frontend for [LoVisual](../README.md) - a marketing site with a live in-game GUI demo,
download page, documentation, themes/showcase pages, and an account dashboard with a cloud
config manager. UI is fully bilingual (Russian default, English toggle, persisted in the URL).

## Stack

- **React 19 + TypeScript + Vite**, package manager: **bun**
- **Tailwind CSS v4** with custom design tokens (radii, glass panels, control sizing)
- **TanStack Query** for backend calls, **react-router** for pages
- **react-i18next** - `ru`/`en` resources per feature, compile-time key parity via `satisfies`
- **motion/react** for animation, **vitest + Testing Library** for tests
- **oxlint** for linting

## Layout

```
src/
├── app/         router, providers, App shell
├── features/    one folder per domain (account, cloud, demo, themes, ...)
│   └── <f>/     api.ts · i18n/{ru,en}.ts · components/ · tests/
├── pages/       route-level pages composing features
├── shared/      ui primitives, i18n bootstrap, easter eggs, helpers
└── test/        vitest setup and utilities
```

Feature-folder rules: API calls, translations and components live together; no cross-feature
imports except through `shared/`.

## Scripts

```bash
bun install        # install dependencies
bun run dev        # dev server on http://localhost:5173 (proxies /api to the backend)
bun run test       # vitest run
bun run build      # tsc -b + vite build → dist/
bun run lint       # oxlint
bun run gen:themes # regenerate theme tokens from the mod palette (scripts/extract-themes.ts)
```

The dev server expects the Rust gateway on `127.0.0.1:8080` (see [`backend/`](../backend));
without it, API-backed pages degrade to demo/offline states by design.

## Testing

Unit and component tests are colocated in `features/*/tests/` and `shared/**/tests/`;
run a single file with `bunx vitest run src/features/account/tests/account.test.tsx`.
Translation parity (ru ↔ en key sets) is enforced at compile time and in tests.
