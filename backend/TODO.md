# LoVisual Backend (Rust + axum)

## Правила (ОБЯЗАТЕЛЬНЫЕ, нарушение = откат)
- **KISS / DRY / SOLID** — простая структура, без оверинжиниринга
- **Макс. 200 строк на файл** — превышение = разбить на модули
- **Макс. 3 файла на папку** — больше = новая подпапка по смыслу
- **Обязательное прохождение:** `cargo fmt` + `cargo clippy --all-targets` (ноль ошибок) + `cargo test`
- **Запрещено `#[allow(dead_code)]`** — мёртвый код удаляется, не прячется
- **Запрещено `unsafe`** (в собственной логике; lints: `unsafe_code = "forbid"`)
- **Без пустышек** — модуль создаётся только с рабочей реализацией
- **Каждый этап компилируется** — перед переходом к следующему
- **Ошибки JSON-декода не игнорировать** — всегда `.context()`/`?`
- **`user_id` из токена, не из тела запроса**

---

## Сделано
- [x] Каркас: main.rs + server/{mod,config} + routes/{mod,health}
- [x] `GET /health` + graceful shutdown (SIGTERM/SIGINT)
- [x] Config из env (LOVISUAL_HOST/PORT/CORS_ORIGINS), валидация на старте
- [x] CORS (настраиваемые origins) + tower TraceLayer
- [x] Rust 2024 edition, актуальные версии пакетов

---

## Фаза 1: DB (SQLite, sqlx)
- [ ] `src/db/mod.rs` — подключение, миграции
- [ ] `src/db/users.rs` — таблица users
- [ ] `src/db/configs.rs` — облачные конфиги
- [ ] `src/db/profiles.rs` — профили, playtime, friends

## Фаза 2: Auth
- [ ] `src/auth/mod.rs` — argon2 + JWT (jsonwebtoken)
- [ ] `src/auth/handlers.rs` — register/login/logout
- [ ] `src/auth/middleware.rs` — Bearer token → 401, не panic

## Фаза 3: Profile API
- [ ] `src/profile/mod.rs` — get/update/playtime/search/friends

## Фаза 4: Configs API
- [ ] `src/config_api/mod.rs` — upload/download
- [ ] 16-byte share keys (минимум)

## Фаза 5: IRC
- [ ] `src/irc/mod.rs` — WebSocket хаб (axum + tokio-tungstenite)

## Фаза 6: Middleware
- [ ] RateLimit — с cleanup-задачей (без утечки)
- [ ] Request logging через tracing

### Проверка после каждой фазы: `cargo fmt && cargo clippy --all-targets && cargo test`
