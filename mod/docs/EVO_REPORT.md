# Отчёт по клиенту EvoVis (evo) для LoVisual

Дата: 2026-10-01. Режим: только чтение, в проекте ничего не менялось, кроме этого файла.

Источники (все пути абсолютные):

- Деобфусцированный мод evo: `/storage/project/jvm/LoVisual/ref/evo/mod/jar-src-named/evovis/` (читаемые имена классов, 1328 штук) и `/storage/project/jvm/LoVisual/ref/evo/mod/jar-src-plain/sources/defpackage/` (оригинальные короткие имена `afe.java` и т.п.). Исходники ещё с control-flow flattening (`switch (i + i2)`), логику читать можно, но вокруг много шума.
- Ресурсы evo (распакованный jar): `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/` (пространства имён `evolution` и `minecraft`).
- Каталоги модулей: `/storage/project/jvm/LoVisual/ref/evo/analysis/modules.txt` (57 модулей) и `lovisual_modules.txt` (88 наших).
- Таблица соответствия имён: `/storage/project/jvm/LoVisual/ref/evo/analysis/class_map.tsv` (старое имя, новое имя, роль).
- Чистый (не обфусцированный) референс HoldMyItems с Lua: `/storage/project/jvm/LoVisual/ref/clients/mercury-src/com/holdmylua/` и миксины `/storage/project/jvm/LoVisual/ref/clients/mercury-src/fun/mercury/mixin/hmi/` + `HeldItemRendererMixin.java`.
- Наш мод: `/storage/project/jvm/LoVisual/mod/src/main/java/dev/loki/lovisual/`.

ВАЖНОЕ ЗАМЕЧАНИЕ О ЛИЦЕНЗИИ. `ref/evo/README.md` прямо говорит: код и ассеты EvoVis проприетарные, "в LoVisual ничего не переносим, смотрим подходы и идеи". Поэтому копировать модели/текстуры/звуки/Lua evo в релиз как есть нельзя без разрешения. Форматы там стандартные (Bedrock geo.json + animation.json из Blockbench, Lua-скрипты формата HoldMyItems), так что реализацию можно делать на своих ассетах, а пути ниже нужны как карта и как тестовый материал для локальной отладки. Ниже в разделах 2 и 3 "пути для копирования" означают пути источников, которые нужны на время отладки. Для публичного релиза нужны либо разрешение, либо свои модели в тех же форматах.

---

## 1. Модули evo и чего нет у нас

Всего у evo 57 модулей (`evovis/module/{builders,combat,cosmetics,misc,render}`), у нас 88. Список наших взят из `@ModuleInfo` в `features/module/modules/*`.

Обозначения: сложность S (до дня работы), M (несколько дней), L (неделя и больше или нужна новая инфраструктура). Приоритет P1 (делать первым), P2, P3. "LOC" это размер класса модуля в evo (грубая оценка объёма; сам модуль часто тянет ещё `evovis/internal/*`).

### 1.1 Что у нас УЖЕ есть (аналог найден, переносить не нужно)

| Модуль evo | Наш аналог |
|---|---|
| Crosshair | `visuals/player/Crosshair` |
| HitBox | `visuals/visibility/Hitboxes` |
| Predictions | `combat/prediction/Predictions` |
| AspectRatio | `visuals/camera/AspectRatio` |
| BetterMinecraft | `misc/vanilla/BetterMinecraft` |
| BlockOverlay | `visuals/scene/BlockHighlight` (у evo есть ещё шейдерные режимы, см. 1.3) |
| Chams | `visuals/scene/visual/Chams` |
| HMI | `visuals/items/held/HoldMyItems` (урезанный аналог, раздел 4) |
| JumpCircles | `visuals/player/JumpCircles` |
| MotionBlur | `visuals/fx/MotionBlur` |
| NameTags | `visuals/nametags/NameTags` |
| TargetESP | `combat/prediction/TargetESP` |
| Trails | `visuals/effects/ambient/Trails` |
| WorldLyrics | `visuals/lyrics/KineticLyrics` |
| WorldTweaks | `visuals/weather/WorldTweaks` |
| Zoom | `visuals/fx/Zoom` |
| ItemScroller | `misc/items/ItemScroller` |
| NoRender | `visuals/visibility/NoRender` |
| Menu / Interface | `misc/ClickGui` и HUD-система `features/gui/hud/*` |
| BetterRain | `visuals/weather/precip/Rain`, `WetWorld` |
| FireFly | `visuals/particles/AmbientParticles` (там есть firefly) |
| Particles (на удар, тотем) | `visuals/effects/hits/HitEffect`, `visuals/fx/TotemFX` |
| Waypoints | частично: `visuals/world/Gps` + Xaero-совместимость `compat/xaero/*` |

### 1.2 Чего у нас НЕТ (полный список, 29 модулей)

| Категория | Модуль evo | Суть | LOC | Сложность | Приоритет |
|---|---|---|---|---|---|
| BUILDERS | Chunk Cache | держит посещённые чанки загруженными за пределами server view distance | 314 | M/L (клиентский ChunkCache, совместимость с Sodium) | P3 |
| BUILDERS | Schematics | призрачный предпросмотр и список материалов для litematic/schem/nbt | 1258 | L | P3 |
| BUILDERS | Showcase | находит постройку, летит кинематографичным путём вокруг и сохраняет клип | 882 | L (камера-путь это M, запись клипа это L) | P2 |
| COMBAT | HitFix | атаки по хитбоксу, который реально видишь, а не на тик вперёд | 665 | M (трогает сетевую логику атаки, риск античита) | P3 |
| COMBAT | TNTTimer | остаток фитиля у зажжённого TNT | 324 | S | P1 |
| COSMETICS | Cosmetics | 3D-косметика и плащи на игроках (крылья, рюкзаки, скины-модели, плащ) | 46 + `internal/Cosmetic*` | L в целом, M только для крыльев (раздел 2) | P1 |
| COSMETICS | Melee Skins | анимированные скины (ножи) на удерживаемых мечах | 860 | L (раздел 3) | P1 |
| COSMETICS | Camera | клиентская камера и штатив: кадр, подбег, фото | 1082 | M/L | P3 |
| COSMETICS | Graffiti | зажать клавишу и наспреить граффити на блок | 1010 | M | P3 |
| COSMETICS | PatPat | пустая рука на мобе, зажать клавишу, погладить | 645 | M | P2 |
| COSMETICS | Skin Wheel | поднять запястный циферблат (Omnitrix), крутить скины со вспышкой | 341 | M (нужны полные модели скинов, `cosmetic/skins`) | P3 |
| MISC | AutoSprint | держит спринт, когда разрешено | 19 | S (у нас уже есть `SprintControlEvent`) | P1 |
| MISC | TotemCounter | счётчик сломанных тотемов и своих | 255 | S | P1 |
| MISC | TotemSound | свой звук при срабатывании тотема | 257 | S | P2 |
| MISC | Sounds | звук на каждое клиентское действие | 195 | S | P2 |
| MISC | InventorySort | сортировка инвентаря по пресету | 390 | S/M | P2 |
| MISC | StorageTracker | запоминает содержимое контейнеров, ищет предметы | 678 | M | P2 |
| MISC | Discord RPC | статус в Discord | 56 | S (но нужна IPC-библиотека, зависимость) | P3 |
| MISC | Replay | хранит последние секунды геймплея, пишет клипы, тримминг | 526 + jcodec, `Evoclip` | L | P3 |
| MISC | Trainer | тренировочные дрели: прицел, тотем, якорь | 402 | M | P3 |
| RENDER | ArmorColor | красит надетую броню по остаточной прочности | 353 | S/M (у нас есть `EquipmentLayerRendererMixin`) | P2 |
| RENDER | Blizzard | высотная метель: ветровые снежинки, белая пелена, обмерзание краёв | 1290 | M/L | P2 |
| RENDER | ConsumeESP | круг с кольцом прогресса, что цель ест или пьёт | 671 | M | P2 |
| RENDER | CustomHand | пост-процесс шейдер руки | 373 | M (у нас есть `shaders/hand_glass.frag`, частично) | P2 |
| RENDER | DeathEffects | игроки рассыпаются на части тела при смерти | 2789 | L | P1 (одна из самых эффектных) |
| RENDER | DynamicLights | факелы и лампы в руке светят по-настоящему | 553 | M (проверить, что наш `Light` это не оно) | P2 |
| RENDER | ElytraTrails | светящиеся шлейфы с концов крыльев элитры | 2208 | M (переиспользовать наш `Trails`) | P2 |
| RENDER | EnchantGlint | замена блеска зачарования стилями | 274 | S/M | P1 |
| RENDER | FoodInfo | сытость, восстановление, подсказка оптимального порядка еды на полосе голода | 889 | M | P2 |
| RENDER | GodRays | лучи солнца сквозь листву и рельеф | 33 + шейдер `shaders/core/godrays` | M | P1 |
| RENDER | GuiPresence | показывает меню Evolution/YouTube, которое держат другие пользователи | 1577 | L (нужен бэкенд evo) | пропустить |
| RENDER | PlaceAnimation | поставленные блоки плавно скользят и наклоняются на место | 195 | S/M | P1 |
| RENDER | ProjectileTrails | гаснущие шлейфы за стрелами, жемчугом и прочими снарядами | 1401 | M (переиспользовать `Trails`) | P1 |

