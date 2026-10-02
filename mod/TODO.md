# LoVisual Mod (26.2): план и статус

Мод вырос из форка Combatant Client. Полный старый текст (фазы 1-12, все решения, коммиты, списки файлов) сохранён
в `docs/TODO_HISTORY.md` (770 строк, не правим); здесь только актуальное.

## Правила (обязательные)
- KISS / DRY / SOLID, без оверинжиниринга; публичный API менять можно, мёртвый код удалять.
- ≤250 строк на файл (новый код держим ≤200), ≤4 .java файла на папку (`checkFolderLimit` падает при нарушении).
- Коммиты на английском; владелец просит большие коммиты, не дробить; ничего не пушить без просьбы.
- Без `static Minecraft` в полях, без this-escape, без мёртвых миксинов и файлов-пустышек.
- Каждый этап компилируется (`./gradlew build`), у нового кода тесты (JUnit) на чистую логику.
- Тексты сайта и ответы без длинного тире; ответы владельцу по-русски.

## Сделано (кратко)
- Ребрендинг, удаление Runtime/JarReplacement, разбивка файлов-гигантов (>600 строк), удаление читерских модулей, тесты.
- Перенос QoL-модулей из Delta (StreamerMode, LockSlot, AutoRespawn/Leave/Accept, ThirdPerson, UseTracker, NoServerPack,
  DeathCoords, ItemScroller, TapeMouse, SoundReducer и др.), команды с префиксом `%` и автокомплит (в том числе в ванильном чате).
- Оптимизация FPS (мёртвые грузы, модуль Optimize), аудит attack-визуалов по sacura/GodWeer.
- Сессия 2026-10-01: TargetESP (Созвездие, Мечи), JumpCircles, KillEffect Blood (с depth), Hero/orbs главное меню и его
  настройки, Simple Voice Chat (мягкая интеграция, модуль, HUD), тултипы (amber по умолчанию, выключаются, авто-масштаб под GUI
  Scale), описания модулей и плашка описания в ClickGUI, `%tier` с подсказками ников, краши запуска (MixinExtras 0.5.5, helper-классы
  вне `mixins.*`, порядок static init тем).

## Фаза 13: порт идей из evo (отчёт: `docs/EVO_REPORT.md`)
Лицензия: код и ассеты evo проприетарные (`ref/evo/README.md`). Для релиза только свои модели/звуки или разрешение автора;
пути из отчёта нужны для локальной отладки, ассеты evo в репозиторий не коммитить.

- [x] HoldMyItems Scripted на LuaJ: режим Dynamics/Scripts/Both; `hand_pose`, `hand_relative_pose`, `item_pose`, `item_model` +
      аддоны (из `config/lovisual/holdmyitems/`, горячая перезагрузка); песочница без io/os/luajava; `V:cancelEquip/cancelSwing`,
      `animator` (квады модели), `particleManager`, `C:setCamRot/setCamPos`, «всегда обе руки». Код: `held/lua/{runtime,api,ext}`.
      Не сделано: события `swingMHand/interact/blockBreaking`, `use_duration`/`using_item`, ItemStack-поля, roll камеры, ресурс-паки.
- [x] TNTTimer: уже был в `Predictions` (плашка с таймером); добавлены «Иконка TNT» и «Полоска фитиля».
- [x] ProjectileTrails: новый модуль `visuals/effects/projectiletrails/` (`TrailStore`, `TrailRibbon`): ленты за стрелами, трезубцами,
      жемчугом, снежками, яйцами, зельями, огненными шарами, ветряными зарядами, фейерверками; длительность, ширина, заливка
      Solid/Glow, контур, глубина, 8 режимов цвета. Шейдерные заливки evo (Nebula, Plasma, Matrix и т.д.) не портированы.
- [x] DeathEffects: режимы KillEffect Shatter, Ascend, Explosion, Disintegrate (`hits/death/`: `DeathPhysics` чистая + 6 тестов,
      `DeathBody`, `DeathMesh`, `DeathEffectHelper`; тело из 6 частей с UV скина игрока, у мобов серые кубы). Настройки в
      `DeathEffectSettings`. Не сделано: скрытие ванильного трупа на время эффекта, шейдер `death_blast`, режим Lightning из evo
      (у нас уже есть свой Lightning).
