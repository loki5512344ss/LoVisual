# LoVisual Addon API (`api/v0`)

Аддоны — обычные Fabric-моды, которые расширяют LoVisual. Точка входа `lovisual:addon` в `fabric.mod.json`. Можно свободно менять публичный API при рефакторинге — владелец разрешил ломать совместимость ради KISS.

## Entrypoint

`fabric.mod.json`:
```json
{
  "schemaVersion": 1,
  "id": "my-addon",
  "name": "My Addon",
  "entrypoints": {
    "lovisual:addon": ["com.example.MyAddon"]
  },
  "custom": {
    "lovisual:addon": { "api": 0, "icon": "assets/my-addon/icon.png" }
  }
}
```

Класс:
```java
public class MyAddon implements LoVisualAddon {
    public void onConfigureModules(LoVisualModuleLoadContext ctx) {
        ctx.disableBuiltInModule("killAura"); // выключить встроенный модуль до его создания
    }
    public void onInitialize(LoVisualAddonContext ctx) {
        ctx.registerModule(MyModule.class);
        ctx.registerDraggableHudElement(MyHud.class);
        ctx.registerStaticHudElement(MyStaticHud.class);
        ctx.registerCommand(new MyCommand());
        ctx.registerClickGuiSection(new MySection());
        ctx.addRenderCallback(LoVisualRenderStage.HUD_RAW, (c) -> { /* 2D */ });
    }
    public void onClientReady(LoVisualAddonRuntimeContext ctx) {}
    public void onShutdown(LoVisualAddonRuntimeContext ctx) {}
    public void onRuntimeSuspended(LoVisualAddonRuntimeContext ctx) {} // при выкл. аддона
    public void onRuntimeResumed(LoVisualAddonRuntimeContext ctx) {}
}
```

`LoVisualAddon.API_VERSION = 0` — сверяется в `AddonManager.java:438`, несовпадение → `WARNING`.

## Жизнь аддона (`AddonManager.java:39`)

1. `discoverEntrypoints:211` — `FabricLoader.getEntrypointContainers` → `ADDONS` map, `ENABLED_OVERRIDES` из `AddonStateStore` (вкл/выкл сохраняется).
2. `prepareModuleLoading:58` — `configureEnabledAddons:230` → `onConfigureModules` (до `ModuleAutoLoader`).
3. `init:65` — `reconcileModuleExclusions:255` + `initializeEnabledAddons:272` → `onInitialize`.
4. `notifyClientReady:79` — `onClientReady` после загрузки конфигов.
5. Вкл/выкл из `LoVisualAddonManagerScreen.java:548` `AddonManager.setEnabled:160` — `suspend/resumeRuntimeContributions:363` глушит модули/HUD, пишет `AddonStateStore`.

Статусы `AddonStatus.java:12`: `DISCOVERED → ACTIVE / DISABLED / ERROR / SUSPENDED`.

## Что может аддон

- **Модули** `ctx.registerModule(Class<? extends Module>)` — обычный `@ModuleInfo`, попадёт в `ModuleManager`, конфиг `addon:ID:module:ID`.
- **HUD** `registerDraggableHudElement` / `registerStaticHudElement` — `@HudElementInfo` / `@HudElementRegister`, конфиг `addon:ID:hud.*`.
- **Команды** `registerCommand(ClientCommand)` — `@CommandInfo`, префикс `%`.
- **ClickGui секции** `registerClickGuiSection(LoVisualClickGuiSection)`.
- **Рендер** `addRenderCallback(stage, callback)` / `addPostProcessCallback` — `LoVisualRenderStage` (`HUD_RAW`, `WORLD_*`).
- **Расширения модулей** `LoVisualModuleExtension` — `beforeEnable/afterTick` и т.д. через `ModuleExtensionManager`.

Профили: `AddonManager.moduleProfileOwnerId:129` → `addon:ID:module:ID` для `.lvcfg`.

## Пример минимального аддона

См. `api/v0` JavaDoc и `addon/core/AddonManager.java:211`, `addon/pipeline/AddonSnapshot.java:14`, `features/gui/mainmenu/LoVisualAddonManagerScreen.java:48` (UI).

## Примечание по рефакторингу

Публичный Java API аддонов **можно ломать свободно** — KISS важнее обратной совместимости (решение владельца 2026-09-05, `TODO.md:9`). При рефакторе меняй сигнатуры, удаляй делегирование, обновляй вызывающих.