Итого отсутствует 29 из 57. Из них 3 из BUILDERS, 6 из COSMETICS, 12 из MISC (включая S-модули), остальное из RENDER и COMBAT.

### 1.3 Самое крутое (рекомендуемый порядок)

1. **Melee Skins (ножи)**: 33 скина, полные анимации draw/inspect/атаки/бег, звуки CS2-стиля, отдельный шейдер "holy". Раздел 3.
2. **Крылья из моделей** (14 видов, часть с анимацией). У нас уже есть модуль Wings, нужно добавить режим моделей. Раздел 2.
3. **DeathEffects**: распад тела на части (в evo есть шейдер `assets/evolution/shaders/core/death_blast`).
4. **GodRays**: готовые референс-шейдеры в `assets/evolution/shaders/core/godrays/`.
5. **Blizzard** и **ElytraTrails/ProjectileTrails**: хорошо ложатся на наш рендер-движок (`Renderer3D`, `MeshBuilder`).
6. **PlaceAnimation**, **EnchantGlint**, **ArmorColor**: маленькие, дают заметный визуальный эффект.
7. **Showcase** + **Camera** (кинематографика), **PatPat**, **Skin Wheel**, **Graffiti** (социальные косметики).
8. Дополнительно для существующего BlockHighlight: шейдерные режимы оверлея блока у evo (`assets/evolution/shaders/core/blockoverlay/{starfield,holy,plasma,nebula,cobweb}`).

Прочие ресурсы evo, которые могут пригодиться как референс: `assets/evolution/shaders/core/*` (liquid_glass, kawase_up/down blur, motionblur, godrays, hand, hand_smoke, rain, sky_*, customsky, chams_*, wetworld, trail_glass), `assets/evolution/fonts/msdf/`, `assets/evolution/textures/{jumpcircles,targetesp,mob_effect,particle}`, `assets/evolution/videos/menu_bg.mp4`.

---

## 2. Крылья (wings)

### 2.1 Где лежат и в каком формате

Формат: Bedrock Blockbench. Модель это `*.geo.json` (корень `minecraft:geometry`, `format_version` 1.12.0, у `easter` 1.21.0), анимация это `*.animation.json` (Bedrock animation, линейные keyframe, без Molang). Форматов obj/bbmodel/Geckolib у крыльев нет, свой парсер.

Каталоги (источник):

- Модели и анимации: `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/evolution/cosmetic/wings/` (13 geo, 10 animation, всего 340 КБ).
- Текстуры: `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/evolution/textures/cosmetic/wings/` (14 png, 88 КБ).

Точные файлы:

geo.json: `angel`, `blue_dragon`, `dark`, `demon`, `dragon`, `easter`, `ghoul`, `pulse`, `rocker`, `spider`, `trident`, `vulcano`, `wota`.

animation.json: `blue_dragon`, `dark`, `easter`, `ghoul`, `pulse`, `rocker`, `spider`, `trident`, `vulcano`, `wota`. У `angel`, `demon`, `dragon` анимации в файлах нет, они процедурные в коде (см. 2.3).

png: `angel_white`, `baby_dragon_white`, `blue_dragon`, `dark`, `demon_black`, `demon_red`, `easter`, `ghoul`, `pulse`, `rocker`, `spider`, `trident`, `vulcano`, `wota`.

### 2.2 Каталог: 14 видов

Каталог задан статически в `evovis/hud/WingsHud.java` (оригинал `defpackage/afe.java`), блок `static { WINGS.add(new Cosmetic5(key, name, DragonMode)) }`. Параметры привязки заданы в `evovis/internal/DragonMode.java` (оригинал `afc.java`): scale, offsetY, offsetZ. Для режимов без явных значений: scale 1.0, offsetY 0.45, offsetZ 0.0.

| key (id) | Название | geo | Текстура (размер png) | Анимация | scale / offsetY / offsetZ |
|---|---|---|---|---|---|
| baby_dragon_white | Baby Dragon (White) | dragon | baby_dragon_white.png (64x64) | процедурная `dragon_wings` | 1.0 / 0.45 / 0 |
| angel_white | Angel (White) | angel | angel_white.png (516x774) | процедурная `angel_wings` | 1.0 / 0.45 / 0 |
| demon_red | Demon (Red) | demon | demon_red.png (516x774) | процедурная `demon_wings` | 1.0 / 0.45 / 0 |
| demon_black | Demon (Black) | demon | demon_black.png (516x774) | процедурная `demon_wings` | 1.0 / 0.45 / 0 |
| pulse | Pulse Wings | pulse | pulse.png (32x32) | pulse.animation.json (idle, 3 с, 4 кости) | 1.0 / -1.2 / 0 |
| dark | Dark Wings | dark | dark.png (64x64) | dark.animation.json (idle, 4 с, 6 костей) | 0.9 / -1.05 / 0.05 |
| ghoul | Ghoul Wings | ghoul | ghoul.png (64x64) | ghoul.animation.json (Idle, 3 с, 21 кость) | 0.6 / -0.65 / 0 |
| rocker | Rocker Wings | rocker | rocker.png (128x128) | rocker.animation.json (main, 2.4 с, 25 костей) | 0.9 / -1.05 / 0 |
| wota | Wota Wings | wota | wota.png (64x640, 10 кадров по 64) | wota.animation.json (idle, 3 с, 8 костей) | 0.9 / -1.05 / 0 |
| trident | Trident Wings | trident | trident.png (32x32) | trident.animation.json (idle, 3.2 с, 1 кость) | 0.9 / -1.05 / 0.2 |
| easter | Easter Wings | easter | easter.png (128x128) | easter.animation.json (idle, 3 с, 19 костей) | 0.9 / -1.05 / 0 |
| blue_dragon | Dragon Wings (Blue) | blue_dragon | blue_dragon.png (128x128) | blue_dragon.animation.json (idle, 1.8 с, 5 костей) | 0.9 / -1.05 / 0 |
| spider | Spider Wings | spider | spider.png (256x256) | spider.animation.json (idle, 2.4 с, 5 костей) | 1.0 / 0 / 0 |
| vulcano | Vulcano Wings | vulcano | vulcano.png (64x64) | vulcano.animation.json (idle, 1.54 с, 4 кости) | 1.0 / 0 / 0 |

Размер png не равен `texture_width/height` в geo (текстуры в 2x выше). UV в geo задаются в пикселях геометрии, поэтому нормировать надо делением на `description.texture_width/texture_height` из geo, а не на размер png.

Число костей/кубов по geo: angel 15/12, blue_dragon 8/52, dark 9/14, demon 7/14, dragon 5/12, easter 45/158, ghoul 25/28, pulse 7/12, rocker 58/186, spider 11/94, trident 1/5, vulcano 4/14, wota 21/50.

Особый случай `wota`: анимированная текстура. `wota.png` это 10 кадров по 64 px высотой (64x640), смена кадра каждые 150 мс (`WOTA_FRAME_HEIGHT = 64`, `WOTA_FRAME_MILLIS = 150`, регистрация через `Animated.register` в `evovis/internal/Animated.java`, `Cosmetic5.getTexture()` дёргает `Animated.tick()`). У нас проще: брать смещение V = frame * (64/640).