- [x] GuiPresence (идея evo, наш бэкенд): `backend/chat-service` (новый крейт, in-memory, `POST /presence/gui`, привязка mc uuid к аккаунту,
      TTL 6 с, план и протокол в `backend/chat-service/PLAN.md`), маршрут `/presence/*` в gateway (+ `CHAT_HTTP_URL`, лимит 6/с на аккаунт),
      Dockerfile/compose/dev-stack. Мод: `platform/presence/{PresenceCodec,PresenceTimeline,PresenceClient}` (кодек 36 байт, плейбэк с
      задержкой 300 мс, адаптивный опрос 350 мс/2 с), модуль `visuals/presence/GuiPresence` (делиться своим меню, показывать чужие,
      дистанция, размер), снимок меню `ModulesMenuScreen.presenceSnapshot`. Мини-копия ClickGUI в мире перед игроком из цветных квадов
      (без текста). Нужны привязанный аккаунт (`%link`) и мультиплеер. Не сделано: значок «LoVisual» у ника/в табе, названия модулей в панели,
      просмотр видео (`.watch` у evo), деплой chat-service на прод СДЕЛАН 2026-10-02 (rsync backend + `docker compose up -d --build chat-service gateway`; на сервер не уехали накопленные правки accounts-service/configs-service, они ждут отдельного деплоя).
- [x] ElytraTrails и Blizzard сверены с оригиналом evo (`ref/evo/.../ElytraTrailsModule`, `BlizzardModule`), спец-тесты по поведению evo:
      ElytraTrails: концы крыльев по настоящей цепочке матриц модели элитры (`pose/WingTipLocator`, `ElytraPoses`), горизонтальная
      лента, сужение Grow/Shrink, колебания Wave/Helix, плавный старт, реакция на скорость, мягкие края, ореол, свой/чужие, разрыв ленты
      при скачке больше 8 блоков или паузе больше 300 мс, шаг записи 0.35, до 256 точек (`ribbon/RibbonShape`, `TrackBuffer.addContinuous`).
      Не сделано из evo: заливка шейдером (8 видов), раздельные цвета крыльев.
      Blizzard: снег только над землёй и под открытым небом (карта высот, спавн между землёй и потолком, нет снега в помещении),
      константы ветра/плотности/турбулентности как у evo, экспозиция неба, «Whiteout» (миксин тумана `BlizzardFogMixin`), скрытие ванильной
      погоды (`WeatherHideMixin`), рендер «капля» вдоль скорости, обмерзание 32 полосы, «только в снегопад» по силе осадков.
      Не сделано из evo: «Snow cover» (снег накапливается на земле, шейдер). Не проверено вживую: вид, туман, положение концов крыльев.
- [ ] Wings: режим «Модель» (Bedrock geo.json + animation.json, 14 видов; отчёт раздел 2.4). Шаги: `BedrockGeoParser` (общий с ножами),
      `BedrockAnimation` + процедурные angel/demon/dragon, `WingsRenderer.renderModel`, настройки `wingSource`/`wingModel`, тесты.
      Ресурсы (локально): `ref/evo/mod/jar/assets/evolution/cosmetic/wings/`, `.../textures/cosmetic/wings/`.
- [ ] Knife (Melee Skins): 33 скина, клипы draw/inspect/melee_1..3/run, first/third person, звуки, шейдер holy (отчёт раздел 3.7,
      порядок работ из 8 шагов). Ресурсы: `ref/evo/mod/jar/assets/evolution/cosmetic/melee/`.
