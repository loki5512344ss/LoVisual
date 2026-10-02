# LoVisual Mod (26.2): план и статус

Мод вырос из форка Combatant Client. Подробная история (фазы 1-12) лежит в git: `git log -p -- mod/TODO.md`.

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
- [ ] Wings: режим «Модель» (Bedrock geo.json + animation.json, 14 видов; отчёт раздел 2.4). Шаги: `BedrockGeoParser` (общий с ножами),
      `BedrockAnimation` + процедурные angel/demon/dragon, `WingsRenderer.renderModel`, настройки `wingSource`/`wingModel`, тесты.
      Ресурсы (локально): `ref/evo/mod/jar/assets/evolution/cosmetic/wings/`, `.../textures/cosmetic/wings/`.
- [ ] Knife (Melee Skins): 33 скина, клипы draw/inspect/melee_1..3/run, first/third person, звуки, шейдер holy (отчёт раздел 3.7,
      порядок работ из 8 шагов). Ресурсы: `ref/evo/mod/jar/assets/evolution/cosmetic/melee/`.
- [ ] Быстрые модули P1: AutoSprint (есть `SprintControlEvent`), TotemCounter, EnchantGlint, PlaceAnimation, GodRays
      (референс `shaders/core/godrays`), ArmorColor.
- [ ] Дальше P1-P2: Blizzard, ElytraTrails (на нашем `Trails`), TotemSound, Sounds, InventorySort, StorageTracker, ConsumeESP,
      CustomHand, DynamicLights (проверить `Light`), FoodInfo, PatPat, Showcase. P3: Chunk Cache, Schematics, HitFix (риск античита),
      Camera, Graffiti, Skin Wheel, Discord RPC, Replay, Trainer. Пропустить: GuiPresence. Полный список: отчёт, раздел 1.2.

## Открытые задачи
### Проверить в игре (код готов, вживую не смотрели)
- Автокомплит `%` в ванильном чате (`%`, `%to`, Tab, `%tier `); если миксин не применится, прислать лог.
- Режимы TargetESP Созвездие и Мечи; формы JumpCircles; кровь (лужи, яркость, depth); новые режимы KillEffect (ориентация
  граней частей, UV скина, яркость искр); ProjectileTrails (вид ленты, глубина); Hero/orbs-меню.
- HoldMyItems Scripted: знаки осей `M:rotate`, видимость Lua-частиц, анимация квадов на реальной модели.
- Войсчат с настоящим SVC 2.6.24 (события mute/speaking, значок шапки HUD).
- Размер плашки описания в ClickGUI и авто-масштаб тултипа при GUI Scale 2/3/4.

### Решения и долги
- Тип ClickGUI: селектор «Тип ClickGUI» (`classic` по умолчанию), точка выбора экрана `ClickGuiScreenManager`; ждёт решения,
  какой второй вид делать.
- ViewModel: свободный `Custom` swing и режимы sacura (Возвращение, Круговая); HitEffect: distortion/glow post-process;
  JumpCircles: easing на появление; KillEffect: toggle «сквозь стены»; Trails: сравнение с sacura вживую.
- Фаза 8.5.3: около 150 файлов 250-400 строк и папки >4 файлов, чинить по мере касания.
- `.gitignore` для `server-plugin/.classpath`, `.project`, `.settings/`.
- Приёмка серверного плагина (Velocity + `%policy`), бэкенд chat-service и addons-registry (корневой TODO.md).

## Заметки для сессий
- Игру запускает владелец: `cd mod && ./gradlew runClient` (из корня репозитория `./gradlew` нет).
- Параллельные сборки блокируют друг друга: `flock /tmp/claude-1000/gradle.lock ./gradlew build --offline -q`.
- Helper-классы для миксинов нельзя класть в пакет `dev.loki.lovisual.mixins.*` (Mixin `IllegalClassLoadError`):
  им место в `mixinhandlers/`, `render/` или рядом с фичей.
- Мировые пайплайны (`WorldPipelines`) рисуют без отсечения задних граней и без записи глубины: порядок обхода вершин не важен.
- Параллельная сессия может коммитить в тот же репозиторий: перед `git commit --amend` смотреть `git log -3`.