Запасные режимы `AVIAN` и `INSECTOID` в `DragonMode` в каталог не попали, это встроенные JSON-анимации-заглушки в `WingsHud` (`AVIAN_ANIM`, `INSECTOID_ANIM`) для кастомных моделей. Нам не нужны.

### 2.3 Как рендерятся

Цепочка в evo:

1. Каталог и загрузка JSON: `evovis/hud/WingsHud.java` (`afe`). `geometry(mode)` читает `/assets/evolution/cosmetic/wings/<geometry>.geo.json` из classpath, `bundledAnimation(mode)` читает `<geometry>.animation.json`. Для `DRACONIC/ANGELIC/DEMONIC` вместо файла подставляется `{"procedural":"dragon_wings"}` и т.п.
2. Экипировка: `WingsHud.applyKey(playerName, key)` собирает JSON "косметики" (`{"name":"wing_<key>","id":..., "attachment":1,"scale":..,"offset":[0,offsetY,offsetZ],"rotation":[0,0,0],"animation":..,"model":<geo>}`) и регистрирует через `Internal34.registerWithTexture(json, textureId)`. `attachment: 1` это `AttachmentMode.BODY` (`evovis/internal/AttachmentMode.java`: FREE, BODY, HEAD, ABOVE_HEAD, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, FULL_BODY). Менеджер косметики: `evovis/internal/Internal34.java` (`adq`), дескриптор: `Internal35.java` (`adr`).
3. Слой рендера игрока: `evovis/internal/Internal33.java` (`adp`), `RenderLayer<AvatarRenderState, PlayerModel>`. Для каждой косметики вызывает `Controller.render(...)`, пропускает невидимых игроков.
4. Позиционирование: `evovis/internal/Controller.java`. Порядок: `bind(poseStack)`, `push`, привязка к части тела `applyAttachment` (для BODY: translate+rotate по `playerModel.body`, поэтому крылья следуют за наклоном при присяде; возвращает сдвиг -0.3), затем `rotateZ(180)`, `translate(offX, offY + attachmentShift, offZ + backOffset)`, `rotateY/X/Z(косметика)`, `scale(scale)`. `backOffset` отодвигает крылья от спины при надетой броне: 0.078125 для нагрудника, 0.1375 для крыльев поверх нагрудника (`WINGS_BACK_OFFSET`). Крылья скрываются при глайдере (элитре) на груди (`DataComponents.GLIDER`).
5. Геометрия: `evovis/hud/ModelRendererHud.java` (`adv`). `render()` вызывает `submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), ...)`, рекурсивно `renderPart(bone)`: translate offset, translate pivot, rotate, scale, translate pivot back, затем кубы (`renderCube`: на каждую из 6 граней по 4 вершины с позицией, UV, нормалью, `OverlayTexture.NO_OVERLAY`, light игрока).
6. Парсер геометрии: `evovis/internal/Rotation2.java` (`aid`), `Definition.java` (`aif`: textureWidth/Height, roots), `Parent.java` (`aig`: кость, pivot, rot, offset, scale, cubes, children).

Правила парсинга Bedrock geo в `Rotation2` (их надо воспроизвести):

- Кость: `pivot` читается как `(-x, y, z)`. `rotation` как `(rad(-rx), rad(-ry), rad(rz))`. Кубы внутри кости: `origin`, `size`, `pivot` (по умолчанию = origin), `rotation`, `inflate`, `mirror`; тот же поворот знаков.
- UV куба: либо box-UV массив `[u,v]` (стандартная раскладка Bedrock), либо per-face объект `{north:{uv,uv_size,uv_rotation},...}`; `uv_rotation` допустим только 0/90/180/270. Есть эвристика `collectFrontRects` для плоских кубов нулевой толщины (dedupe прямоугольников "south" против "north").
- Размеры в пикселях, масштаб 1/16 блока.

Анимации:

- Файловые (`*.animation.json`): корень либо `{"animations":{...}}`, либо плоский `{"idle":{...}}` (так у dark, pulse, trident, vulcano, wota; а у blue_dragon, easter, spider корень плоский, но имя клипа `animation.xxx.idle`). Берётся первый клип. Каналы: `rotation`, `position`, `scale`, значения `time -> [x,y,z]`, интерполяция линейная (`ModelRendererHud.interpolate`). Время: `(System.currentTimeMillis() - start) / 1000 % animation_length`. Применение (`ModelRendererHud.applyAnimation`): `rotX = base + rad(-x)`, `rotY = base + rad(-y)`, `rotZ = base + rad(z)`; `offset = base + position`; `scale = base * scale`. Базовые позы костей снимаются один раз (`snapshotBasePose`) и перед каждым кадром восстанавливаются (`resetPart`).
- Процедурные (angel, dragon, demon): `ModelRendererHud.applyProceduralAnimation` выбирает по имени: `angel_wings` это `Internal29` (`defpackage/adg.java`), `dragon_wings` это `Internal42` (`aei.java`), `demon_wings` это `Internal41` (`aeh.java`). Время `lifeTime = ((tickCount + partialTick) / 20) % 360`. Внутри обычные `cos/sin` от градусов на костях: angel (Left_Wing, Right_Wing, bone2..bone11; корневой масштаб 0.6), demon (Wing2, Wing3, bone2, bone3, bone8, bone9; множитель времени 200), dragon (left_wing, right_wing, left_tip, right_tip; частоты 120/300/600/2200). Формулы лежат в этих трёх классах в явном виде.

Вывод: у evo крылья не зависят от позы игрока (нет режима полёта/присяда, кроме следования за телом). Анимация общая по часам, а не по игроку.

### 2.4 Наш модуль Wings и как добавить режим "модель из evo"

Наш модуль: `/storage/project/jvm/LoVisual/mod/src/main/java/dev/loki/lovisual/features/module/modules/visuals/wings/`

- `Wings.java` (`@ModuleInfo(id="wings", category=ACCESSORIES)`): настройки `wingsType` (ModeValue из `WingShapes.TYPES`: Angelic, Dragon, Butterfly, Phoenix, Crystal, Mechanical, Fairy, Demon), target (Self/Friends/All Players), scale, flapping, flapStrength, flapSpeed, throughWalls, glow, outline, ribs, color. Фаза `WorldPhase.END_MAIN`, метод `onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta)`. Свой игрок показывается только не от первого лица и без элитры.
- `WingsRenderer.java`: рисует мировые вееры треугольников через `Renderer3D.batch(...)` и joml-матрицу `translate(player) · rotY(180-bodyYaw) · pre-translate · rotX(pitch) · rotZ(roll) · translate(anchor) · scale · [сторона]...`. Есть `resolvePose` (idle/sneak/glide).
- `WingShapes.java`: данные 8 силуэтов и пресеты поз (`POSE_IDLE`, `POSE_SNEAK`, `glidePose`).
- Тесты: `src/test/java/.../wings/WingShapesTest.java`, `WingsRendererTransformTest.java`.

Текстурный рендер в нашем движке уже есть и ровно подходит: `Renderer3D.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_DEPTH | WORLD_TEXTURED, Identifier texture, Renderer3D.DepthMode.MAIN | NONE)` и запись вершин `mesh.vec3(x,y,z).vec2(u,v).color(r,g,b,a).next()`, затем `mesh.quad(i1,i2,i3,i4)`. Пример: `features/module/modules/visuals/trollface/TrollfaceMask.java` строки 125-132. Текстуры берутся по `Identifier` (ресурс-пак `assets/lovisual/textures/...`). Значит миксин на слой игрока (как `Internal33` у evo) не нужен: можно трансформировать вершины на CPU так же, как это делает `WingsRenderer.put()` и `transformPosition()`.

План (режим "модель"):