- [x] Быстрые модули P1 (2026-10-02, сверены с `ref/evo/mod/jar-src-named/evovis/module/{misc,render}`, тесты зелёные, вживую не смотрели):
      AutoSprint (`player/`, как у evo держит спринт; вход через `SprintControlEvent`, решает `SprintController.canStartSprinting`),
      TotemCounter (`visuals/totems/`, свои тотемы справа от хотбара; счёт попов чужих у нас уже в `util/pvp/opponents/TotemPopSnapshot`,
      без отдельной клавиши сброса как у evo), EnchantGlint (`visuals/items/glint/`: стили Rainbow/Solid/Gradient/Hidden, сила, скорость,
      масштаб, «предметы/броня»; у evo свои RenderType и 11 стилей, у нас перерисовка ванильных текстур блеска `GlintTexture` +
      `GlintTextureMixin`; нет Pulse/Fire/Galaxy/Hologram/Glitch/Sparkle/Aurora, Coverage), ArmorColor (`visuals/items/armor/`: цвет по
      прочности Gradient/Steps, цели Players/Friends/Self/Mobs, Only Below, Low Pulse; миксин в `EquipmentLayerRendererMixin`, флаг цели
      в `LivingEntityRenderState`), PlaceAnimation (`visuals/placeanimation/`: блок вылетает из руки, наклон/масштаб, как у evo: скрытие из
      меша `SectionCompilerPlaceMixin` + Sodium `SodiumPlace*Mixin`, отрисовка движущимся блоком `LevelRendererPlaceMixin`, блок-сущности
      `BlockEntityPlaceMixin`; формулы кривых и сторона руки восстановлены по идее evo, направление наклона и сдвига по вертикали подобраны
      на глаз).
      Проверить в игре: миксины брони/блеска/PlaceAnimation (при ошибке прислать лог), вид наклона и сдвига блока, перекраску блеска и её
      возврат при выключении, Sodium (скрытый блок не оставляет дыр у соседей), TotemCounter при GUI Scale 2/3/4.
- [x] KineticLyrics починен (2026-10-02): `LyricPlayback.loadTrack` вызывал `reset()` после записи ключа трека, ключ стирался, загруженный
      текст отбрасывался, а запрос уходил заново каждый тик (lrclib отвечал 503). Теперь `lrc/source/LyricsFinder`: lrclib (точный запрос с
      длительностью, ранжированные поиски `lrc/match/LyricMatch`, обычный текст получает оценочные тайминги `PlainLyricTimer`), запасной
      NetEase, повтор при занятом сервисе (до 6 раз). Нет из evo: YouTube Music и распознавание Whisper. Проверить вживую на Spotify.
- [x] CustomModels (2026-10-02, из `CustomModels-2026-10-02-120830.zip`): `visuals/models/` + `mesh/` (Chicken, Rabbit, Verity из .bbmodel),
      ассеты в `assets/lovisual/models/`, миксины `render/models/`. Только у вас на экране, цели: себя/друзья/все. Нужна проверка вживую
      (поза рук, рука от первого лица, Verity под Iris/Sodium). Исходный проект зарезан до этого модуля, остальные его части не нужны.
- [ ] GodRays (референс `shaders/core/godrays`).
- [ ] Дальше P1-P2 — перенос модулей из evo поштучно (референс: `docs/EVO_REPORT.md` §1.2;
      лицензия evo проприетарная — их ассеты/код в репо не кладём, только идеи):
  - [ ] **Blizzard** (render, evo ~1290 LOC, M/L) — высотная метель: ветровые снежинки с горизонтальным
        сносом, белая пелена, обмерзание краёв экрана.
  - [ ] **FoodInfo** (~889, M) — сытость/восстановление и подсказка оптимального порядка еды на полосе голода.
  - [ ] **ElytraTrails** (~2208, M) — светящиеся шлейфы с концов крыльев элитры, переиспользовать наш `Trails`.
  - [ ] **CustomHand** (~373, M) — пост-процесс шейдер руки (частично есть `shaders/hand_glass.frag`).
  - [ ] **DynamicLights** (~553, M) — факелы/лампы в руке светят по-настоящему; сначала проверить наш `Light`.
  - [ ] **TotemSound** (~257, S) — свой звук при срабатывании тотема.
  - [ ] **Sounds** (~195, S) — звук на клиентские действия (открытие инвентаря, прыжок, клик в GUI…).
  - [ ] **InventorySort** (~390, S/M) — сортировка инвентаря по пресету (клиентские QUICK_MOVE, без чужих пакетов).
  - [ ] **StorageTracker** (~678, M) — память содержимого контейнеров, поиск предмета, подсветка где лежит.
  - [ ] **ConsumeESP** (~671, M) — круг с кольцом прогресса над целью, когда она ест/пьёт.
  - [ ] **PatPat** (~645, M) — гладить моба пустой рукой (бинд, анимация/частицы, всё локально).
  - [ ] **Showcase** (~882, L) — кинематографичный облёт постройки камерой; запись клипа — позже (MVP: облёт).
  - P3 (следующий заход): Chunk Cache, Schematics, HitFix (риск античита), Camera, Graffiti, Skin Wheel,
    Discord RPC, Replay, Trainer. Полный список и сложности: отчёт, раздел 1.2.

