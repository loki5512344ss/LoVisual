# LoVisual

Проект: клиентский мод (`mod/`, бывш. `combatant-client-26.2/`) + платформа
(`backend/`, `frontend/` — сайт+бэкенд), см. Фазу 10/Подсистемы 1-3 ниже.

> Старый лаунчер (Rust + Slint) и старый backend (Rust + axum, другой дизайн)
> были удалены раньше (решение 2026-09-05) — `backend/` и `frontend/` сейчас
> строятся заново по дизайну из Фазы 10 / Подсистем 1-3, это не восстановление
> старого кода.

> **Коммиты — на английском** (решение 2026-09-23, действует с этого момента
> вперёд; более старые коммиты на русском не переписываем).

> **TEMP-NOTE (удалить этот блок, когда сделано):** после окончания текущего
> рефакторинга владелец squash'ит историю git с 100+ коммитов до ~20, затем
> пушит и открывает репозиторий из приватного в публичный (сейчас приват —
> ранний код был спижен). Это план владельца, не техническая задача агента —
> просто держать коммиты чистыми/по делу до этого момента.

---

## Правила (для мода, продублированы в его TODO)
- KISS / DRY / SOLID
- Макс. 200 строк на файл (фасады-делегаты исключение)
- Макс. 4 файла на папку
- Никакого статика, никаких this-escape, никаких мёртвых миксинов
- Без пустышек; каждый этап компилируется (`./gradlew build`)
- Тесты обязательны для новых модулей (JUnit)

---

## Правила платформы (`backend/`, `frontend/`) — исследовано 2026-09-23

> KISS/DRY/SOLID и "без пустышек, тесты обязательны" из правил мода действуют
> и здесь без изменений. Ниже — то, что отличается или уточняется под
> Rust/Axum и React/Vite, по итогам ресёрча актуальных практик и антипаттернов.

**Лимит на файл: ≤250 строк (не 200, как у мода)** — сознательно шире:
у мода лимит калибровался под Java (многословный синтаксис, много
boilerplate на класс), а в Rust/TypeScript тот же объём логики обычно
компактнее на строку, но модуль (структуры + impl + тесты в одном файле,
как в `backend/PLAN.md`) естественно тянет чуть больше. Фасады-делегаты —
то же исключение, что и у мода.

**Макс. 4 файла на папку** — то же правило, что у мода, без изменений:
превышение — сигнал выносить смысловую подпапку. Считаются только файлы,
подпапки в лимит не входят (проверено на живом коде мода: `addon/` — 0
файлов + 4 подпапки).

**Backend (Rust/Axum) — паттерны:**
- Модуль на домен (`auth/`, `accounts/`, `device/`, `avatars/`), а не по
  техническому слою (`handlers/`, `models/`, `services/` вперемешку) —
  так одну фичу не размазывает по всему дереву папок
- Repository-паттерн: SQL живёт только в `*/repo.rs`, хендлеры его не видят
  напрямую (уже так в `backend/PLAN.md`)
- Собственный `enum AppError` вместо `String`/`anyhow` как типа ошибки на
  границе HTTP — `String` теряет контекст и заставляет аллоцировать на
  каждой ошибке
- Axum-экстракторы (`Json<T>`, `State<T>`) — валидация на границе запроса
  компилятором, а не руками в теле хендлера

**Антипаттерны — сознательно избегаем:**
- `.unwrap()`/`.expect()` на данных из запроса — только `?` с конвертацией
  через `From<...> for AppError` (уже в `Global Constraints` плана)
- Чрезмерный `.clone()` вместо ссылок — первый признак борьбы с borrow
  checker'ом не по делу
- Блокирующий I/O внутри `async fn` (напр. синхронный файловый API) —
  душит рантайм tokio целиком, а не только один запрос
- "Бог-структуры" (`AppState` со всем на свете) — состояние делится по
  доменам (`AuthState`, `DeviceState`, `AvatarState` — уже так в плане)

**Frontend (React/Vite) — структура фичами, не по типу файла:**
Один из самых частых промахов — раскладывать по `/components`, `/hooks`,
`/services` вперемешку: фича размазывается по всему дереву, и удалить её
целиком нельзя, не зацепив другие фичи. Вместо этого — `src/features/<имя>/`
хранит свои компоненты+хуки+API-вызовы+типы вместе; общее (`src/shared/`)
только для того, что реально используют 2+ фичи. Детали — в
`frontend/ARCHITECTURE.md`.

