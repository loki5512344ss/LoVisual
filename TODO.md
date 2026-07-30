# LoVisual — План разработки с нуля

## Принципы (в отличие от прошлого кала)
- **Никаких пустышек** — файл без реализации не создаётся
- **Никаких this-escape** — конструктор не публикует this
- **Никакого статика** — `Minecraft.getInstance()` только лениво, никогда в поле
- **Никаких мёртвых миксинов** — миксин только если он реально нужен
- **Каждый этап компилируется** — перед переходом к следующему
- **Никаких 140 папок** — плоская структура, группировка только по смыслу

---

## База: что осталось
- `ref/` — ~415 референс-модулей из 8 клиентов (не трогать)
- `.omo/` — конфиги opencode
- `.git/`, `.gitignore`, `.gitattributes`
- `SYSTEM.md` — описание архитектуры (актуализировать по ходу)

---

## Фаза 0: Project Setup
- [ ] Создать `mod/` (NeoForge 1.21.1, Gradle, Java 21)
  - build.gradle.kts (userdev, checkstyle)
  - settings.gradle.kts
  - META-INF/neoforge.mods.toml
  - access_transformer.cfg (только нужные записи)
  - mixins.json (только файлы, которые реально есть)
  - LoVisual.java — entry point
  - LoVisualClient.java — client init
- [ ] Создать `backend/` (Go 1.24)
  - go.mod
  - cmd/server/main.go
  - internal/config/config.go (!! существует до main.go)
- [ ] Создать `launcher/` (Rust)
  - Cargo.toml
  - src/main.rs

### Проверка: `./gradlew build`, `go build ./cmd/server`, `cargo build`

---

## Фаза 1: Core Systems (мод)
- [ ] EventBus — свой, нестатический, с поддержкой cancellation
  - Event.java (abstract), EventBus.java
  - EventHandler аннотация
  - post() прекращает итерацию при cancel
- [ ] Module base — без `Minecraft.getInstance()` в статике
  - Module.java: lazy getMc(), рефлексия для settings, toggle/enable/disable
  - ModuleInfo аннотация
  - ModuleCategory enum (COMBAT, MOVEMENT, RENDER, PLAYER, MISC)
- [ ] Setting system — 6 типов:
  - Setting<T>, BooleanSetting, SliderSetting, ModeSetting, ColorSetting, BindSetting, MultiSelectSetting
- [ ] ModuleManager — регистрация, получение по имени/категории
- [ ] Command system — toggle, bind, help, config
- [ ] ConfigManager — .loconf, профили, автосейв (только после полной инициализации)
- [ ] Theme / ThemeManager — без garbage (Color кэш или прямые int)

### Проверка: мод запускается, не падает, `/lovisual help` работает

---

## Фаза 2: Mixins (только нужные)
- [ ] MinecraftClientMixin — runTick() → TickEvent
- [ ] InGameHudMixin — render → Render2DEvent
- [ ] GameRendererMixin — renderLevel → Render3DEvent
- [ ] ClientConnectionMixin — send/receive → PacketEvent
- [ ] KeyboardHandlerMixin — keyPress → KeyPressEvent
- [ ] MouseHandlerMixin — click → MouseClickEvent
- [ ] ClientPlayerEntityMixin — travel → MoveEvent (если нужен)

### НИКАКИХ пустых миксинов. Никаких `cancellable=true` без cancel.

### Проверка: все события постится, можно залогировать из любого модуля

---

## Фаза 3: Первый рабочий модуль
- [ ] Sprint (MOVEMENT) — полный цикл:
  - Вкл/выкл через `/lovisual toggle Sprint`
  - onEnable → подписка, onDisable → отписка
  - TickEvent → зажим CTRL
  - Сохраняется в конфиг
  - Клавиша привязывается через `/lovisual bind Sprint 29`

### Проверка: модуль работает, конфиг сохраняется/грузится

---

## Фаза 4: Modules — по одному, каждый рабочий

### Combat (4)
- [ ] AutoClicker — CPS на клик, не юзать System.currentTimeMillis()
- [ ] Trajectories — Render3DEvent, предсказание траектории
- [ ] Criticals — фолл-криты
- [ ] Velocity — анти-отбрасывание

### Movement (3)
- [ ] Sprint ↑ (готов)
- [ ] Speed — страф/вода/эйр
- [ ] AutoWalk — зажим W