## Открытые задачи
### Проверить в игре (код готов, вживую не смотрели)
- Автокомплит `%` в ванильном чате (`%`, `%to`, Tab, `%tier `); если миксин не применится, прислать лог.
- Режимы TargetESP Созвездие и Мечи; формы JumpCircles; кровь (лужи, яркость, depth); новые режимы KillEffect (ориентация
  граней частей, UV скина, яркость искр); ProjectileTrails (вид ленты, глубина); Hero/orbs-меню.
- HoldMyItems Scripted: знаки осей `M:rotate`, видимость Lua-частиц, анимация квадов на реальной модели.
- GuiPresence: нужен запущенный стек (`backend/scripts/dev-stack.sh`) и два клиента с привязанными аккаунтами на одном сервере.
- SpatialGui (инвентарь был прозрачным): сверено с `ref/mods/Spatial-GUI`; убрано ванильное затемнение в захвате (`ScreenSpatialBackgroundMixin`) и
  композитинг плоскости переведён на premultiplied-alpha пайплайн `WORLD_TEXTURED_PREMULTIPLIED`. Проверить в игре; если всё ещё прозрачно,
  следующий подозреваемый: глубина захвата (clear 1.0) и HUD внутри захвата (у эталона захватывается только экран, отдельным GuiRenderer).
- Войсчат с настоящим SVC 2.6.24 (события mute/speaking, значок шапки HUD).
- Размер плашки описания в ClickGUI и авто-масштаб тултипа при GUI Scale 2/3/4.

### Решения и долги
- Тип ClickGUI: селектор «Тип ClickGUI» (`classic` по умолчанию), точка выбора экрана `ClickGuiScreenManager`; ждёт решения,
  какой второй вид делать.
- ViewModel: свободный `Custom` swing и режимы sacura (Возвращение, Круговая); HitEffect: distortion/glow post-process;
  JumpCircles: easing на появление; KillEffect: toggle «сквозь стены»; Trails: сравнение с sacura вживую.
- Фаза 8.5.3 (история: раздел 8.5): около 150 файлов 250-400 строк и папки >4 файлов. Правило: касаешься файла по другой
  задаче, первым коммитом разбей на ≤250 (фасад + package-private хелперы, API вызывающих не трогать), вторым целевое изменение.
  Очередь самых больших: LoVisualRenderPipelines, DraggableHudElementRegistry, Predictions, DynamicIsland, Module,
  HudNotifier, AltManager, Triangulator, TargetESP, UiPrimitiveRenderer. Фасады (Renderer2D и т.п.) остаются исключением.
  При переносах пакетов не ломать id модулей и ключи настроек (совместимость `.lvcfg`), после move проверять grep'ом.
- Разнос `addon`/`events`/`mixininterface` по смыслу отложен (54/41/88 файлов-импортёров, чистый import-churn): только скриптовым
  отдельным проходом.
- Фаза 9 (FPS) закрыта: мёртвые грузы починены в коде, ручной модуль Optimize есть; авто-деградацию по FPS решено не делать.
- Фаза 7 (Delta QoL): Wasted и Arrows отменены, TapeMouse оставлен по решению владельца; список в истории.
- `.gitignore` для `server-plugin/.classpath`, `.project`, `.settings/`.
- Приёмка серверного плагина (Velocity + `%policy`), бэкенд chat-service и addons-registry (корневой TODO.md).

## Заметки для сессий
- Игру запускает владелец: `cd mod && ./gradlew runClient` (из корня репозитория `./gradlew` нет).
- Параллельные сборки блокируют друг друга: `flock /tmp/claude-1000/gradle.lock ./gradlew build --offline -q`.
- Helper-классы для миксинов нельзя класть в пакет `dev.loki.lovisual.mixins.*` (Mixin `IllegalClassLoadError`):
  им место в `mixinhandlers/`, `render/` или рядом с фичей.
- Мировые пайплайны (`WorldPipelines`) рисуют без отсечения задних граней и без записи глубины: порядок обхода вершин не важен.
- Параллельная сессия может коммитить в тот же репозиторий: перед `git commit --amend` смотреть `git log -3`.