1. Ассеты. Положить свои/разрешённые файлы в:
   - `src/main/resources/assets/lovisual/wings/<key>.geo.json` и `<key>.animation.json`;
   - `src/main/resources/assets/lovisual/textures/wings/<key>.png`.
   Для отладки можно скопировать (только локально) из:
   `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/evolution/cosmetic/wings/{angel,blue_dragon,dark,demon,dragon,easter,ghoul,pulse,rocker,spider,trident,vulcano,wota}.geo.json`,
   `.../cosmetic/wings/{blue_dragon,dark,easter,ghoul,pulse,rocker,spider,trident,vulcano,wota}.animation.json`,
   `.../textures/cosmetic/wings/{angel_white,baby_dragon_white,blue_dragon,dark,demon_black,demon_red,easter,ghoul,pulse,rocker,spider,trident,vulcano,wota}.png`.
2. Новые классы в пакете `wings`:
   - `WingModelDef` (record: key, displayName, geoKey, texture Identifier, animKey или procedural id, scale, offsetY, offsetZ). Таблица 2.2 переносится в статический каталог.
   - `BedrockGeoParser` (аналог `Rotation2`/`Definition`/`Parent`): читает `minecraft:geometry[0]`, строит дерево костей `Bone{name,parent,pivot,rot,cubes}`. Знаки pivot/rotation как в 2.3. Запекает кубы в массивы позиций и UV (нормировка на `texture_width/height` из description).
   - `BedrockAnimation` (аналог `ModelRendererHud.applyAnimation` и `interpolate`): разбор обоих корней (`animations` и плоский), линейные keyframe, `loop`.
   - `ProceduralWingAnims` (angel/demon/dragon): три функции `apply(Map<String,BoneState>, float timeSeconds)` с формулами из `Internal29/41/42`.
   - Реестр с кэшем, ленивая загрузка через `Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("lovisual", "wings/x.geo.json"))`.
3. Рендер. В `WingsRenderer` добавить `renderModel(renderer, player, tickDelta, bodyYaw, def, style)`:
   - Якорь: вызывать ту же цепочку матриц, что `drawWingSide` (translate player, rotY(180-bodyYaw), pre-translate/pitch от `resolvePose`), затем масштаб `def.scale`. По evo: начало координат модели лежит на `1.5 (шея) + offsetY - 0.3` блока над ногами (у режимов с offsetY около -1.05 это почти уровень ног, потому что координаты geo заданы от ног; у angel/dragon/demon с offsetY 0.45 это уровень шеи, кости ниже pivot). Z-сдвиг: `offsetZ + backOffset` (0.078125 при нагруднике, 0.1375 при крыльях и нагруднике) в сторону от спины. Знак по Z и направление взгляда модели подобрать на практике одним тестом (в тестах `WingsRendererTransformTest` есть пример проверки матриц).
   - Обход дерева костей: матрица кости = `translate(offset) · translate(pivot) · rotZYX(rot) · scale · translate(-pivot)`, в блоках (деление на 16). Куб рисовать 6 гранями, вершины через `transformPosition` и `batchTextured(WORLD_TEXTURED_DEPTH, texture, DepthMode.MAIN)`; для throughWalls брать `WORLD_TEXTURED` с `DepthMode.NONE`.
   - Время анимации: лучше `(player.tickCount + tickDelta) / 20` (у evo wall-clock, одинаково у всех игроков).
   - Свет: наш мировой пайплайн не знает lightmap. Умножить цвет вершины на яркость позиции игрока (`level.getBrightness`) или оставить `fullbright` как опцию, чтобы ночью крылья не светились.
   - Анимированная текстура wota: смещение V кадра.
   - Для режима glide и sneak взять существующую `WingShapes.glidePose/POSE_SNEAK` (повернуть всю модель по pitch, как сейчас), у evo этого нет, это наша добавка.
4. Настройки в `Wings.java`: добавить `ModeValue wingSource` ("Procedural" / "Model") и второй `ModeValue wingModel` (14 ключей из 2.2) с `visibleWhen(..., () -> "Model".equals(wingSource.get()))`. Прежний `wingType` показывать только для "Procedural". Цвет и glow для режима "модель" можно применять как tint (`color`) и дополнительным аддитивным проходом через `WORLD_TEXTURED_ADDITIVE`. Локализация: `assets/lovisual/lang/{en_us,ru_ru}.json` (в проекте есть `DescriptionCoverageTest`, ключи описаний обязательны).
5. Тесты: парсер на мини-geo, проверка знаков pivot/rotation, нормировки UV, интерполяции keyframe (по образцу `WingShapesTest`).

---

## 3. Ножи (knife, модуль Melee Skins)

### 3.1 Что это в evo

Модуль `evovis/module/cosmetics/MeleeSkinsModule.java` (`sz.java`, `@ModuleInfo(name="Melee Skins")`). Подменяет любой меч в руке (`ItemTags.SWORDS`, условие `appliesTo`) на выбранный "скин" (на самом деле полную 3D-модель ножа с анимированными руками). Настройки: `firstPersonSetting` (в своём виде), `thirdPersonSetting` (на теле), `inspectSetting` (клавиша осмотра, `OpenKeySetting`). Выбор скина хранится через `Melee3.equip/applyKey` и сохраняется в профиль косметики (`Cosmetic.persistIfLocal`), в нашем случае достаточно локального конфига.

### 3.2 Ресурсы (источник)

- Каталог моделей/анимаций: `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/evolution/cosmetic/melee/` (5.7 МБ).
  - `index.json` каталог скинов (33 записи, 16.5 КБ).
  - `<key>.geo.json` модель (Bedrock geo, `format_version` 1.12.0) и `<key>.anim.json` анимации (Bedrock animation `format_version` 1.8.0) на 21 геометрию (скины-варианты используют общую геометрию через поле `model`).
  - Геометрии: `annihil, bayonet, butterfly, cobra_vanguard, css, executor, karambit, m9, mercy, null_annihil, polaris, push, quenching, shadowkiller, silencefd, skeleton, stiletto, tactical, talon, thieve_sea, treasure_kn`.
- Текстуры: `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/evolution/textures/cosmetic/melee/<key>.png` (3.5 МБ; размеры от 128 до 2048 px; нормировка UV по `texture_width/height` из geo) и иконки слотов `.../textures/cosmetic/melee/slot/<key>.png` (33 штуки). Служебные `m9.png.ppm` и `m9amethyst.png.ppm` (786 КБ каждый) в мод не нужны.
- Звуки: `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/evolution/sounds/melee/` (94 ogg, 7.6 МБ), подпапки `annihil, cobra_vanguard, cs2, executor, mercy, polaris, quenching, shadowkiller, silencefd, thieve_sea, treasure_kn` (`val` пустая). Регистрация звуков: `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/evolution/sounds.json`, ключи `melee.<папка>.<имя>` (94 события; `melee.cs2.knife_slash1` -> `evolution:melee/cs2/knife_slash1`). Событие в анимации задаётся в `sound_effects` как `{"0.0":{"effect":"melee.cs2.knife_deploy1"}}`.
- Шейдер "holy": `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/evolution/shaders/core/melee_holy.vsh` и `melee_holy.fsh` (GLSL 330, шумовое поле fbm, дрейф, градиент; формат вершин ENTITY, Sampler0).

### 3.3 Все скины (из `index.json`)

Базовые (21 геометрия, `key` = имя файла): annihil "Dark Star" (варианты angel/devil, 16 клипов), cobra_vanguard "Cobra Vanguard", executor "Executor", mercy "Mercy", null_annihil "\"Free Dark Star\"", polaris "Desmoulins' Edge", quenching "Dragon's Fang", shadowkiller "Fiery Owl", silencefd "TAC Dagger", thieve_sea "Sea Gleam", treasure_kn "Creed", bayonet "Bayonet", butterfly "Butterfly", css "Classic Knife", karambit "Karambit", m9 "M9 Bayonet", push "Push", skeleton "Skeleton", stiletto "Stiletto", tactical "Huntsman", talon "Talon".

Перекраски (общая геометрия, поле `model` в index.json): bayoauto "Bayonet | Autotronic" (bayonet), buemerald "Butterfly | Emerald" и busnowfall "Butterfly | Snowfall" (butterfly), karobsidian "Karambit | Obsidian" (karambit), m9auto, m9amethyst, m9emerald, m9sapphire, m9holy (m9; `holyBones:["blade"]`), stilettosapphire, stilettott "Tiger tooth", stilettoholy (stiletto; `holyBones:["b"]`).

