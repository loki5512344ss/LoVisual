## Фаза 11: Серверный плагин — сервер может отключать функции мода (дизайн 2026-09-30)

> Идея владельца 2026-09-29. Решения владельца 2026-09-30: **Velocity (прокси)** —
> целевой софт v1; **политика — локальный YAML плагина** (наш бэкенд не участвует);
> **дизайн → сразу код**. Статус: дизайн написан, код — в этом же заходе.

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
не входит). Velocity API 4.2.0 (release, papermc-repo), Java 21; gson + snakeyaml
shaded в jar; JUnit 5 для тестов логики.

- `PolicyConfig` — YAML из `plugins/lovisual-policy/config.yml`:
```yaml
enabled: true
mode: lock              # lock | suggest
reason: "Правила сервера"
deny: [esp, freecam, hitboxes, fullbright]
```
  при первом запуске пишется дефолтный; `/lvpolicy reload` — перечитать,
  `/lvpolicy status` — режим/счётчик/версию конфига.
- `PolicyMessage` — кодирование policy-JSON и разбор `request` (gson).
- `LoVisualPolicyPlugin` — тонкий вiring: `ProxyInitializeEvent` (регистрация канала),
  `PostLoginEvent`/`ServerConnectedEvent` (push), `PluginMessageEvent` (ответ на
  request), команда `/lvpolicy`. Вся логика — в `PolicyConfig`/`PolicyMessage`
  (тестируются без Velocity-сервера).
- Если конфиг `enabled: false` — плагин молчит (канал зарегистрирован, сообщений нет).

### Клиент в моде (`features/platform/serverpolicy/`)

- `ServerPolicy` — record: `mode` (`LOCK`/`SUGGEST`), `deny` (set), `reason`, `server`;
- `ServerPolicyCodec` — Gson parse/encode (тот же gson, что у `PlatformHttpClient`);
- `ServerPolicyState` — синглтон-состояние: текущая политика, `allows(moduleId)`,
  `isLocked()`, `clear()` при дисконнекте; при получении — принудительное выключение
  запрещённых включённых модулей (source INTERNAL) + одно chat-уведомление;
- `ServerPolicyPayload` + регистрация через Fabric networking
  (`PayloadTypeRegistry` playS2C/playC2S) — S2C receiver пишет в State и отвечает
  ничего (push-модель), C2S `request` шлётся клиентом при входе; авторегистрация
  `minecraft:register` — на стороне Fabric API;
- применимый шлюз один: `ModuleLifecycleHelper.setEnabled` — при `state==true` и
  `!ServerPolicyState.allows(id)` → отказ (+ chat-сообщение в `lock`, просто
  предупреждение в `suggest`); то же правило в `loadAndApply` (загрузка конфига не
  включает запрещённое);
- ClickGui: на строке запрещённого модуля — замок и подпись reason (по образцу
  setting-level unavailable);
- `%policy` — команда (`features/command/impl/platform/PolicyCommand`): текущий режим,
  список запрещённых, источник (сервер), время получения.

### Честные ограничения (записаны заранее)

Применение политики — на клиенте: читер может её игнорировать. Это не античит —
цель: правила сервера и удобство честных игроков. Настоящая защита — серверный
античит, он отдельно. Подсистема 2 не конфликтует: там realtime-слой (RPC/друзья),
здесь — один payload при входе, каналы разные.

### Тесты и приёмка

- плагин: `PolicyConfigTest` (парс YAML: режимы, дефолты, кривой mode → исключение),
  `PolicyMessageTest` (encode policy / decode request / экранирование reason);
- мод: `ServerPolicyCodecTest` (round-trip, мусорный JSON → пустая политика),
  `ServerPolicyStateTest` (allows/lock/suggest, force-disable, clear на дисконнекте);
- `./gradlew build` + `test` в обоих проектах; ручная приёмка на стенде владельца:
  поднять Velocity с плагином → зайти клиентом → `%policy` показывает политику →
  запрещённый модуль не включается (lock).

### Риски / известное

- версия протокола клиента (MC 26.2) должна поддерживаться версией прокси —
  проверяется на стенде владельца (плагин от версии протокола не зависит);
- push S→C может не дойти, пока клиент не зарегистрировал канал — поэтому есть
  C→S `request` и повторный push при `ServerConnectedEvent`;
- пер-server политика (у каждого backend свой набор) — backlog: формат поля `server`
  для неё зарезервирован.
