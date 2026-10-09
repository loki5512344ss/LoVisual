<div align="center">

# LoVisual

Open-source Minecraft utility client **and** the platform around it - website, cloud config sharing, and a Rust backend.

Developed and maintained by [loki5512344](https://github.com/loki5512344).

![Release](https://img.shields.io/gitea/v/release/boba/LoVisual?gitea_url=https%3A%2F%2Fgit.wihuk.pro&style=flat-square&color=blue)
![Java](https://img.shields.io/badge/Java-25+-orange?style=flat-square&logo=openjdk&logoColor=white)
![Rust](https://img.shields.io/badge/Rust-backend-red?style=flat-square&logo=rust&logoColor=white)
![React](https://img.shields.io/badge/Frontend-React%20%2B%20Vite-61dafb?style=flat-square&logo=react&logoColor=white)
![License](https://img.shields.io/badge/license-GPLv3-blue?style=flat-square&logo=gnu&logoColor=white)

**[⬇ Download the mod](https://git.wihuk.pro/boba/LoVisual/releases/latest/download/lovisual.jar)** · [Releases](https://git.wihuk.pro/boba/LoVisual/releases)

[English](#english) | [Русский](#русский)

</div>

---

## English

### What is this?

LoVisual is three things in one repository:

| Component | What it is | Stack |
|-----------|------------|-------|
| [`mod/`](mod) | The Minecraft client itself - 60+ modules, ClickGUI, HUD editor, theme system, custom renderer | Java 25 · Fabric · OpenGL/Vulkan |
| [`frontend/`](frontend) | The website - downloads, docs, live demos of the in-game GUI, account & config pages | React 19 · Vite · TypeScript · Tailwind · bun |
| [`backend/`](backend) | The platform services - API gateway, accounts, cloud config slots with share codes | Rust · Axum · PostgreSQL · gRPC |

The pieces talk to each other: link the mod to your platform account with `%link`, push a config
to a cloud slot with `%config save <1-4>`, and a friend imports it by code with `%config load <code>`.

### Mod highlights

- **Combat / Visuals / Player / Misc** - Reach, ESP, NameTags, Chams, FullBright, Freecam, AutoTool, FakePlayer and more (see [mod README](mod/README.md) for the full list)
- **ClickGUI** on `RShift` - module browser, settings, theme editor, config profiles
- **HUD editor** - draggable and static elements: TargetHUD, ModuleList, Watermark, Keystrokes…
- **Cloud configs** - 4 slots per account, short share codes, public showcase
- **`.lvcfg` profiles** - export/import modules, HUD and themes locally, no account needed
- **Addon API** - versioned API for extending the client

Requires Fabric Loader, Fabric API and Sodium; optional compatibility with Iris, ViaFabricPlus,
Xaero's Minimap/World Map and more.

### Repository layout

```
├── mod/         Fabric client (Gradle, Java)      → mod/README.md
├── frontend/    marketing + dashboard site (bun)  → frontend/README.md
├── backend/     gateway, accounts-, configs-service → backend/STRUCTURE.md
├── docs/        cross-cutting docs                 → docs/README.md
└── ref/         reference material (not shipped)
```

### Development

Mod (from `mod/`):

```bash
./gradlew build        # compile + remap jar into build/libs
./gradlew runClient    # launch a dev Minecraft client
./gradlew test         # unit tests
```

Frontend (from `frontend/`, requires bun):

```bash
bun install
bun run dev            # http://localhost:5173
bun run test           # vitest
bun run build          # type-check + production bundle
```

Backend (from `backend/`, requires Rust + PostgreSQL):

```bash
cargo build
cargo test
```

Releases are cut by pushing a `v*` tag - Forgejo Actions builds the mod jar and publishes it to
[Releases](https://git.wihuk.pro/boba/LoVisual/releases) as both `lovisual-<version>.jar`
and a stable `lovisual.jar` link.

### License

GNU General Public License v3.0. See [LICENSE](mod/LICENSE).

---

## Русский

### Что это?

LoVisual - три проекта в одном репозитории:

| Компонент | Что это | Стек |
|-----------|---------|------|
| [`mod/`](mod) | Сам клиент для Minecraft - 60+ модулей, ClickGUI, редактор HUD, темы, свой рендер | Java 25 · Fabric · OpenGL/Vulkan |
| [`frontend/`](frontend) | Сайт - загрузки, документация, живые демо внутриигрового GUI, страницы аккаунта и конфигов | React 19 · Vite · TypeScript · Tailwind · bun |
| [`backend/`](backend) | Сервисы платформы - API-шлюз, аккаунты, облачные слоты конфигов с кодами расшаривания | Rust · Axum · PostgreSQL · gRPC |

Части связаны: привяжи мод к аккаунту командой `%link`, залей конфиг в облачный слот через
`%config save <1-4>`, друг заберёт его по коду командой `%config load <code>`.

### Возможности мода

- **Combat / Visuals / Player / Misc** - Reach, ESP, NameTags, Chams, FullBright, Freecam, AutoTool, FakePlayer и другие (полный список - в [README мода](mod/README.md))
- **ClickGUI** на `RShift` - браузер модулей, настройки, редактор тем и профилей
- **Редактор HUD** - перетаскиваемые и статические элементы: TargetHUD, ModuleList, Watermark, Keystrokes…
- **Облачные конфиги** - 4 слота на аккаунт, короткие коды, публичный показ
- **Профили `.lvcfg`** - локальный экспорт/импорт модулей, HUD и тем без аккаунта
- **Addon API** - версионированное API для расширения клиента

Нужны Fabric Loader, Fabric API и Sodium; опционально - Iris, ViaFabricPlus, Xaero's Minimap/World Map и др.

### Структура репозитория

```
├── mod/         клиент Fabric (Gradle, Java)       → mod/README.md
├── frontend/    сайт-витрина и дашборд (bun)       → frontend/README.md
├── backend/     gateway, accounts-, configs-service → backend/STRUCTURE.md
├── docs/        сквозная документация               → docs/README.md
└── ref/         референсы (не поставляется)
```

### Разработка

Мод (из `mod/`):

```bash
./gradlew build        # сборка и remap jar в build/libs
./gradlew runClient    # запуск dev-клиента Minecraft
./gradlew test         # модульные тесты
```

Фронтенд (из `frontend/`, нужен bun):

```bash
bun install
bun run dev            # http://localhost:5173
bun run test           # vitest
bun run build          # типы + production-сборка
```

Бэкенд (из `backend/`, нужны Rust и PostgreSQL):

```bash
cargo build
cargo test
```

Релизы создаются пушем тега `v*` - Forgejo Actions собирает jar мода и публикует в
[Releases](https://git.wihuk.pro/boba/LoVisual/releases) как `lovisual-<версия>.jar`
и по стабильной ссылке `lovisual.jar`.

### Лицензия

GNU General Public License v3.0. См. [LICENSE](mod/LICENSE).
