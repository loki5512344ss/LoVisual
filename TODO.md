# LoVisual — структура проекта

Репозиторий разбит на 3 независимых компонента. **У каждого свой TODO, корневой TODO больше не ведётся.**

| Компонент | Стек | TODO |
|-----------|------|------|
| `combatant-client-26.2/` | Java 25, Fabric (MC 26.2) | [`combatant-client-26.2/TODO.md`](combatant-client-26.2/TODO.md) |
| `backend/` | Rust 2024 + axum | [`backend/TODO.md`](backend/TODO.md) |
| `launcher/` | Rust 2024 + Slint | [`launcher/TODO.md`](launcher/TODO.md) |

---

## Общие правила для Rust-компонентов (backend + launcher)
- KISS / DRY / SOLID
- Макс. 200 строк на файл
- Макс. 3 файла на папку
- Обязательно: `cargo fmt` + `cargo clippy --all-targets` + `cargo test`
- Запрещено `#[allow(dead_code)]`
- Запрещено `unsafe` в собственной логике
- Без пустышек: модуль создаётся только с рабочей реализацией
- Каждый этап компилируется

---

## Статус

### Backend
- [x] Каркас: config, /health, graceful shutdown, CORS
- [ ] Auth (argon2 + JWT), профили, конфиги, IRC — см. `backend/TODO.md`

### Launcher
- [x] Каркас: окно логина, фоновая проверка /health
- [ ] Auth, dashboard, settings, launch — см. `launcher/TODO.md`

### Мод (combatant-client-26.2)
- [ ] Полный rewrite: ребрендинг (1464 `combatant`), разбивка 12 файлов >1500 строк, выкинуть Runtime/JarReplacement, тесты — см. `combatant-client-26.2/TODO.md`
