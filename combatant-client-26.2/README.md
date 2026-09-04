<div align="center">

# LoVisual

Minecraft utility client for PvP and automation.
Developed and maintained by loki5512344.

![Java](https://img.shields.io/badge/Java-25+-orange?style=flat-square&logo=openjdk&logoColor=white)
![Fabric](https://img.shields.io/badge/Fabric-1.21.5-dimgrey?style=flat-square&logo=data:image/png;base64,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)
![License](https://img.shields.io/badge/license-GPLv3-blue?style=flat-square&logo=gnu&logoColor=white)

[English](#english) | [Русский](#русский)

</div>

---

## English

### Overview

LoVisual is an open-source utility and PvP client for Minecraft. It provides configurable combat, movement, visual, automation, HUD, and UI features with a custom ClickGUI, theme system, and addon API.

LoVisual is developed and maintained by [loki5512344](https://github.com/loki5512344). The goal is to keep adding new features and maintain compatibility with the latest Minecraft versions.

### Quick Start

1. Install Fabric Loader 0.19.3+ for MC 26.2+
2. Download the LoVisual jar
3. Place it in your `mods/` folder along with Fabric API and Sodium
4. Open the ClickGUI with **`RShift`** (configurable)

### Features

| Feature | Description |
|---------|-------------|
| Combat | KillAura, AutoCrystal, AutoAnchor, AutoBed, AutoBow, Criticals, Reach, Velocity, Backtrack, and more |
| Movement | Speed, Flight, ElytraFly, ElytraHelper, Sprint, Timer, Strafe, TargetStrafe, Scaffold, Parkour |
| Visuals | ESP, NameTags, Chams, Tracers, Crosshair, Zoom, Freecam, FullBright, MotionBlur, NoRender |
| Player | AutoTool, AutoEat, AutoArmor, AutoTotem, Scaffold, ClickPearl, FakeLag, XCarry |
| Misc | BetterMinecraft, FakePlayer, HitSounds, Panic, NoSound |
| HUD | Draggable elements, TargetHUD, ModuleList, Watermark, Armor, Potions, Keystrokes, Coords, Notifications, Scoreboard |
| ClickGUI | Full-featured GUI with module browser, settings editor, theme editor, config profiles |
| Config profiles | `.lvisual` profiles for modules, HUD, and themes — shareable between users |
| Addon API | Versioned addon API for extending the client |
| Custom renderer | Custom UI rendering with MSDF fonts, SVG icons, and shader support |
| Iris/Sodium/Xaero | Optional compatibility with popular mods |

### Compatibility

| Mod | Status |
|-----|--------|
| Sodium | ✅ Required |
| Iris | ✅ Optional — shaderpack support |
| ImmediatelyFast | ✅ Optional — performance |
| Entity Culling | ✅ Optional — performance |
| Xaero's Minimap | ✅ Optional — waypoint integration |
| Xaero's World Map | ✅ Optional — waypoint integration |
| ViaFabricPlus | ✅ Optional — protocol translation |

### Dependencies

- **Required:** Fabric Loader 0.19.3+, Fabric API, Sodium, Java 25+
- **Optional:** Iris, ImmediatelyFast, ViaFabricPlus, Entity Culling, Xaero's Minimap, Xaero's World Map

### Installation

1. Download the jar from [Releases](../../releases)
2. Place it in your `mods/` folder
3. Restart the game

---

## Русский

### Обзор

LoVisual — открытый утилитарный и PvP-клиент для Minecraft. Включает настраиваемые боевые, двигательные, визуальные и автоматизированные модули, кастомный ClickGUI, систему тем и API для аддонов.

LoVisual разрабатывается и поддерживается [loki5512344](https://github.com/loki5512344). Цель — добавлять новые возможности и поддерживать актуальные версии Minecraft.

### Быстрый старт

1. Установите Fabric Loader 0.19.3+ для MC 26.2+
2. Скачайте LoVisual jar
3. Положите в папку `mods/` вместе с Fabric API и Sodium
4. Откройте ClickGUI через **`RShift`** (настраивается)

### Возможности

| Возможность | Описание |
|-------------|----------|
| Combat | KillAura, AutoCrystal, AutoAnchor, AutoBed, AutoBow, Criticals, Reach, Velocity, Backtrack и другие |
| Movement | Speed, Flight, ElytraFly, ElytraHelper, Sprint, Timer, Strafe, TargetStrafe, Scaffold, Parkour |
| Visuals | ESP, NameTags, Chams, Tracers, Crosshair, Zoom, Freecam, FullBright, MotionBlur, NoRender |
| Player | AutoTool, AutoEat, AutoArmor, AutoTotem, Scaffold, ClickPearl, FakeLag, XCarry |
| Misc | BetterMinecraft, FakePlayer, HitSounds, Panic, NoSound |
| HUD | Перетаскиваемые элементы: TargetHUD, ModuleList, Watermark, Armor, Potions, Keystrokes, Coords, Notifications, Scoreboard |
| ClickGUI | Полноценный GUI с браузером модулей, редактором настроек, тем и профилей конфигов |
| Профили `.lvisual` | Профили для модулей, HUD и тем — можно шарить между пользователями |
| Addon API | Версионированное API для аддонов |
| Кастомный рендер | UI-рендеринг с MSDF-шрифтами, SVG-иконками и шейдерами |
| Iris/Sodium/Xaero | Опциональная совместимость с популярными модами |

### Совместимость

| Мод | Статус |
|-----|--------|
| Sodium | ✅ Требуется |
| Iris | ✅ Опционально — шейдерпаки |
| ImmediatelyFast | ✅ Опционально — производительность |
| Entity Culling | ✅ Опционально — производительность |
| Xaero's Minimap | ✅ Опционально — интеграция waypoint |
| Xaero's World Map | ✅ Опционально — интеграция waypoint |
| ViaFabricPlus | ✅ Опционально — протокол |

### Зависимости

- **Обязательные:** Fabric Loader 0.19.3+, Fabric API, Sodium, Java 25+
- **Опциональные:** Iris, ImmediatelyFast, ViaFabricPlus, Entity Culling, Xaero's Minimap, Xaero's World Map

### Установка

1. Скачайте jar из [Releases](../../releases)
2. Положите в папку `mods/`
3. Перезапустите игру

---

## Development

### Building

```bash
./gradlew build
```

### Docs

- [Architecture](docs/ARCHITECTURE.md)

### License

GNU General Public License v3.0. See [LICENSE](LICENSE).

---

## TODO

План разработки и актуальный статус ведутся в [TODO.md](TODO.md).

[loki5512344]: https://github.com/loki5512344