Итого 33 записи (21 базовая + 12 перекрасок). Текстуры у каждой своя `textures/cosmetic/melee/<key>.png`. Поля записи index.json: `key`, `name`, `model` (общая геометрия, опц.), `clips` (список клипов), `transforms` (`thirdperson_righthand`, `thirdperson_lefthand`, `ground`, `fixed`, `gui`, `head`: `rotation/translation/scale` для разных display-контекстов), `variants` (только annihil: `[{suffix:"",hidden:["angel"]},{suffix:"_angel",hidden:["devil"]}]`, скрытие костей и суффикс клипов), `holyBones` (кости, к которым применяется шейдер holy).

### 3.4 Структура модели и клипов

Кости, общие для всех ножей (пример karambit, m9, bayonet): `root > bone > knife_and_right > <knife> > weapon > ...`, `righthand`, `righthand_pos`, `lefthand`, `lefthand_pos`, `constraint`, а также служебные: `camera`, `view`, `refit_view`, `iron_view`, `idle_view`, `positioning > {ground, thirdperson_hand, fixed}`. Служебные имена, которые читает код (`evovis/internal/Model2.java`): `righthand_pos`, `lefthand_pos` (куда ставить руки игрока), `camera` (покачивание камеры), `idle_view` (якорь вида первого лица), `thirdperson_hand` (якорь в третьем лице).

Клипы (набор варьируется): `draw`, `draw_1` (случайный выбор варианта), `inspect`, `inspect_1`, `melee_1`, `melee_2`, `melee_3`, `static_idle`, `idle`, `run`, `run_start`, `run_end` (не у всех). У annihil суффикс `_angel`. Длительности: draw 0.96 до 1.12 с, melee 1.12 с, inspect от 6 до 13 с.

Формат клипа: `{"loop":true,"animation_length":..,"override_previous_animation":..,"bones":{<bone>:{"rotation":{"0.0":[x,y,z],...},"position":{...},"scale":{...}}},"sound_effects":{"t":{"effect":"melee..."}}}`. Ключи времени: строка секунд (шаг около 0.04 с).

### 3.5 Анимация: осмотр, доставание, удар, бег

Класс состояния: `evovis/internal/Inspect.java` (`defpackage/ahl.java`). Два экземпляра: `Inspect.getInstance()` (главная рука) и `Inspect.offHand()`. Состояние:

- Три трека (`BaseTrack`, `evovis/internal/BaseTrack.java`): `baseTrack` (вечная `static_idle`, режим `Mode6.LOOP`), `mainTrack` (одноразовые: draw, inspect, melee), `movementTrack` (`run_start` > `run` > `run_end`). Режимы `Mode6`: `LOOP`, `ONCE_STOP`, `ONCE_HOLD`.
- Конечный автомат: `MainStateMode.START` > при привязке модели проигрывает `draw` или (50% если есть) `draw_1` > `IDLE`. `MovementStateMode` для бега.
- Осмотр: `onInspect()` (вызывается по клавише из `MeleeSkinsModule.onKeyPressed`) работает только в `IDLE`; с 50% шансом играет `inspect_1` (если клип есть), иначе `inspect`. Переход 0.2 с.
- Атаки: `onAttack(boolean primary)`. ЛКМ чередует `melee_1` и `melee_2` (`PRIMARY_ATTACK_VARIANTS = 2`), ПКМ играет `melee_3`. Переходы 0.2 с (`TRANSITION_ACTION`). Хуки: `evovis/mixin/MeleeInputMixin.java` (`Minecraft.startAttack` HEAD, `Minecraft.startUseItem` RETURN, если не используется оффхенд).
- Бег: только при `player.isSprinting()`; `run_start` (0.2 с) > `run` (цикл по пройденной дистанции, `RUN_CYCLE_DISTANCE = 2.0` блока) > `run_end` (0.3 с). Не у всех ножей есть run-клипы (нет у bayonet/karambit/m9 и др.).
- Смена предмета/слота: `heldChanged` перепривязывает модель (`bind`), случайный вариант (`Variant`) выбирается из `variants`, скрытые кости помечаются `Model2.markHidden`.
- Композиция: `compose()` смешивает статичную позу (static fade) и аддитивную (additive fade) в `float[] composedPose` (на кость 9 float: rot xyz, pos xyz, scale xyz). Время считается по `System.nanoTime`, максимальный шаг 0.25 с.
- Интерполяция клипа: `evovis/internal/Internal68.java` (`CHANNEL_STRIDE=3`, `BONE_STRIDE=9`, Catmull-Rom сплайн; константы `CATMULL_HALF/TWO/FIVE`) и `Tracks.java` (`sample(time, float[] out)` по каналам rotation/position/scale).
- Звуки: `evovis/internal/Melee5.java` (`aia`): события `melee.*` из sounds.json, `play(name)` через `SimpleSoundInstance` в позиции игрока (SoundSource.PLAYERS, volume/pitch 1.0). `BaseTrack` ведёт курсор `sound_effects` (capacity 4) и останавливает звук при смене клипа.

### 3.6 Рендер

Хуки (все в `evovis/mixin/`):

- `MeleeItemInHandMixin` (на `ItemInHandRenderer`): `submitHandsWithItems` HEAD вызывает `Thirdperson.beginHands()`, RETURN вызывает `flushHandItem(...)` (отложенно рисует предмет в левой руке модели ножа); `submitArmWithItem` HEAD cancellable: если `MeleeSkinsModule.rendersFirstPerson(item)` то `Thirdperson.renderFirstPerson(player, hand, matrices, queue, light, retireProgress)` и отмена ванильного рендера; пустую главную руку скрывает `hidesEmptyMainHand`; не-меч в другой руке откладывается `deferHandItem`.
- `MeleePlayerItemInHandLayerMixin` (на `PlayerItemInHandLayer`): `submitArmWithItem` HEAD cancellable для третьего лица: `renderer.getModel().translateToHand(state, arm, matrices)`, `rotation XP -90`, `YP 180`, затем `Thirdperson.renderThirdPerson(...)` (только для своего игрока, `player.getId() == state.id`).
- `MeleeInputMixin` (на `Minecraft`): атака, см. выше.

Класс рендера: `evovis/internal/Thirdperson.java` (`defpackage/...`, на деле рендер первого и третьего лица). Основные приёмы:

- `buildWorld(PrimaryBones, Model2, pose[], hidden[])` считает мировые матрицы костей из позы клипа (`PrimaryBones`: `world`, `combined`, `normals`, `visible`).
- `applyCamera(model, inspect, poseStack)` применяет кость `camera` (покачивание камеры при осмотре и атаке). `applyViewAnchor` якорит модель по кости `idle_view`.
- Руки: `submitArm(...)` рендерит руки игрока (скин через аватар-рендер) в точках `righthand_pos` и `lefthand_pos` модели, `hideHands` скрывает нерелевантные. Левая рука зеркалится при левшах (`handMirrored`, `scale(-1,1,1)`).
- Геометрия ножа: `submitGeometry(...)` отправляет меш каждой кости через `submitCustomGeometry` с `RenderType` entityTranslucent и текстурой скина. Для `holyBones` используется `Melee.holy(texture)` (`evovis/internal/Melee.java`, `ahr.java`): `RenderPipeline "pipeline/melee_holy"` из `evolution:core/melee_holy`, формат вершин `DefaultVertexFormat.ENTITY`, translucent blend, без cull.
- Запечённая модель: `evovis/internal/Model2.java`: массивы `boneNames/boneParents/bonePivots/boneRestRotation`, `boneMeshes[i]` (float[]: на вершину 8 float: pos(3), uv(2), normal(3); `VERTEX_STRIDE = 8`, 4 вершины на грань), `clips: Map<String, Internal68>`, индексы `rightHandBone/leftHandBone/cameraBone/viewAnchorBone`. Бейк `Model2.bake(Definition, key)` из того же `Rotation2`-парсера, что и крылья.
- Загрузка и кэш: `evovis/internal/Melee4.java` (`ahx`): фоновый поток "evo-melee-loader", `ConcurrentHashMap<String, Model2>` кэш, `LOADING` set, вытеснение (`evict`). Модель читается из `/assets/evolution/cosmetic/melee/<geo>.geo.json` и `<geo>.anim.json` (`Melee2.modelResource()/animationResource()`).
- Третье лицо и инвентарь: кость `thirdperson_hand`, в `index.json` блок `transforms.thirdperson_righthand/lefthand` задаёт rotation/translation на скин, `ground/fixed/gui/head` для других display-контекстов (в evo на них отдельные поля не используются в коде рендера кроме third-person, но в данных есть).