### Render (12)
- [ ] FullBright — гамма
- [ ] NoHurtCam — отмена тряски
- [ ] NoWeather — отмена погоды
- [ ] NoBobbing — отмена качания
- [ ] Zoom — FOV smooth (без глитча при disable)
- [ ] Crosshair — кастомный
- [ ] Nametags — ники над игроками
- [ ] HealthTags — хп под никами
- [ ] HitParticles — частицы при ударе
- [ ] Chams — подсветка (миксин читает настройки модуля)
- [ ] Animations — свайпы/рука
- [ ] CustomGlint — кастомный глинт

### Player (8)
- [ ] AutoTool — автосмена инструмента
- [ ] ExpThrower — авто-бросок опыта
- [ ] InvManager — сортировка/мусор
- [ ] MiddleClickFriend — добавить в друзья
- [ ] InventorySort — сортировка по клику
- [ ] Refill — авто-заполнение стаков
- [ ] NoInteract — блокировка кликов по сущностям
- [ ] Friends — список друзей

### Misc (6)
- [ ] AntiAFK — анти-афк
- [ ] AutoRespawn — авто-респавн
- [ ] AutoReconnect — авто-переподключение
- [ ] IRC — WebSocket чат
- [ ] Spammer — авто-спам в чат
- [ ] NameProtect — защита ника

### Extra (из ref/: только если реально нужны)
- [ ] KillAura
- [ ] ESP
- [ ] Scaffold
- [ ] Freecam
- [ ] Blink
- [ ] ElytraFlight
- [ ] AntiBot
- [ ] HitBox
- [ ] Tracers
- [ ] ItemPhysics
- [ ] ChestStealer
- [ ] AutoFish
- [ ] AutoEat

### Проверка после каждого: модуль компилируется, включается/выключается, не крашит

---

## Фаза 5: GUI

### ClickGUI
- [ ] ClickGuiScreen — Screen
- [ ] CategoryPanel — чипсы категорий + поиск
- [ ] ModuleGrid — сетка модулей (ЛКМ toggle, ПКМ настройки, СКМ бинд)
- [ ] SettingsPanel — рендер настроек
- [ ] ThemePanel — слайдеры цветов

### HUD (10 элементов)
- [ ] Watermark
- [ ] ModuleList
- [ ] TargetHUD
- [ ] ArmorHUD
- [ ] PotionsHUD
- [ ] Keystrokes
- [ ] Scoreboard
- [ ] Coords
- [ ] ComboCounter
- [ ] Notifications

### MainMenu
- [ ] LoVisualMainMenu — кастомный экран

### Проверка: GUI рендерится, кликабельно, модули togglятся

---

## Фаза 6: Backend (Go)

### Config (существует до main.go)
- [ ] internal/config/config.go — Port, DB.Path, JWT.Secret, CORS origins

### DB
- [ ] internal/repository/db.go — SQLite, миграции
- [ ] internal/repository/users.go
- [ ] internal/repository/configs.go
- [ ] internal/repository/profiles.go

### Auth
- [ ] internal/service/auth.go — bcrypt, JWT (не SHA256)
- [ ] internal/handler/auth.go — register/login/logout
- [ ] internal/middleware/auth.go — Bearer token (не panic, возвращает 401)

### Profile
- [ ] internal/service/profile.go
- [ ] internal/handler/profile.go — get/update/playtime/search/friends
- [ ] JSON decode errors НЕ игнорировать

### Configs API
- [ ] internal/handler/config.go — upload/download (auth check, user_id из токена, не из тела)
- [ ] 16-byte share keys (минимум)

### IRC
- [ ] internal/irc/hub.go — WebSocket хаб (gorilla/websocket, не deprecated x/net)

### Middleware
- [ ] CORS — настраиваемые origins
- [ ] RateLimit — с cleanup горутиной (без утечки)
- [ ] Log — запросы

### Проверка: `go build ./cmd/server` проходит, сервер стартует, эндпоинты отвечают

---

## Фаза 7: Launcher (Rust)

### API клиент
- [ ] api.rs — reqwest, таймауты, десериализация с rename_all
- [ ] Версия читается из Cargo.toml (не хардкод)

### Экраны
- [ ] login.rs — регистрация/логин
- [ ] dashboard.rs — профиль, playtime, version check
- [ ] settings.rs — кастомная команда (ЧИТАЕТСЯ на Play, не stale)

### Launch
- [ ] launch.rs — запуск MC (команда из настроек, защита от пустой строки)
- [ ] save_session — только при логине, при logout удалять

### Проверка: `cargo run` — лаунчер открывается, логин/регистрация работают

---

## Фаза 8: Polish

- [ ] Sound system (49 звуков из ref/)
- [ ] Animation system (SpringAnimator)
- [ ] Style system (6 стилей для HUD)
- [ ] Cloud config sharing (если бэкенд готов)
- [ ] Auto-updater
- [ ] Loading screen
- [ ] AltManager

---