Источники: [Rustify — Axum Guide 2026](https://rustify.rs/articles/rust-backend-development-axum-2026), [Rust FAQ — Common Anti-Patterns](https://www.rustfaq.org/en/common-rust-anti-patterns-and-how-to-avoid-them/), [Rust Design Patterns — Anti-patterns](https://rust-unofficial.github.io/patterns/anti_patterns/), [Robin Wieruch — React Folder Structure 2026](https://www.robinwieruch.de/react-folder-structure/).

---

## Статус
- [x] Фаза 1: Ребрендинг `combatant` → `lovisual`
- [x] Фаза 2: Выкинуть Runtime/JarReplacement + Panic
- [x] Фаза 3: Разбивка 12 файлов-гигантов
- [x] Фаза 3.5: Удаление читерских модулей (легит-направление)
- [x] Фаза 4: Security (BackdoorProtection, StaffTracker)
- [x] Фаза 5: Нормализация модулей (ленивый Minecraft, GameClock)
- [x] Фаза 6: Тесты (EventBus, конфиг, утилиты) — см. `mod/TODO.md`
- [x] Фаза 7: Новые модули по мотивам SoupVisuals — ЗАВЕРШЕНА 2026-09-16 (Cosmetics
      Overhaul 2026-09-11; Gps/HitBubbles/AmbientParticles/EndCrystal/Balancer/Trinket/
      HandsShader — 2026-09-15…16, см. ниже)
- [ ] Фаза 8.5: Аудит структуры ≤200/≤4 — 8.5.1 (папки >4) ЗАВЕРШЕНА 2026-09-10
      (0 папок >4, коммиты 8846d53…303b17e); 8.5.2 (гиганты >600) ЗАВЕРШЕНА
      2026-09-28 (0 файлов >600, крупнейший — Renderer2D 474, фасад-исключение);
      план в `mod/TODO.md`
- [x] Задача «Скачивание» (2026-09-29, ЗАВЕРШЕНА, детали в конце файла) — страница
      `/download` с двумя кнопками: прямая скачка jar с нашего бэкенда + кнопка
      GitHub под лентой «временно не работает»; jar 64,4 МБ живёт на VDS и
      отдаётся gateway (`GET /downloads/*` → `/api/downloads/lovisual.jar`),
      проверено curl'ом и в браузере; кнопки лендинга/футера ведут на страницу
- [ ] Фаза 11: Серверный плагин — сервер может отключать функции мода —
      дизайн + КОД ГОТОВЫ 2026-09-30 (Velocity-плагин `server-plugin/`, клиент
      `features/platform/serverpolicy/`, шлюз в `ModuleLifecycleHelper`,
      коммиты af2b4172, 711fcafc, 33cff90c, fdc239db; обе сборки зелёные:
      плагин 9 тестов, мод 449 тестов, checkFolderLimit OK); осталась ручная
      приёмка на стенде владельца (см. Фазу 11 в конце файла)
- [ ] Фаза 8.5.3: остаток разбивки гигантов мода (файлы >200 → ≤200) — топ-4
      закрыт 2026-09-30 (крупнейшие: ConfigDiffPanelComponent 393→167,
      RotationManager 391→199, ScriptedTriangulatorHudPanel 386→189,
      BetterButtons 381→197; коммиты 7e8b6b64, 3e57542b, 76bd8e95, 71f779c8,
      build+test ✓); остаток — 151 файл >200, не марафон, по мере касания,
      план в `mod/TODO.md`
- [x] Фаза 9: Оптимизация FPS — 9.1 (мёртвые грузы) ЗАВЕРШЕНА 2026-09-11 (137121e…4d60d3d);
      9.2 Optimize ЗАВЕРШЕНА 2026-09-25 (ade82f06…8746074d, 4 тумблера + OptimizeState);
      9.3 A/B-мерка убрана из плана (решение владельца 2026-09-25: нужен запуск
      Minecraft на Windows, задачей не является) — фаза закрыта
- [ ] Фаза 10: Платформа (сайт + бэкенд) — Подсистемы 1/2/3 спроектированы 2026-09-23
      (см. ниже). Реализация: план backend Подсистемы 1 в `backend/PLAN.md`
      (написан 2026-09-23), frontend/Подсистема 2/3 — планы позже.
      Бэкенд 2026-09-25: Подсистема 1 backend ЗАВЕРШЕНА — `accounts-service`,
      `common`, `gateway` (identity, рейт-лимиты, CORS, refresh-ротация,
      device links) и `configs-service` (4 слота, share-коды, витрина
      publish/browse/detail/copy, публичный `/users/{id}` с бейджем `early`);
      планы: `backend/PLAN.md`, `backend/gateway/PLAN.md`,
      `backend/configs-service/PLAN.md`; e2e через gateway проверен вручную.
      Следующий шаг — фронтенд (`frontend/PLAN.md`) и интеграция мода.
      Решения 2026-09-25: мод бесплатный (никаких цен/подписок/ключей на сайте);
      сайт двуязычный ru/en (i18n); кнопка «Скачать» — прямая ссылка на
      `releases/latest/download/lovisual.jar` в GitHub; в план сайта добавлены
      редактор тем в браузере, страница «Скачать» + ченджлог, публичные профили;
      лендинг — насыщенный (живой ClickGui/HUD, частицы), см. `frontend/PLAN.md`.
      Интеграция мода 2026-09-27: первый шаг — URL бэкенда стал runtime-настройкой,
      а не константой сборки. Новый `PlatformConfig` (`mod/src/main/java/dev/loki/lovisual/features/platform/`,
      + `BackendEndpoint` нормализатор) хранит `backendBaseUrl` через тот же
      `ConfigSerializer`, редактируется в ClickGui (категория UTILITY главных
      настроек, рядом со `StyleConfig`). Значение по умолчанию — плейсхолдер
      `https://api.lovisual.example` (реальный прод-домен ещё нигде в репо не
      зафиксирован). HTTP-клиент и сама интеграция (auth/device-link/витрина) —
      отдельная будущая задача.
      Решение 2026-09-29 (см. задачу «Скачивание» ниже): прямая ссылка на GitHub
      из кнопок убрана — jar отдаёт наш бэкенд, GitHub остаётся второй кнопкой
      под лентой «временно не работает».

---

## Фаза 7: Новые модули — референс SoupVisuals
> Референс: `ref/soupvisuals-src/` — SoupVisuals 3.2.0 (MC 1.21.11, лицензия SOUP-1.0).
> Код референса НЕ копируем (чужая лицензия + декомпил) — только пере-реализация идей
> под наши правила (≤200 строк/файл, ≤4 файла/папку, без статики, тесты).
> Модули референса лежат в `ref/soupvisuals-src/padej/soup/implement/features/modules/`.

- [x] **Hitboxes** (visuals) — цветные хитбоксы сущностей, как у Soup: ТОЛЬКО рендер
      контейнера с настройкой цвета/видимости, БЕЗ увеличения хитбокса (увеличение —
      чит, удалён в Фазе 3.5). Референс: `visuals/Hitboxes.java`
- [x] **Watermark** (HUD, draggable) — обещан в README, в коде отсутствует
- [x] **Gps** (world) — ЗАВЕРШЕНО 2026-09-15: компас-стрелка + маркер к точке
      (`visuals/world/Gps.java`, `util/world/GpsBearing.java` чистая математика + тесты,
      команда `%gps <x> <y> <z> | clear`). Референс: `world/Gps.java` — идея взята
      (стрелка+маркер+дистанция+авто-удаление), код не копировался. Xaero-связка НЕ
      сделана (была опциональной идеей, не в скоупе запроса). Коммит fb0b288.
- [x] **Light** (world) — кастомный цвет освещения (у Soup 41 стр.). Референс: `world/Light.java`
- [x] **HitBubbles** (particles) — ЗАВЕРШЕНО 2026-09-15: отдельный модуль
      `visuals/effects/HitBubbles.java` (не режим HitEffect.BUBBLES — независимое включение,
      трёхфазная анимация появление/жизнь/исчезновение через новый `ThreeStageProgress`).
      Заодно вынесены общие хелперы из HitEffect (DRY): `AttackHitPoint`,
      `OrientedQuadRenderer`, `AnimatedColorModeOptions`. Референс: `particles/HitBubbles.java`
      (идея взята, код не копировался; интерполяции/множественные текстуры Soup не портированы).
      Коммит 5f2fc70.
- [x] **AmbientParticles + FireFlyParticle** (particles) — ЗАВЕРШЕНО 2026-09-15: модуль
      `visuals/particles/AmbientParticles.java`, два режима — Ambient (плывущие мошки) и
      Firefly (блуждание + кольцевой буфер шлейфа вместо Trail-объектов референса). 19
      визуальных стилей референса (Stars/Hearts/Bloom/.../Pyramid, asset-тяжёлые) НЕ
      портированы — фича-крип чит-клиента вне направления проекта; цвет через уже
      существующий AnimatedRenderColors вместо своей Sync/Custom-системы. Коммит 28ec166.
- [x] **NameProtect** (misc) — подмена своего ника и ников друзей в тексте чата/скорборда
      (у Soup 42 стр.). Референс: `other/NameProtect.java`
- [x] **EndCrystal** (visuals) — ЗАВЕРШЕНО 2026-09-15: `visuals/scene/EndCrystal.java` +
      `mixins/render/entity/EndCrystalRendererMixin.java` (WrapOperation на ModelPart.render,
      различает 2 вызова glass + 1 cube счётчиком-ordinal — тонировка/скрытие частей) +
      inject в `ClientLevelMixin.removeEntity` (осколки-вспышка на месте разрушения, т.к.
      ваниль убирает сущность мгновенно). Референс: `other/EndCrystal.java` — идея взята
      (кастомный цвет, "доиграть" разрушение), Sync/Custom×4-цвета+Wave/Vertex упрощены до
      общего AnimatedRenderColors. Коммит 50db8e3.
- [x] **Trinket** (не "2.0" — CosmeticDisplay удалён откатом 2026-09-11, апгрейдить нечего) —
      ЗАВЕРШЕНО 2026-09-16: отдельный модуль `visuals/items/Trinket.java` в стиле текущих
      ChinaHat/TazikHat. `util/physics/TrinketPhysics` (чистая, тесты) — шарик на пружине
      у ног игрока (гравитация/отскок/тетер). Рендер — тонированный billboard (наш
      AnimatedRenderColors) + затухающий шлейф (кольцевой буфер) + звук на отскок
      (ванильный SoundEvents.SLIME_JUMP). Библиотека 10 иконок-спутников референса и
      8 кастомных звуковых тем НЕ портированы — нужны новые ассеты. Референс:
      `other/Trinket.java` (там тоже почти нет реализации — только настройки).
      Коммит 840a716.
- [x] **Cosmetics Overhaul (Unified Cosmetics)** — ЗАВЕРШЁН 2026-09-11:
      слоты `CosmeticSlot` (MODEL/PET/HEAD/SHOULDERS/BACK/WAIST/LEGS/HELD), парсинг
      `slot` из avatar.json + эвристика по имени папки, `CosmeticManager` на EnumMap
      (аксессуары — несколько одновременно), `CosmeticsScreen` (3D-превью drag/zoom +
      хотспоты + правая панель live-apply с "None" первым), per-slot offset/rot/scale
      через NumberValue-поля, built-in ChinaHat/TazikHat/BackSword в `cosmetics/builtin/`
      (старые модули chinahat/tazikhat/backsword удалены), i18n en+ru, тесты
      CosmeticSlotTest/CosmeticsHotspotTest.
      - ~~CosmeticDisplay 232 / TazikHatRenderer 242 / BackSwordRenderer 231 >200~~ —
        разбиты: CosmeticDisplay 196 + CosmeticBuiltins (значения+диспетчер built-in),
        TazikHat 186 (палитра-математика переиспользует ChinaHatPalette), BackSword 200
        (edge/glow слиты в параметризованный drawKatana).
      - Не сделано из плана (следующий заход): иконки-превью built-in в списке слота,
        правка слотов SHOULDERS/LEGS/PET из экрана (сейчас только HEAD/BACK built-in),
        предпросмотр до эквипа.
- [x] **Balancer** (mctiers) — ЗАВЕРШЕНО 2026-09-16: `features/module/modules/misc/Balancer.java`
      (fixType Vanilla/UHC/Pot/NethOP/SMP/Sword/Axe/Mace) + `util/mctiers/MCTiersCache`
      (асинхронный кэш, паттерн SkinManager) + `util/mctiers/MCTiersTier` (форматирование,
      тесты) + команда `%tier <ник> [режим]`. Референс `mctiers/Balancer.java` почти пуст —
      логика была в недоступной McTiersModule-базе; реальный публичный API mctiers.com
      найден и проверен через сторонний open-source плагин (sinnayuh/MCTier):
      `GET https://mctiers.com/api/search_profile/<username>`. Коммит cf81ee4.
- [x] **HandsShader** — ЗАВЕРШЕНО 2026-09-16: сверка показала, что Chams уже реализует
      9/10 режимов Soup (Solid/Chroma/Balatro/Smoke/Stripes/Glow/Glass/Snow + свои
      Metallic/Noise) и полный dual/blend-набор (Mix/Add/Multiply/Screen/Overlay/
      Difference, `HandBlendUniforms`) — сверх ожиданий. Не хватало только "Invert":
      добавлен `hand_chams_invert.frag` (RGB-инверт под маской руки через уже
      существующий UBO HandChams/u_Mix.x, без HSV-тумблеров и edge-glow референса —
      это дублировало бы наш Glow) + регистрация пайплайна + вписан в оба mode-списка
      Chams (основной и dual shader1/shader2). Референс: `visuals/HandsShader.java`.
      Коммит 76ffdec. **Это был последний пункт Фазы 7** — SoupVisuals-раздел закрыт
      полностью (Gps/HitBubbles/AmbientParticles/EndCrystal/Balancer/Trinket/HandsShader).
- [x] **Фикс ChinaHat** — текущая геометрия кривая (смахивает на самбреро/зонт);
      переделать модель в нормальную конусную шляпу по референсу Soup
      (`visuals/ChinaHat.java` + `ChinaHatModel.java`).

---

## Фаза 10: Платформа — сайт + бэкенд (дизайн от 2026-09-23)

> Прим.: лаунчер и старый backend (Rust+axum) были удалены ранее (см. шапку файла) —
> это НЕ восстановление старого кода, а новый дизайн с нуля под текущие требования.
> Статус: дизайн готов, backend Подсистемы 1 реализован 2026-09-25 (планы в
> `backend/PLAN.md`, `backend/gateway/PLAN.md`, `backend/configs-service/PLAN.md`);
> дальше — фронтенд (`frontend/PLAN.md`).
> Код — в выделенных `backend/` и `frontend/` (созданы 2026-09-23), отдельно от `mod/`.

### Границы (декомпозиция)
- **Подсистема 1 (этот дизайн)**: аккаунты, облачные конфиги, аватарки, публичная
  витрина конфигов.
- **Подсистема 2 (backlog, отдельный дизайн)**: RPC-чат (переименован из рабочего
  названия "RVS" — отдельная чат-группа для владельцев клиента, не привязана к
  конкретному minecraft-серверу), друзья/статус онлайн, виджеты-телеметрия друга
  (HP/координаты/сервер/в бою или нет поверх HUD). Требует realtime-слой
  (WebSocket/presence) — не влезает в REST-модель Подсистемы 1.

### Архитектура
Микросервисы с самого старта (осознанный выбор владельца проекта, не
default-рекомендация): `accounts-service`, `configs-service`, `chat-service`
(зарезервирован под Фазу 11), каждый — отдельный Rust/Axum процесс, общение
между сервисами по gRPC (tonic). Перед ними — отдельный **API Gateway**
(тоже Axum): единственная публичная точка (`api.<domain>`), терминирует TLS,
проверяет JWT один раз, роутит по gRPC во внутренние сервисы; сайт и мод не
знают о микросервисах и стучатся только в gateway. Для будущего chat-service
gateway дополнительно проксирует WebSocket (`wss://api.<domain>/chat/ws`).

Один физический Postgres-инстанс/порт, у каждого сервиса — своя отдельная БД
внутри этого инстанса (`accounts_db`, `configs_db`, `chat_db`) — логическая
изоляция без отдельных серверов на старте. Аватарки и файлы конфигов — в
S3-совместимом хранилище (MinIO), не в Postgres.

Фронтенд сайта: React + Vite + Tailwind CSS. Дизайн/палитра — плагин
`design-assets` (jezweb/claude-skills маркетплейс, skills `color-palette`,
`favicon-gen`, `icon-set-generator`, `image-processing`, `ai-image-generator`)
+ встроенный skill `frontend-design`.

### Модель данных

**accounts_db**
- `accounts`: id, email (unique), password_hash (argon2id), display_nick
  (произвольный, НЕ привязан к minecraft-нику), created_at
- `avatars`: account_id, s3_key, uploaded_at
- `device_links`: id, account_id, device_token_hash, linked_at, last_seen —
  привязанные устройства (моды), видны/отзываемы в личном кабинете

**configs_db**
- `config_slots`: id, account_id, slot_index (1..4 — фиксированный лимит на
  аккаунт), name, data (jsonb), updated_at
- `share_codes`: config_slot_id, code (unique, криптографически случайный,
  постоянный, владелец может регенерировать — старый код инвалидируется)
- `showcase_listings`: config_slot_id, title, description, published_at —
  публичная витрина (фича из brainstorm-опроса)

Свой конфиг грузится по имени/индексу напрямую. Чужой — только по коду через
`%config load <код>` в игре (см. API ниже); одноразовость кода осознанно
отклонена — код обязан быть постоянным (ссылка-приглашение), пока владелец
сам не перевыпустит.

### Auth — два независимых пути

**Сайт**: обычный email + пароль.
- `POST /auth/register` {email, password, nick}
- `POST /auth/login` → access JWT (~15 мин) + refresh-токен (httpOnly cookie)
- `POST /auth/refresh`, `POST /auth/logout`

**Мод → аккаунт**: OAuth 2.0 Device Authorization Grant (как Twitch/PlayStation),
пароль в мод никогда не вводится.
1. Мод: `POST /device/code` → `{device_code, user_code, expires_in}`
   (`user_code` короткий, напр. `ABCD-1234`)
2. Мод показывает `user_code` в настройках, игрок вводит его на
   `site.<domain>/link` (уже залогинен на сайте) → `POST /device/confirm`
3. Мод параллельно поллит `POST /device/token` {device_code} каждые 2-3с
4. После подтверждения — долгоживущий `device_token` (хранится у мода
   локально), используется как Bearer-заголовок ко всем запросам gateway
5. Личный кабинет → список привязанных устройств с `last_seen` и кнопкой
   revoke (единичный отзыв, не ломает остальные устройства)

### API конфигов и аватарок
- `GET /configs` → 4 слота аккаунта (name, updated_at, share_code)
- `PUT /configs/{slot}` {name, data} → сохранить, last-write-wins
- `GET /configs/{slot}` → своё содержимое
- `POST /configs/{slot}/regenerate-code`
- `GET /configs/shared/{code}` → чужой конфиг по коду (код = секрет, отдельной
  авторизации получателя не требует) — этот путь дёргает `%config load <код>`
- `POST|DELETE /configs/{slot}/publish` → витрина
- `GET /showcase?sort=new|popular&page=`, `GET /showcase/{listing_id}` —
  публично, без авторизации; "скопировать к себе" → копия в свободный слот
- `POST /avatars` (multipart, ≤5MB, png/jpg/webp, валидация по magic bytes,
  не по расширению) → ресайз до 256×256, S3 + CDN-URL в `accounts.avatar_url`

### Безопасность
- Rate-limit на `/auth/login` и `/device/*` (брутфорс / спам кодов)
- `password_hash` — argon2id
- `device_token` хранится хэшированным в БД, как пароль
- Share-код — криптографически случайный, не инкрементный ID (не перебираем)

## Пасхалки (решение владельца 2026-09-25)

1. **`.env`-ловушка** (gateway): запросы на `.env` (включая через `../`) → 200 + фейковый
   шуточный `.env` («nice_try_skiddie…»), в апстрим не уходят, IP в лог. Делается вместе
   с фиксом обхода лимитов через dot-segments.
2. **Konami code на сайте** (↑↑↓↓←→←→BA): ClickGui на главной включает TrollfaceMask,
   всё уходит в радужную тему.
3. **Сообщение в консоли DevTools**: ASCII-логотип LoVisual + «Шаришь? Исходники
   аддонов — на витрине».
4. **`IDDQD`**: `%config load IDDQD` в моде и `/configs/shared/IDDQD` на сайте отдают
   шуточный конфиг «God mode» — всё выключено, кроме ChinaHat. Код не пересекается с
   настоящими (в алфавите share-кодов нет `I`, длина 5 ≠ 8).
5. **404 = пустой мир**: падает блок, по клику «ломается» с частицами, как в игре.
6. **`/coffee` → 418 I'm a teapot** (gateway) со ссылкой на мод.

Планы: `frontend/PLAN.md` Task 11, `backend/configs-service/PLAN.md` Task 7,
`backend/gateway/PLAN.md` Task 12; мод-часть `%config load IDDQD` — в плане интеграции мода.

---

## Подсистема 2: RPC-чат + друзья + виджеты-телеметрия (дизайн от 2026-09-23)

> Статус: спроектировано, реализация не начата. Требует realtime-слой
> (WebSocket + presence), поэтому — отдельный `chat-service` (см. Фазу 10,
> раздел "Архитектура"), а не REST как у Подсистем 1/3.
>
> Важно не путать с уже существующей в моде системой "друзей"
> (`NameProtect` и т.д., `features/module/modules/misc/protect/NameProtect.java`)
> — это **локальный** список ников на клиенте для защиты от пинга/подсветки,
> не связан с аккаунтами. Ниже — отдельная, новая система друзей на аккаунтах.

### Структура чата
Один глобальный канал ("все онлайн-владельцы клиента") + личные сообщения
1-на-1 только между взаимными друзьями. Без тематических каналов/групп —
не усложняем на старте.

- Только live-доставка, **без хранения истории на сервере** (как IRC) —
  ушёл из чата/оффлайн — старое не восстановить. Упрощает `chat_db` до
  почти пустой схемы (не нужна retention-политика, GDPR-очистка и т.п.)
- `chat_db` хранит только: список текущих online-подключений (для presence)
  и friend-графа (см. ниже) — сами сообщения в БД не пишутся, гоняются
  напрямую через WebSocket-хаб в памяти chat-service

### Друзья (новая, account-based система)
- Добавление — запрос/принятие по нику аккаунта (не по minecraft-нику):
  поиск на сайте или в клиенте → `POST /friends/request` {target_nick} →
  получатель видит входящий запрос (на сайте и/или в клиенте) →
  `POST /friends/accept|decline`
- `chat_db.friendships`: account_id_a, account_id_b, status (pending/accepted),
  created_at (симметричная связь после accepted)
- Личные сообщения в чате доступны только между accepted-друзьями

### Виджеты-телеметрия друга (HUD)
- Данные: HP, координаты, minecraft-сервер (адрес/имя), в бою (да/нет) —
  каждое поле передаётся отдельно, не пакетом "всё или ничего"
- **Настраиваемый шаринг по полям** (выбор пользователя, приватность):
  `chat_db.telemetry_sharing_prefs`: account_id, share_hp (bool),
  share_coords (bool), share_server (bool), share_combat_state (bool) —
  дефолт консервативный (напр. только HP+combat включены, координаты/сервер
  выключены by default, т.к. чувствительны)
- Транспорт: тот же WebSocket-канал chat-service, отдельный тип сообщения
  `telemetry_update` (broadcast только подписанным взаимным друзьям, с
  фильтром по `sharing_prefs` получателя-источника на сервере — клиент
  никогда не получает поле, которое источник не разрешил шарить)
- Отображение — новые draggable HUD-элементы по образцу существующих
  (`registerDraggableHudElement`), один виджет на друга или агрегированный
  список — решить на этапе UI-дизайна перед реализацией

### Presence (онлайн-статус)
- Мод коннектится к `wss://api.<domain>/chat/ws` с `device_token` при старте
  клиента (не требует отдельного логина — токен уже есть после device-link
  из Подсистемы 1)
- `online` = есть открытый WebSocket; `last_seen` обновляется на disconnect
- Список друзей на сайте/в клиенте показывает online/offline живым статусом
  через тот же сокет (site тоже может подключиться, если залогинен — видеть
  друзей онлайн из браузера)

### Профили/статусы (лёгкая версия, backlog внутри backlog)
Идея из brainstorm-опроса ("уровень/бейджи/тайтлы", "давно с нами", "шарил N
конфигов") — не проектируется в деталях сейчас, т.к. это чисто косметическая
надстройка без технических рисков — легко добавить в `accounts_db` (поля
`created_at` уже есть, `badges` — просто jsonb-массив) на любом этапе позже,
не блокирует остальную Подсистему 2.

### Рейт-лимиты Подсистемы 2 (дополнение к общей таблице в Фазе 10)
| Действие | Лимит |
|---|---|
| Отправка сообщения (глобальный канал) | 1 / сек / аккаунт, burst 5 |
| Отправка личного сообщения | 2 / сек / аккаунт |
| `POST /friends/request` | 20 / час / аккаунт (защита от спама заявками) |
| `telemetry_update` (от клиента к серверу) | не чаще 1 раз / 2 сек / аккаунт — сервер троттлит и досылает клиентам с той же частотой, не завязан на тик-рейт игры |

---

## Подсистема 3: Аддоны 2.0 + витрина аддонов (дизайн от 2026-09-23)

> Статус: спроектировано, реализация не начата. Расширяет существующий Java
> addon-API (`docs/api/addons.md`, `addon/core/AddonManager.java`) — тот API
> и дальше можно ломать (решение владельца 2026-09-05, см. верх файла), новый
> скриптовый слой — надстройка сбоку, не замена.

### 1. Скриптовые аддоны (новый тип, для витрины)
- JavaScript через GraalVM, отдельный от Java-`AddonManager` загрузчик рядом
  с `addon/core`
- Точка входа — `manifest.json` (не `fabric.mod.json`: скрипт-аддону не нужен
  Fabric-loader): `id, name, version (semver), author, tags, entry.js`
- Ограниченный whitelisted API — обёртки над тем же, что доступно
  Java-аддонам сейчас: `registerModule` (JS-класс с полями-настройками),
  `registerCommand`, `addRenderCallback` (только 2D HUD-отрисовка). Никакого
  прямого доступа к рефлексии/файловой системе/сети — весь I/O только через
  явно предоставленные хелперы контекста
- Java-аддоны (текущие Fabric-моды) остаются для продвинутых случаев
  (мировой рендер, миксины), устанавливаются вручную; в витрине идут отдельным
  помеченным разделом от скриптовых

### 2. Больше точек расширения в существующем Java API
Добавить в `LoVisualAddonContext`:
- Доступ к событиям (input, world tick, packet)
- Кастомные конфиг-виджеты в ClickGui
- Регистрация собственных рендер-пайплайнов (не только callback)
- API для чтения/записи через облачную конфиг-систему Подсистемы 1

### 3. Метаданные / версии / автообновления
- `manifest.json` получает `version` (semver) — и у скриптовых, и у Java-аддонов
  (в `custom.lovisual:addon` добавляется поле)
- `AddonManager` при старте сверяет установленную версию с версией на сайте
  (если аддон пришёл оттуда — манифест хранит `source_url`/`addon_id`) →
  бейдж "обновление доступно" в `LoVisualAddonManagerScreen`; апдейт — по
  клику пользователя, никогда не втихую

### 4. Витрина аддонов на сайте
- Публикация обязательно требует исходники (архив или ссылка на репозиторий)
  — открытость как единственный механизм доверия. Читерские аддоны **не
  блокируются** (решение владельца) — пользователь сам выбирает, что качать
- Теги: обязательный `type: cheat | legit`; необязательные категории
  (мультивыбор, для фильтров, не для модерации): `visuals | hud | new-module
  | utility | qol | other`
- Данные: `addons_db` (отдельная БД в том же Postgres-инстансе, как
  `accounts_db`/`configs_db`) — таблицы `addons` (id, owner_account_id,
  latest_version, tags[], type, source_url, downloads_count) и
  `addon_versions` (addon_id, version, changelog, file_s3_key, published_at)
- Скачивание — прямая ссылка на файл/архив в S3; установка не автоматическая
  и не тихая — юзер жмёт "добавить" в клиенте, клиент качает и кладёт в
  addons-папку, но не запускает без явного включения в `LoVisualAddonManagerScreen`

### 5. Админка на сайте (`/admin`)
- Новая роль `accounts.role: admin` (по умолчанию `user`); доступ к
  admin-эндпоинтам проверяется на gateway, не на фронте — обычные
  пользователи даже не видят `/admin` в UI
- Модерация аддонов: снять с публикации / пометить проблемным (не за
  cheat/legit — только за реальные нарушения: сигналы о malware от юзеров,
  нерабочие/поддельные ссылки на исходники, спам) / **удалить** аддон целиком
  (со всеми версиями, если, например, DMCA или явный malware)
- `accounts.can_publish_addons: bool` (default `true`) — отдельный флаг,
  отзываемый админом независимо от общего бана аккаунта: юзер продолжает
  пользоваться сайтом/конфигами/чатом, но не может публиковать новые аддоны
  (существующие остаются видны, если явно не удалены отдельно)
- Модерация витрины конфигов (Подсистема 1): аналогично, скрыть листинг
- Управление аккаунтами: бан/разбан (полный), отзыв `can_publish_addons`,
  просмотр жалоб
- Статистика: кол-во аккаунтов/аддонов/конфигов, загрузок за период; график
  регистраций по дням/неделям/месяцам; активные аккаунты DAU/MAU (по
  `device_links.last_seen` + активности на сайте)

### 6. Рейт-лимиты (сквозной механизм, все Подсистемы)
Реализуются на **API Gateway** (единая точка — не дублируются в каждом
сервисе), token bucket на Redis (или in-memory при однонодовом деплое на
старте), ключ — `account_id` для авторизованных запросов, `IP` для
неавторизованных (регистрация, логин).

| Эндпоинт(ы) | Лимит | Почему |
|---|---|---|
| `POST /auth/login` | 5 / мин / IP | брутфорс пароля |
| `POST /auth/register` | 3 / час / IP | спам-аккаунты |
| `POST /device/code`, `/device/token` | 10 / мин / IP | спам device-кодов |
| `PUT /configs/{slot}` | 20 / мин / аккаунт | защита БД от спама сохранений (мод может дёргать часто) |
| `GET /configs/shared/{code}` | 30 / мин / IP | перебор share-кодов |
| `POST /avatars` | 5 / час / аккаунт | не файлопомойка |
| `POST /addons` (публикация версии) | 10 / день / аккаунт | не спамить витрину |
| `GET /showcase`, `GET /addons` (листинги) | 60 / мин / IP | защита от скрейпинга/DoS чтения |
| Chat WebSocket: отправка сообщения | 1 / сек / аккаунт (burst 5) | защита от флуда в RPC-чате |

Превышение → `429 Too Many Requests` с `Retry-After`; для WebSocket —
локальный drop сообщения с уведомлением клиенту, без разрыва соединения.

---

## Задача «Скачивание»: две кнопки + jar с нашего бэкенда (2026-09-29, ЗАВЕРШЕНА)

Проблема: кнопка «Скачать» вела на `/downloads/lovisual.jar`, но на проде такого
файла нет — nginx по `try_files` отдаёт `index.html`, то есть вместо мода
скачивалась HTML-страница (проверено 2026-09-29: `content-length: 2458`).

Решение владельца 2026-09-29:

1. Все кнопки «Скачать» (hero лендинга, FaqDownload, футер) ведут на страницу
   `/download`, а не на файл напрямую.
2. На `/download` две кнопки:
   - **«Скачать напрямую»** — рабочая, тянет jar с нашего бэкенда
     (`/api/downloads/lovisual.jar` → nginx `/api/` → gateway → файл на диске);
   - **«Скачать с GitHub»** — закрыта лентой «временно не работает» (релизы в
     GitHub сейчас не публикуются), пока её не уберут вручную.
3. Gateway: новый маршрут `GET /downloads/*` — `tower-http` `ServeDir` на
   каталог из env `DOWNLOADS_DIR` (в docker: `/srv/downloads`, том
   `./downloads:ro`), отдаёт потоком (60 МБ не буферизуются в памяти),
   `Content-Type` гаджетом по расширению (`application/java-archive`),
   `Accept-Ranges: bytes`, 404 на отсутствующий файл, каталог не отдаётся.
   Обход `..` отсекает существующий guard (`reject_ambiguous_paths`, 400);
   nginx не менялся — фронт ходит через существующий `location /api/`
   (срезает префикс → `GET /downloads/*` на gateway).
4. Артефакт: `./gradlew build` в `mod/`, jar 64 415 945 байт (61,4 МБ),
   sha256 `359166d2…4156f9`, кладётся на VDS в
   `/opt/lovisual/backend/downloads/lovisual.jar`, `docker compose build gateway`
   + `up -d gateway`, проверка curl'ом.
5. Релизный процесс: пересобрать мод → положить jar на VDS вручную
   (мод остаётся отдельным Gradle-проектом, в `deploy.sh` не встраивается).

Проверка (2026-09-29, прод): `curl -I https://visual.loki-code.dev/api/downloads/lovisual.jar`
→ 200, `content-type: application/java-archive`, `content-length: 64415945`,
байты `PK\x03\x04`, Range-запрос работает, неизвестное имя → 404; страница
`/download` (снимок в браузере) показывает обе кнопки, GitHub — под лентой;
`bun run test` 114/114, `cargo test --workspace` зелёный, clippy/oxlint чистые.

**Готово (коммиты):** `54af7158` gateway-маршрут + тесты `tests/downloads.rs`,
`fc440f0c` страница/лендинг/футер/i18n/тесты, `70e33f29` лента не перекрывает
надпись кнопки, `eb6e38e2` `.dockerignore` (контекст сборки без `target/` и
jar), `192ef52e` этот план. Деплой на VDS — rsync изменённых файлов (рабочее
деревие `/opt/lovisual` не синхронизировано с origin и содержимо одних и тех же
коммитов — уникальных правок на сервере нет), `.env` на сервере не трогался.

---

## Фаза 11: Серверный плагин — сервер может отключать функции мода (дизайн 2026-09-30)

> Идея владельца 2026-09-29. Решения владельца 2026-09-30: **Velocity (прокси)** —
> целевой софт v1; **политика — локальный YAML плагина** (наш бэкенд не участвует);
> **дизайн → сразу код**. Статус 2026-09-30: код готов, обе сборки зелёные
> (коммиты af2b4172, 711fcafc, 33cff90c, fdc239db); приёмка — на стенде владельца.

**Что это.** Отдельный плагин для Velocity-прокси, который публикует подключившимся
игрокам «политику»: какие возможности LoVisual на этом прокси запрещены. Мод применяет
её автоматически — честный игрок не может включить запрещённое, пока находится за этим
прокси. Работает без интернета и без нашего бэкенда.

### Канал и формат (v1)

Канал: plugin message `lovisual:policy` (`MinecraftChannelIdentifier`), payload — UTF-8 JSON.

- **S→C (push)** — прокси шлёт при `PostLoginEvent` и при `ServerConnectedEvent`
  (смена backend-сервера), плюс в ответ на запрос клиента:
```json
{ "type": "policy", "deny": ["esp", "freecam", "hitboxes"],
  "mode": "lock", "reason": "правила сервера", "server": "lobby-1" }
```
- **C→S (request)** — клиент просит политику (шлёт при входе в play-состояние и после
  каждой смены сервера; это страховка, если push не дошёл из-за регистрации канала):
```json
{ "type": "request" }
```
- `mode: lock` — запрещённые модули выключаются и не включаются, ClickGui показывает
  замок с подписью «отключено сервером» (reason из политики);
- `mode: suggest` — при попытке включить: предупреждение в чат, игрок сам решает;
- поле `server` — имя backend-сервера (для логов и будущей per-server политики;
  v1 политика одна на весь прокси).

### Velocity-плагин (`server-plugin/`)

Отдельный Gradle-проект в корне репо (серверная Java, не Fabric-клиент; в `mod/`-сборку
не входит). Velocity API 4.2.0 (release, papermc-repo) требует JVM 25 — toolchain 25 с
foojay-resolver (автозагрузка тулчейна, если локально только JDK 21); gson +
snakeyaml shaded в jar (guice НЕ шадится — его даёт сам Velocity); JUnit 5.

- `PolicyConfig` — YAML из `plugins/lovisual-policy/config.yml`:
```yaml
enabled: true
mode: lock              # lock | suggest
reason: "Правила сервера"
deny: [esp, freecam, hitboxes, fullbright]
```
  при первом запуске пишется дефолтный; `/lvpolicy reload` — перечитать,
  `/lvpolicy status` — режим/счётчик запрещённых/причину.
- `PolicyMessage` — кодирование policy-JSON (`encodePolicy`) и разбор `request`
  (`isRequest`, gson); `PolicyMode` — `parse`, нет mode → `LOCK`, неизвестное
  значение → исключение (админ видит ошибку).
- `LoVisualPolicyPlugin` — тонкий вiring: `ProxyInitializeEvent` (регистрация канала),
  `PostLoginEvent`/`ServerConnectedEvent` (push), `PluginMessageEvent` (ответ на
  request), команда `/lvpolicy`. Вся логика — в `PolicyConfig`/`PolicyMessage`
  (тестируются без Velocity-сервера).
- Если конфиг `enabled: false` — плагин молчит (канал зарегистрирован, сообщений нет).

### Клиент в моде (`features/platform/serverpolicy/`)

- `ServerPolicy` — record: `mode` (`LOCK`/`SUGGEST`), `deny` (список), `reason`,
  `server`; null-mode → `LOCK`;
- `ServerPolicyCodec` — Gson **parse** + константа `REQUEST_JSON` (encode не нужен:
  клиент только принимает; тот же gson, что у `PlatformHttpClient`); мусорный JSON
  → `null`, неизвестный mode → `LOCK` (ничего не разблокируется);
- `ServerPolicyState` — синглтон-состояние: текущая политика, `allows(moduleId)`,
  `isLocked()`, `clear()` при дисконнекте; при получении — принудительное выключение
  запрещённых включённых модулей (source INTERNAL) + одно chat-уведомление;
- `ServerPolicyClient` (вложенный `Payload` record, `Identifier.fromNamespaceAndPath`)
  + регистрация через Fabric networking (`PayloadTypeRegistry.serverboundPlay()/
  clientboundPlay()`) — S2C receiver применяет политику в State (включая
  force-disable уже включённых запрещённых + одно chat-уведомление),
  `ClientPlayConnectionEvents.JOIN` шлёт C2S `request`, `DISCONNECT` → `clear()`;
  `init()` вызывается из `LoVisual.onInitializeClient()`;
- применимый шлюз один: `ModuleLifecycleHelper.setEnabled` — при `state==true` и
  `!ServerPolicyState.allows(id)` → отказ (+ chat-сообщение в `lock`, просто
  предупреждение в `suggest`); то же правило в `loadAndApply` (загрузка конфига не
  включает запрещённое, значение enabledValue не затираем) и в `setTransient`
  (addon-код не включает запрещённое в обход шлюза);
- ClickGui: на строке запрещённого модуля — замок и подпись reason (по образцу
  setting-level unavailable);
- `%policy` — команда (`features/command/impl/platform/PolicyCommand`): текущий режим,
  список запрещённых, backend-сервер, причина (или «политики нет»). i18n:
  `command.policy.description`, `lovisual.policy.{applied,denied,locked,suggest}`
  в `en_us.json`/`ru_ru.json`.

### Честные ограничения (записаны заранее)

Применение политики — на клиенте: читер может её игнорировать. Это не античит —
цель: правила сервера и удобство честных игроков. Настоящая защита — серверный
античит, он отдельно. Подсистема 2 не конфликтует: там realtime-слой (RPC/друзья),
здесь — один payload при входе, каналы разные.

### Тесты и приёмка

- плагин: `PolicyConfigTest` (парс YAML: режимы, дефолты, пустой файл, кривой
  mode → исключение, запись дефолта при первом запуске), `PolicyMessageTest`
  (encode policy / экранирование reason / `isRequest` против мусора) — 9 тестов;
- мод: `ServerPolicyCodecTest` (полный парс, неизвестный mode → LOCK, отсутствующие
  поля, мусор/чужой type → null, корректность `REQUEST_JSON`),
  `ServerPolicyStateTest` (изначально открыто, lock/suggest, замена, `clear`);
  force-disable в `ServerPolicyClient` unit-тестами не покрыт (там MC/fabric) —
  ручная проверка на стенде;
- `./gradlew build` в обоих проектах: плагин 9 тестов OK, мод 449 тестов OK,
  `checkFolderLimit` OK; ручная приёмка на стенде владельца:
  поднять Velocity с плагином → зайти клиентом → `%policy` показывает политику →
  запрещённый модуль не включается (lock).

### Риски / известное

- версия протокола клиента (MC 26.2) должна поддерживаться версией прокси —
  проверяется на стенде владельца (плагин от версии протокола не зависит);
- push S→C может не дойти, пока клиент не зарегистрировал канал — поэтому есть
  C→S `request` и повторный push при `ServerConnectedEvent`;
- push при `PostLoginEvent` может прийти в configuration-фазе (у клиента
  зарегистрирован только play-канал) — потеря не критична: фактическую политику
  несёт push на `ServerConnectedEvent` и ответ на request;
- пер-server политика (у каждого backend свой набор) — backlog: формат поля `server`
  для неё зарезервирован.
