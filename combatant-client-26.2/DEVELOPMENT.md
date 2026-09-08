# LoVisual — руководство разработчика

Как устроен проект, как добавлять фичи и какие правила обязательны.
Версии: **Java 25 · Fabric Loader 0.19.3 · Minecraft 26.2** (Loom).

---

## 1. Сборка и запуск

```bash
cd combatant-client-26.2
./gradlew build        # jar: build/libs/lovisual-<ver>.jar
./gradlew test         # юнит-тесты (сейчас ~320)
./gradlew runClient    # быстрый dev-запуск Minecraft с модом
./gradlew runClient --rerun-tasks   # если кэш врёт
```

Готовый jar для теста в реальной сборке — скопировать в инстанс:

```bash
cp build/libs/lovisual-*.jar /storage/prismlauncher/instances/26.2/minecraft/mods/
```

Ресурсы (текстуры) — **только настоящие PNG** (сигнатура `89 50 4E 47`).
JPEG, переименованный в .png, уронит `Bad PNG Signature` при загрузке ресурспака.

---

## 2. Правила проекта (нарушение = откат)

- **KISS / DRY / SOLID** — простая структура, без оверинжиниринга.
- **≤ 200 строк на файл.** Фасад-делегат (например `Renderer2D`) — исключение.
  Больше логики — выноси package-private хелперы в тот же пакет.
- **≤ 4 файла на папку** — больше, дели папку по смыслу (подпапка).
- **Никакого статики от Minecraft**: `Minecraft.getInstance()` — только локальной
  переменной в месте использования, НИКОГДА в поле (`private final Minecraft mc = ...` — баг).
  Функциональный синглтон (`INSTANCE`) — только для сервисов без состояния жизненного цикла.
- **`Lifecycle.isActive()/canRunHud()` — гейты** для рендера/событий; `EventBus.post()`
  сам игнорирует события до activation.
- **Никаких this-escape** — конструктор не публикует `this`.
- **Никаких мёртвых миксинов** — миксин только если реально нужен;
  `cancellable=true` только при реальном cancel.
- **`dev.loki.lovisual.mixins.*` — ТОЛЬКО классы с `@Mixin`.** Обычный helper-класс
  внутри mixin-пакета крашит игру: Knot не даёт transformed-классу обратиться к нему
  (`IllegalClassLoadError`). Хелперы — в `util/` или пакет фичи (см. `HandDynamics`).
- **Без пустышек** — файл без реализации не создаётся.
- **Каждый этап компилируется**: `./gradlew build` перед коммитом.
- **Тесты обязательны** для новой чистой логики (JUnit 5, без моков Minecraft —
  выноси математику/парсинг в чистые статические методы и тесть их).
- **Референсы (`ref/`)** — чужой код (SoupVisuals SOUP-1.0, Delta и др.):
  **только идеи**, пере-реализация под наши правила, никакого копирования.

---

## 3. Карта пакетов

```
dev.loki.lovisual
├── LoVisual.java            — инициализация ClientModInitializer
├── Lifecycle.java           — INIT → ACTIVE → SHUTDOWN, гейты canRunHud/Render
├── events/                  — EventBus (@EventHandler, приоритеты, гейт по Module)
│   └── impl/                — конкретные события (Tick, RenderHud, Packet, Lightmap...)
├── config/                  — MainConfig, SettingDef, сериализация (.lvcfg, legacy .cbcfg)
│   ├── values/              — ConfigValue: bool/num/mode/color/set/... (есть тесты)
│   └── profile/             — профили конфигов (collect/apply/diff/binary codec)
├── features/
│   ├── module/              — Module, ModuleCategory, ModuleInfo, ModuleManager,
│   │   └── modules/...      — сами модули по категориям
│   ├── gui/                 — ClickGUI, HUD (draggable + nondraggable), BetterChat
│   ├── command/             — чат-команды (@CommandInfo, ClassGraph-поиск)
│   ├── relations/           — друзья/враги/стаф (PlayerRelations, CategoryService)
│   └── security/            — AntiBot-фильтр, StaffTracker
├── render/engine/           — Renderer2D/3D, пайплайны, шрифты, MeshBuilder
├── mixins/                  — @Mixin-классы (см. правило выше)
└── util/                    — чистые утилиты (text/animation/time/...) — тут живут тесты
```

---

## 4. Как добавить МОДУЛЬ

Модули находятся в `features/module/modules/<категория>/`. Категории:
`COMBAT · MOVEMENT · PLAYER · VISUALS · MISC` (см. `ModuleCategory`).
Легит-направление: читерские модули (автоудары, полёты, speed...) **удаляем, не добавляем**.

Регистрация автоматическая: **ClassGraph сканирует `@ModuleInfo`** — ничего никуда
дописывать не надо. Пустой конструктор обязателен.

Шаблон:

```java
@ModuleInfo(id = "mymodule", displayName = "My Module", category = ModuleCategory.VISUALS)
public class MyModule extends Module {

    private static final String SETTING_X = "x_amount";
    private final NumberValue<Integer> amount = num("myModuleAmount", SETTING_X, 5, 1, 10);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();      // локально, не в поле!
        if (mc.level == null) return;
        // ...
    }

    @EventHandler
    private void onRenderHud(RenderHudEvent event) { /* авто-подписка через EventBus */ }
}
```