### 3.7 Что нужно у нас для отдельного модуля Knife

Новый модуль: `features/module/modules/visuals/items/knife/Knife.java` (`@ModuleInfo(id="knife", displayName="Knife", category=ModuleCategory.VISUALS` или `ACCESSORIES`). Классы:

| Роль | Предлагаемое имя | Прототип в evo |
|---|---|---|
| Модуль и настройки | `Knife` (скин, firstPerson, thirdPerson, inspectKey, scale, offset, звук) | `MeleeSkinsModule` |
| Каталог и дескриптор скина | `KnifeCatalog`, `KnifeSkin` (record: key, geoKey, name, variants, holyBones, thirdPerson transforms, texture, icon) | `Melee3`, `Melee2`, `Variant`, `ThirdPersonLeftHand` |
| Парсер geo и бейк | `BedrockGeoParser` (общий с крыльями), `KnifeModel` | `Rotation2`, `Definition`, `Parent`, `Model2` |
| Клипы и сэмплинг | `KnifeClip`, `KnifeClipTrack` (LOOP/ONCE_STOP/ONCE_HOLD), `KnifeAnimationState` | `Internal68`, `Tracks`, `BaseTrack`, `Mode6`, `Inspect` |
| Рендер первого лица | `KnifeFirstPersonRenderer` | `Thirdperson.renderFirstPerson`, `buildWorld`, `applyCamera`, `applyViewAnchor`, `submitArm`, `submitGeometry` |
| Рендер третьего лица | `KnifeThirdPersonRenderer` | `Thirdperson.renderThirdPerson` |
| Шейдер holy | `KnifeRenderTypes` + шейдер `assets/lovisual/shaders/knife_holy.{vsh,fsh}` (писать по образцу `melee_holy`, регистрация pipeline в `LoVisualRenderPipelines`) | `Melee` + `melee_holy.vsh/fsh` |
| Звуки | `KnifeSounds` + `sounds.json` в `assets/lovisual/` | `Melee5` + `evolution/sounds.json` |
| Загрузка | `KnifeModelCache` (асинхронная, кэш, вытеснение) | `Melee4` |
| Миксины | `KnifeInputMixin` (на `Minecraft`), `KnifeItemInHandMixin` (на `ItemInHandRenderer`), `KnifePlayerItemInHandLayerMixin` (на `PlayerItemInHandLayer`) | `MeleeInputMixin`, `MeleeItemInHandMixin`, `MeleePlayerItemInHandLayerMixin` |

Ресурсы к копированию (источник, для отладки; см. предупреждение о лицензии): весь каталог `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/evolution/cosmetic/melee/` (`index.json`, 21 `.geo.json`, 21 `.anim.json`), все png из `.../textures/cosmetic/melee/` и `.../textures/cosmetic/melee/slot/` (кроме `.ppm`), каталог `.../sounds/melee/` + выбранные ключи из `.../sounds.json`, шейдеры `.../shaders/core/melee_holy.{vsh,fsh}`. Целевые пути у нас: `assets/lovisual/knife/*.json`, `assets/lovisual/textures/knife/*.png`, `assets/lovisual/sounds/knife/*.ogg`.

Что у нас уже есть и можно переиспользовать: `ItemInHandRendererMixin` (`mixins/render/item/ItemInHandRendererMixin.java`, уже перехватывает `submitArmWithItem` и `submitHandsWithItems`-путь), `HoldMyItems`/`ViewModel` (смещения руки), `EquipmentLayerRendererMixin`, текстуры через `Identifier`, `LoVisualRenderPipelines`. Чего нет и надо сделать с нуля: парсер Bedrock geo, система клипов, пайплайн и шейдер holy, миксин на `PlayerItemInHandLayer`, миксин ввода (атака/ПКМ), каталог скинов, UI выбора (иконки слотов).

Порядок работы: 1) общий `BedrockGeoParser` + бейк (он же нужен для крыльев), 2) клипы + `KnifeAnimationState` (draw, static_idle, inspect, melee_1..3 без бега), 3) рендер первого лица без рук (только нож), 4) руки игрока в `righthand_pos`/`lefthand_pos`, 5) третье лицо, 6) звуки, 7) бег, варианты annihil, holy-шейдер, 8) UI и сохранение выбора.

---

## 4. HoldMyItems и Lua ("нода на Lua")

### 4.1 Как это устроено в evo

evo бандлит оригинальный мод HoldMyItems целиком (миксин-конфиг `holdmyitems.mixins.json`, лист `"package":"evovis.mixin"`) и обёртку-модуль `HmiModule` (`evovis/module/render/HmiModule.java`: только флаг `active()`). Движок скриптов: LuaJ 3.0.1 (`/storage/project/jvm/LoVisual/ref/evo/mod/jar/META-INF/jars/luaj-jse-3.0.1.jar`, исходники `evovis`-дерева `org/luaj/vm2`).

Скрипты (источник): `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/minecraft/holdmyitems/`

| Файл | Размер | Назначение |
|---|---|---|
| `hand_pose.lua` | 66 КБ | поза рук "сцены" (экипировка, использование: bow, crossbow, trident, spear, eat/drink, brush, block, подмена свинга для мечей и т.д.) |
| `hand_relative_pose.lua` | 1.6 КБ | поза руки относительно сцены (пустая рука, смена предмета, бросаемое) |
| `item_pose.lua` | 45 КБ | поза самого предмета в руке |
| `item_model.lua` | 20 КБ | анимация частей (квадов) модели предмета: вёдра, лодки, пакеты |
| `hand_pose.lua.rpo`, `item_pose.lua.rpo` | 157 Б | опции Respackopts (`hmi.xOffset/yOffset/zOffset/inspectKeybind`) |

Необязательные "аддоны" подхватываются как `holdmyitems/hand_addon.lua`, `hand_relative_addon.lua`, `item_addon.lua`, `item_model_addon.lua` (со всех ресурс-паков, `getAllResources`).

Ресурсы вокруг скриптов (источник `/storage/project/jvm/LoVisual/ref/evo/mod/jar/assets/minecraft/`): `items/*.json` (35, лодки, вёдра, end_crystal и др.), `models/item/*.json` (48 3D/2D моделей, `eatinganimation/` 17 моделей), `textures/item/` (58), `textures/particle/` (36, пламя и glow для Lua-частиц), `optifine/`.

Классы evo (читаемые имена; оригинал в скобках):

