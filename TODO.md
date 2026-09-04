# LoVisual

Проект состоит из одного компонента — мода.

| Компонент | Стек | TODO |
|-----------|------|------|
| `combatant-client-26.2/` | Java 25, Fabric (MC 26.2) | [`combatant-client-26.2/TODO.md`](combatant-client-26.2/TODO.md) |

> Лаунчер (Rust + Slint) и backend (Rust + axum) удалены из проекта — развитие только мода.

---

## Правила (для мода, продублированы в его TODO)
- KISS / DRY / SOLID
- Макс. 200 строк на файл (фасады-делегаты исключение)
- Макс. 3 файла на папку
- Никакого статика, никаких this-escape, никаких мёртвых миксинов
- Без пустышек; каждый этап компилируется (`./gradlew build`)
- Тесты обязательны для новых модулей (JUnit)

---

## Статус
- [x] Фаза 1: Ребрендинг `combatant` → `lovisual`
- [x] Фаза 2: Выкинуть Runtime/JarReplacement + Panic
- [x] Фаза 3: Разбивка 12 файлов-гигантов
- [x] Фаза 3.5: Удаление читерских модулей (легит-направление)
- [x] Фаза 4: Security (BackdoorProtection, StaffTracker)
- [x] Фаза 5: Нормализация модулей (ленивый Minecraft, GameClock)
- [ ] Фаза 6: Тесты (EventBus, конфиг, утилиты) — см. `combatant-client-26.2/TODO.md`