Ключевые хуки `Module`: `onEnable/onDisable/onTick/onFrame`,
`onRenderHudEngine(...)`, `onRenderWorldEngine(...)`, `getWorldPhase()/getHudPhase()`.
Хелперы настроек сами объявляют `SettingDef`: `bool/num/mode/color/colorNoAlpha/
enumSetting/text/group/bind` + `visibleWhen(value, () -> cond)`.
Модуль-лист по умолчанию недоступен цели — смотри также `boolCommon`/`modeCommon`
для общих схем (`CommonSettingSchemas`).

Доступ к чужому модулю: `Modules.get(X.class)`, проверка состояния — `isEnabled()`.

Мировой рендер (боксы/линии) — через `Renderer3D.batch(WORLD_COLORED_LINES, DepthMode...)`
и MeshBuilder; готовые куски геометрии — `EspMeshBuilder.addOutlineBox` и т.п.

---

## 5. Как добавить HUD-элемент

Два вида, оба ищутся ClassGraph-ом автоматически:

**A. Draggable (перетаскиваемый, вкладка Settings → HUD):**
`features/gui/hud/draggable/impl/`, аннотация `@HudElementInfo(id, displayName,
enabledByDefault, order)`, базовый класс `DraggableHudElement`.
Обязательно: `applyDefaultPosition(screenW, screenH)`, рендер в `renderEngine(...)`,
`usesEngineRenderer() = true`. Позиция сохраняется через `layoutValue`.

**B. Static (фиксированный, вкладка Settings → UI):**
`features/gui/hud/nondraggable/impl/`, аннотация `@HudElementRegister(order=N)`,
базовый класс `AbstractHudElement`, конструктор `super(id, "Title", defaultEnabled)`,
синглтон `public static final X INSTANCE` (лоадер забирает его из статического поля).
Чтобы карточка появилась во вкладке **UI**, добавь `new StaticHudCard("<id>", "<Title>")`
в `STATIC_UI_CARDS` в `MenuSettingsResolver.java` (draggable-виджеты там не нужны —
они собираются из реестра сами).

Цвет из темы: `Theme.theme().accent()/windowBg()/textMuted()`; альфа —
`HudRenderUtil.setAlpha(argb, a)`; текст — `Fonts.renderer(name, type, fallback)`
(`begin(scale,...)` → `getWidth`/`render` → `end()`).

---

## 6. Как добавить СОБЫТИЕ / МИКСИН

1. Событие: наследуй `Event` (есть `cancel()`), положи в `events/impl/`.
2. Публикация: `Events.BUS.post(new MyEvent(...))` из миксина.
3. Миксин: класс с `@Mixin(Target.class)` в `mixins/` **и имя в `lovisual.mixins.json`**
   (секции `mixins`/`client`). Миксин, которого нет в json, не применится.
4. Если миксин опциональный (зависит от чужого мода, напр. Sodium) — цель может
   отсутствовать: Mixin напечатает WARN и пройдёт (см. `SodiumChunkMeshFormatsMixin`).
   Такой WARN — норма, краш — нет.
5. Чистая логика из миксина — вобы в обычный класс вне `mixins/`-пакета (см. правило про `HandDynamics`).

---

## 7. Настройки, конфиг и i18n

- Конфиги модулей/HUD пишутся в `<game>/lovisual/` в формате **`.lvcfg`**
  (legacy `.cbcfg` читается). Профили — `config/profile/` (кнопка в ClickGUI → Configs).
- Каждой настройке нужен i18n-ключ в **обоих** файлах
  `src/main/resources/assets/lovisual/lang/{en_us,ru_ru}.json`:
  - модуль: `module.<id>.description`, настройки `setting.<id>.<setting_id>`,
    опции режима `setting.<id>.<setting_id>.option.<Option>`;
  - draggable HUD: `hud.draggable.<id>` / `setting.draggable.<id>.<key>`;
  - static HUD: `setting.hud_element.<id>.<key>` (см. Watermark, Swap Tooltip).
- `setting_id` — snake_case (`"show_ping"`); configName —camelCase с префиксом мода
  (`"watermark_show_ping"`) — из него авто-выводится id.
- Пропущенный ключ ловит `MissingI18nReporter` в дев-окружении — проверяй консоль.

---

## 8. Тесты

`src/test/java/` зеркалит main. Пишутся **только на чистую логику** (без Minecraft-
рантайма): парсеры, математика, codec'ы, матчинги. Примеры:
- `util/text/NameProtectorTest`, `LegacyTextUtilTest` — парсинг/подмена,
- `config/values/*Test` — значения конфига,
- `config/profile/ConfigProfileValueCodecTest` — бинарный round-trip,
- `events/EventBusTest` — события (нужен `Lifecycle.activate()` в `@BeforeAll`).

Если фича завязана на Minecraft — выделяй статический pure-метод
(`static int tint(...)` / `static record Layout(...)`) и тесть его (см. `Light.tintAmbient`).

---

## 9. Чеклист перед коммитом

- [ ] `./gradlew build` зелёный, `./gradlew test` зелёный
- [ ] новых файлов >200 строк нет; папка не разрослась >4 файлов
- [ ] нет полей `Minecraft` / `getInstance()` в полях
- [ ] lang-ключи en_us + ru_ru добавлены для всех новых настроек
- [ ] тест на новую чистую логику есть
- [ ] в mixin-пакете только `@Mixin`-классы
- [ ] TODO.md обновлён (галка/заметка), если это этап плана
- [ ] в чужой код (`ref/`) не заглядывался копипаст