- Загрузчик: `evovis/internal/Holdmyitems.java` (`SimpleSynchronousResourceReloadListener`, id `holdmyitems:lua_animation_loader`). Грузит 4 основных + 4 вида аддонов при перезагрузке ресурсов. Препроцессор `preprocessScript`: строки `global.x = v;` превращаются в `local x = registry:getOrDefault('x', v)` и в конце скрипта добавляется `registry:put('x', x)` (персистентные между кадрами переменные); плейсхолдеры `${name}` заменяются на `0`, если нет мода respackopts.
- Песочница и глобалы: `evovis/internal/Internal82.java` (`LuaScriptManager`): `Globals` с библиотеками Base, Package, Bit32, Table, String, Coroutine, JseMath, `LoadState.install`, `LuaC.install`.
- Кэш скрипта руки/предмета: `evovis/internal/HandScriptCache.java` (аналог `LuaScriptCache`): регистрирует в Lua глобалы `M, I, Texture, Items, Tags, P, Easings, KeyBindManager, S, C, particleManager, swingSpeed, registry, renderAsBlock, translateItem, itemSwingSpeed, animator, useDuration, usingItem, debugger, applyBlockRotation, context` и вызывает `chunk.invoke()` на каждый кадр. При ошибке Lua: тост "HMI Lua Runtime error!" и `canRun=false` до перезагрузки.
- Кэш скрипта модели: `evovis/internal/ItemModelCache.java` (глобал `data` вместо `context`), `executeModel`.
- Контекст кадра: `Context` (поля `mainHand, hand, bl (правая ли рука), swingProgress, mainHandSwingProgress, offHandSwingProgress, equipProgress, matrices (PoseStack), item, player, deltaTime, mainHandSwitchEvent, offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles`).
- Опасная часть: `evovis/mixin/JavaMethodMixin.java` патчит `org.luaj.vm2.lib.jse.JavaMethod.invokeMethod` и возвращает NIL для любого Java-метода без аннотации `@Safe` (кроме `getOrDefault`/`put`): так защищают от того, что Lua дёргает произвольные Java-методы.
- Обёртки API (Lua-видимые): `MInstance` (`M:`: moveX/Y/Z, rotateX/Y/Z (с pivot), scale, translate, shear, push/pop, sin/cos/abs/round/floor/ceil/pow/clamp/lerp), `IInstance` (`I:`: isOf, isIn, isEmpty, isBlock, isThrowable, isLantern, isChargedCrossbow, isEnchanted, getUseAction, getName, getAttackDamage, getSpearData, setSwingSpeed, setRenderAsBlock, setTranslate, shouldRenderAsBlock, shouldTranslateItem, isCustomTranslate, setMainStack/setOffStack, getComponents, copyAppearanceComponents, getDefaultStack), `PInstance` (`P:`: getSpeed, getXSpeed/YSpeed/ZSpeed, getYaw/Pitch, getX/Y/Z, getAge, getHealth, isOnGround, isSneaking, isSwimming, isCrawling, isClimbing, isUsingItem, isUsingRiptide, isUsingSpyglass, isTouchingWater, isSubmergedInWater, hasVehicle, isItemCoolingDown, getActiveHand, getMainItem, getOffhandItem, getSwingCount, getBlockBelow/Above/Standing), `EasingsInstance` (30 easing функций: easeInOutBack, easeOutElastic, easeOutBounce, cubicEase и др.), `JsItemsInstance` (`Items:get("minecraft:...")`), `JsTagsInstance` (`Tags:getVanillaTag/getFabricTag`), `KeyBindManagerInstance` (`isKeyPressed`), `SInstance` (`S:playSound`), `CInstance` (`C:setCamRot/setCamPos`, камера), `TextureInstance`, `ParticleManagerInstance` (`particleManager:addParticle(...)` с Lua-колбэком на тик; сама система частиц `Particle`, `ParticleManager`, рендер-типы в мире и в руке).
- Анимация частей модели предмета: `ModelPartAnimator` (`animator:rotateX/Y/Z(from, to, deg, pivotX, pivotY, pivotZ)`, `moveX/Y/Z`, `scale` по диапазону индексов частей `from..to`, применяется при рендере квадов через `ItemRendererMixin`/`BlockRenderManagerMixin`).

Чистый, не обфусцированный референс тех же классов: `/storage/project/jvm/LoVisual/ref/clients/mercury-src/com/holdmylua/source/`:
`lua_runtime/{LuaScriptManager,LuaScriptCache,ModelScriptCache,ScriptHolder}.java`, `lua_runtime/resource_controller/LuaAnimationResourceLoader.java`, `global/{LuaContext,GlobalsStorage,DispatcherStorage}.java`, `global/item_model/*`, `scripting/script_wrappers/{M,P,IInstance,Easings,C,S,JSItems,JSTags}.java`, `scripting/custom_api/*`, `model/{ModelPartAnimator,PoseX/Y/Z,RotationX/Y/Z,ScalePose}.java`, `patricles/*` (Particle 450 строк, ParticleManager), `annotation/Safe.java`, `access/*` (аксессоры), `SwordAttacks.java`, `data_structures/SpearData.java`, `compat/IrisCompat.java`.
Миксины: `/storage/project/jvm/LoVisual/ref/clients/mercury-src/fun/mercury/mixin/hmi/*` (BlockRenderManagerMixin, HandRenderTypeMixin, HmiCameraMixin, HmiGameRendererMixin (deltaTime по GLFW, не более 0.05), HmiLivingEntityMixin (счётчики свинга), HmiMinecraftClientMixin (doAttack, doItemUse), ItemRendererMixin, ItemRenderStateMixin, ItemStackMixin (поля transform, swingSpeed, renderAsBlock), JavaMethodMixin, UseDurationMixin, UsingItemMixin) и главный `HeldItemRendererMixin.java` (905 строк; точки вызова скриптов: `hmi$itemPose` > `item_pose`, `hmi$mainHandPose` > `hand_relative_pose`, `hmi$scenePoseMain` > `hand_pose`; `hmi$applyArmMatrices` копирует ванильное построение руки; `wrapApplyEquipOffset`/`wrapSwingArm` отключают ванильную экипировку и свинг; `hmi$renderOverhaul` переопределяет `renderItem`).

Другие Lua-системы в ref (для идей "нод"/скриптов модулей, не руки):

- `/storage/project/jvm/LoVisual/ref/clients/sacura-1.21.4/src/main/java/su/sacura/util/impl/lua/`: `LuaManager` (папка `<run>/sacura/lua`, вызывает глобальные `onEnable/onDisable/onUpdate/onRender` у каждого скрипта-модуля), `LuaScript` (скрипт как `Module`), API `ClientApi, GlobalApi, ModulesApi, PlayerApi, Render3DApi, RenderApi, SettingsApi, Vec2`, `render/LuaFontManager`. Зависимость: `org.luaj:luaj-jse:3.0.1` (в `build.gradle` как `include`).
- `/storage/project/jvm/LoVisual/ref/clients/newcode-1.21/` (`FiguraAvatarInstaller`, `figura_avatars/*/script.lua`): Figura-аватары на Lua (`test_luaj.gradle`).
- `/storage/project/jvm/LoVisual/ref/clients/mercury-src/` (HoldMyLua, выше).
- `/storage/project/jvm/LoVisual/ref/README.md` упоминает `hmi` (HoldMyItems event API) и `mercury-src` (HoldMyLua), но каталога `ref/clients/hmi` на диске сейчас нет; в `mod/TODO.md` строки 546-560 описан разбор HMI.zip в `/tmp/lv_refs/hmi/` (Delta HMI, обёртка над сторонним `holdmyitems-5.1.1+26.2.jar`).

### 4.2 Что есть у нас

Только чистая Java-динамика, без Lua и без скриптов:

- `features/module/modules/visuals/items/held/HoldMyItems.java` (`id="holdmyitems"`, displayName "Item Holder"): настройки `sway_intensity`, `ease_strength`, `offhand`, `walk_bob`/`bob_strength`, `use_drift`/`drift_strength`, `join_appear`, `grip_enabled` + 6 слайдеров grip (`gripExtend/Slide/Out/Forward/Tilt/Turn/Roll`). Метод `onFrame` вызывает `HandAnimState.beginFrame`. Метод `shapeEquipProgress`.
- `util/animation/hand/HandDynamics.java` (применяет позу в `submitArmWithItem` HEAD через `pushPose`), `HandPoseMath` (sway, bob, drift, joinDrop, grip), `HandAnimState`, `SwayState`, `BobMath`, `HeldAlongFist`.
- `util/animation/EquipProgressComposer.java` + `EaseCurves` (кривая equip).
- `mixins/render/item/ItemInHandRendererMixin.java`: инъекции `submitArmWithItem` HEAD/RETURN (динамика руки), HEAD (ViewModel offsets), `@ModifyVariable` equipProgress (ordinal 3), `renderItem` (мини-предметы), `swingArm` (кастомные свинги ViewModel `Hammer/Chop/Arc/Double/Shake/Shove/Slash/Thrust` из `visuals/camera/swing/*`).
- Родственные: `visuals/camera/ViewModel.java`, `visuals/items/held/AlwaysVisibleHand.java`, `mixins/render/item/ItemInHandAlwaysVisibleMixin.java`.

Решение по Lua в `mod/TODO.md` (строки 546-558): полный HMI не портировали из-за правила "без стороннего jar в сборке" и KISS; взята только идея grip. Скриптового движка для рук нет. В проекте есть только Javet (V8) для UI/HUD-скриптов: `render/engine/renderer/ui/runtime/script/engine/{UiScriptEngine,JavetUiScriptEngine,UiScriptEngineProvider}.java` (подключение в `build.gradle`: `bundledLibrary "com.caoccao.javet:javet:5.0.9"`, строка 186). Lua/LuaJ в проекте отсутствует (в зависимостях нет `luaj`).

### 4.3 Чего не хватает до полного функционала HMI (список)

Движок и инфраструктура:

1. Зависимость LuaJ: `bundledLibrary 'org.luaj:luaj-jse:3.0.1'` в `mod/build.gradle` рядом со строкой Javet. Версия та же, что у evo, mercury и sacura. Лицензия LuaJ (MIT) допускает бандл, но это "сторонняя библиотека в сборке", как Javet, а не сторонний мод.
2. Пакет `features/module/modules/visuals/items/held/lua/`: `LuaHandRuntime` (общие `Globals`, как `Internal82`), `HandScript` (аналог `HandScriptCache`: 3 основных скрипта руки/предмета + аддоны), `ItemModelScript` (аналог `ItemModelCache`), `HandScriptContext` (поля из `Context`), `ScriptHolder`, `LuaScriptPreprocessor` (регэкспы `global.x = v;` > `registry`, `${name}` > `0`), `HmiResourceReloadListener` (загрузка `holdmyitems/*.lua` из ресурс-паков, плюс из `config/lovisual/holdmyitems/` для пользовательских скриптов).
3. Песочница. Evo и mercury используют миксин на `JavaMethod` + `@Safe`. Лучше без миксина на сторонний класс: не отдавать Lua `CoerceJavaToLua.coerce(javaObject)` (даёт доступ ко всем public-методам, включая `getClass()`), а собирать `LuaTable` с `LibFunction`-обёртками только нужных методов (M, I, P, Easings, Items, Tags, S, C, KeyBindManager, particleManager, registry). `Globals` без `JseBaseLib.loadfile/dofile` и без `luajava`, `io`, `os`. Это закрывает вопрос безопасности скриптов из чужих ресурс-паков.
4. Реестр персистентных переменных `registry` (HashMap) со сбросом при перезагрузке ресурсов.
5. Дельта времени кадра: по GLFW (`HmiGameRendererMixin`: `min(0.05, dt)`, 0 при паузе). У нас есть `render/helpers/util/TickDelta`.
6. Тосты/лог ошибок Lua (`canRun=false` до reload).

Хуки рендера (нашего `ItemInHandRendererMixin` для этого мало):

7. Подмена всей цепочки руки: обернуть/перенаправить вызов построения руки и предмета (аналог `hmi$renderOverhaul`, `wrapApplyEquipOffset`, `wrapSwingArm`), чтобы Lua управлял `equipProgress` и свингом вместо ванили. Сейчас у нас только `ModifyVariable` equipProgress, `swingArm` cancel для ViewModel и pushPose/popPose.
8. Раздельные фазы скриптов: `hand_pose` (сцена), `hand_relative_pose` (рука относительно), `item_pose` (предмет), плюс вызов `item_model` при рендере модели.
9. Счётчики свинга и события (`mainHandSwingProgress/offHandSwingProgress`, `swingMHand/swingOHand`, `interact`, `blockBreaking`, `mainHandSwitchEvent/offHandSwitchEvent`): миксины `HmiLivingEntityMixin`, `HmiMinecraftClientMixin` (doAttack, doItemUse), `getSwingCount`.
10. Рендер обеих рук всегда (`HandRenderTypeMixin`: `renderMainHand/renderOffHand = true`).
11. Подмена параметров ItemStack на лету: `ItemStackMixin` (поля transform, swingSpeed, renderAsBlock) и `setMainStack/setOffStack`, `copyAppearanceComponents`.
12. Подмена свойств item-моделей `use_duration` и `using_item` (`UseDurationMixin`, `UsingItemMixin`) для анимаций еды/лука/арбалета.
13. Аниматор частей модели (`ModelPartAnimator` + хуки на рендер квадов `ItemRendererMixin`, `BlockRenderManagerMixin`, `ItemRenderStateMixin` сброс диспетчера) для `item_model.lua` (вёдра с рыбой, лодки, пакеты).
14. Камера из Lua (`C:setCamRot/setCamPos`, `HmiCameraMixin`) для crawl/climb/riptide.
15. Lua-частицы: `Particle`, `ParticleManager`, рендер-типы (мир/рука), `particleManager:addParticle` с Lua-колбэком (пламя, glow).
16. Привязка клавиши осмотра (`KeyBindManager`, `hmi.inspectKeybind`, `S:playSound`). У нас есть настройки кнопки в ClickGUI (`OpenKeySetting`-аналог), надо пробросить в Lua.

Контент:

17. Сами скрипты: `hand_pose.lua`, `hand_relative_pose.lua`, `item_pose.lua`, `item_model.lua` (источник: каталог из 4.1). Для релиза свои или с разрешением автора HMI/evo. Скрипты завязаны на ресурсы `assets/minecraft/items/*.json`, `models/item/*` (3D-вёдра, лодки, eatinganimation), `textures/particle/*`.
18. Настройки в ClickGUI: выбор набора скриптов, переключатели per-feature (inspect, eating, bow, crossbow, trident, spear, sword attacks), горячая перезагрузка скриптов (`.reload`-команда или кнопка).
19. Тесты: препроцессор `global.x`, песочница (скрипт не вызывает `os/io`, не достаёт `getClass`), математика `M:` и `Easings:` (по образцу `HandPoseMath`-тестов).

Что у нас лучше evo: готовые Java-режимы (sway/bob/drift/grip), настраиваемые слайдеры в ClickGUI, ViewModel-свинги, нет зависимости от Lua-интерпретатора и риска чужих скриптов. Рекомендация: сначала добавить пункты 1-8 как режим "Scripted" внутри существующего `HoldMyItems` (переключатель `Dynamics (Java)` / `Scripts (Lua)`), собственные короткие примеры скриптов, и лишь затем наращивать 9-16.

### 4.4 Минимальный план "нода на Lua" (если нужна именно скриптуемая нода)

Идея аналогична sacura `LuaManager`/`LuaScript`: Lua-файл = модуль/узел, который объявляет `onEnable`, `onDisable`, `onUpdate`, `onRender`. Минимум:

1. `LuaScriptManager` + `LuaScript` (по образцу `sacura/util/impl/lua`), папка `<run>/lovisual/lua`, регистрация скрипта как `Module` в нашем реестре модулей (`features/module/lifecycle/Modules`).
2. Сначала только безопасные API: `Player`, `Render3D` (линии, квады через `Renderer3D`), `Settings` (создание num/bool/mode значений как у `Module.num/bool/modeSetting`), `Client` (chat print).
3. Для hold-items узла: отдельный тип скрипта `held`, вызываемый из `ItemInHandRendererMixin` с `HandScriptContext`, `matrices`, глобалами `M, I, P, Easings` из 4.1.

Альтернатива Lua: у нас уже есть Javet/V8 (`UiScriptEngine`). Если совместимость с готовыми HMI-паками не нужна, то скрипты рук можно писать на JS и не тянуть второй интерпретатор. Но экосистема HMI (готовые `hand_pose.lua` и т.д.) только на Lua, поэтому для "полного функционала HMI" нужен именно LuaJ.

---

## 5. Итоговый приоритет работ

1. Общий `BedrockGeoParser` + бейк + сэмплер анимации (нужен и крыльям, и ножам). Затем режим "модель" в Wings (раздел 2.4): ближайший заметный результат при умеренной сложности (M).
2. Модуль Knife (раздел 3.7): самая эффектная фича, сложность L, строить этапами.
3. Быстрые модули: TNTTimer, AutoSprint, TotemCounter, PlaceAnimation, EnchantGlint, ArmorColor, GodRays, ProjectileTrails.
4. DeathEffects, Blizzard, ElytraTrails.
5. LuaJ-движок и скриптовый режим HoldMyItems (раздел 4.3, пункты 1-8), потом остальное.
6. Лицензионный вопрос по ассетам evo решить до начала копирования любых файлов.
