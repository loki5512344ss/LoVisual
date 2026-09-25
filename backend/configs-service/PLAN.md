# Backend: configs-service (Подсистема 1, часть 3) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Prerequisite:** `backend/gateway/PLAN.md` fully done (this service relies on `common::internal`, the gateway routing `/configs*` and `/showcase*` here, and the accounts gRPC server).

**Goal:** Cloud configs for the mod and site: 4 named slots per account, a permanent regenerable share code per slot (`%config load <code>`), and a public showcase with "copy to my slot".

**Architecture:** Axum service with its own `configs_db` (same Postgres instance). Sits behind the gateway: every request needs the internal key, identity comes from `GatewayIdentity`. Account ids are plain UUIDs here (no FK — they live in `accounts_db`). Author nicks/avatars for the showcase come from accounts-service over gRPC (`GetPublicProfiles`), batched per page, and degrade to `author: null` if accounts is down.

**Tech Stack:** same versions as accounts-service / gateway (Axum 0.8, sqlx 0.9, tonic 0.14, rand 0.10, serde_json). No new crates.

## Global Constraints

- Everything in `backend/PLAN.md` and `backend/gateway/PLAN.md` Global Constraints applies (TDD, ≤250 lines/file, ≤4 files/folder, no unwrap on request data, `CARGO_BUILD_JOBS=4`, explicit `git add` paths, English commits, never touch `mod/`).
- Test DB: `CONFIGS_DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/configs_db` (created on the test VDS on 2026-09-24).
- Slots are exactly 1..=4. Config name: 1..=32 chars after trim. Config `data`: any JSON value, serialized ≤ 256 KiB. Showcase title 1..=64 chars, description ≤ 500 chars. Page size 20.
- Share code: 8 chars from `23456789ABCDEFGHJKMNPQRSTUVWXYZ` (no 0/O/1/I/L — typed by hand in chat). Lookups trim + uppercase. Code is permanent until the owner regenerates it.
- `error.rs` is a per-service copy of accounts-service's `AppError` shape (`{"error": msg}` JSON). Deliberately not in `common`: the `From<sqlx::Error>` mapping is service-specific (constraint names), and a shared error type would drag `sqlx` into the gateway.

---

## File Structure

```
backend/
  common/proto/accounts.proto          # + GetPublicProfiles
  accounts-service/src/grpc/mod.rs     # + get_public_profiles
  accounts-service/src/accounts/repo.rs# + public_profiles
  configs-service/
    Cargo.toml
    PLAN.md
    migrations/0001_init.sql
    src/
      main.rs
      lib.rs                 # build_app(pool, &Config, Arc<dyn ProfileSource>) -> Router
      config.rs
      error.rs
      slots/{mod,repo,handlers}.rs       # GET/PUT /configs, GET /configs/{slot}
      sharing/{mod,codes,handlers}.rs    # share codes, regenerate, GET /configs/shared/{code}
      showcase/{mod,repo,handlers,profiles}.rs  # publish/unpublish, listing, copy; gRPC author lookup
    tests/
      common/mod.rs
      slots.rs
      sharing.rs
      showcase.rs
```

---

### Task 1: Scaffold — crate, config, schema, health

**Files:**
- Create: `backend/configs-service/{Cargo.toml, migrations/0001_init.sql, src/main.rs, src/lib.rs, src/config.rs, src/error.rs, tests/common/mod.rs, tests/slots.rs}`
- Modify: `backend/Cargo.toml` (members += "configs-service"), `backend/.env.example`

**Interfaces:**
- Produces: `Config { database_url, port /*8082*/, internal_key, accounts_grpc_url }`, `from_env()`, `validate()` (internal key ≥32 bytes). `AppError { Validation, NotFound, Conflict, Internal }`. `build_app(pool, &Config) -> Router` (Task 5 adds the `profiles` arg). Tests: `test_pool()`, `test_config()`, `test_server(Router)`, `ACCOUNT_ID_HEADER` re-export, `new_account() -> Uuid`.

- [ ] **Step 1: `Cargo.toml`**

```toml
[package]
name = "configs-service"
version = "0.1.0"
edition = "2024"

[lib]
name = "configs_service"
path = "src/lib.rs"

[dependencies]
common = { path = "../common" }
axum = "0.8"
tokio = { version = "1", features = ["rt-multi-thread", "macros"] }
tracing = "0.1"
tracing-subscriber = "0.3"
serde = { version = "1", features = ["derive"] }
serde_json = "1"
sqlx = { version = "0.9", default-features = false, features = ["runtime-tokio", "tls-rustls", "postgres", "uuid", "chrono", "json", "macros", "migrate"] }
uuid = { version = "1", features = ["v4", "serde"] }
chrono = { version = "0.4", features = ["serde"] }
rand = "0.10"
tonic = "0.14"
anyhow = "1"
dotenvy = "0.15"

[dev-dependencies]
axum-test = "21"
```

- [ ] **Step 2: Migration `0001_init.sql`**

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- account_id has no FK: accounts live in accounts_db (separate database).
CREATE TABLE config_slots (
    account_id UUID NOT NULL,
    slot_index SMALLINT NOT NULL CHECK (slot_index BETWEEN 1 AND 4),
    name TEXT NOT NULL,
    data JSONB NOT NULL,
    share_code TEXT NOT NULL UNIQUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, slot_index)
);

CREATE TABLE showcase_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    slot_index SMALLINT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    copies_count INTEGER NOT NULL DEFAULT 0,
    -- set by admin moderation (Подсистема 3); public queries always filter it
    hidden BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (account_id, slot_index),
    FOREIGN KEY (account_id, slot_index)
        REFERENCES config_slots (account_id, slot_index) ON DELETE CASCADE
);

CREATE INDEX idx_showcase_new ON showcase_listings (published_at DESC) WHERE NOT hidden;
CREATE INDEX idx_showcase_popular ON showcase_listings (copies_count DESC, published_at DESC) WHERE NOT hidden;
```

- [ ] **Step 3: `config.rs` (tests first: valid passes, short internal key rejected)**

```rust
use anyhow::{Context, Result};

#[derive(Clone)]
pub struct Config {
    pub database_url: String,
    pub port: u16,
    pub internal_key: String,
    pub accounts_grpc_url: String,
}

impl Config {
    pub fn from_env() -> Result<Config> {
        Ok(Config {
            database_url: std::env::var("CONFIGS_DATABASE_URL").context("CONFIGS_DATABASE_URL not set")?,
            port: std::env::var("CONFIGS_PORT").unwrap_or_else(|_| "8082".into()).parse().context("CONFIGS_PORT")?,
            internal_key: std::env::var("INTERNAL_KEY").context("INTERNAL_KEY not set")?,
            accounts_grpc_url: std::env::var("ACCOUNTS_GRPC_URL").context("ACCOUNTS_GRPC_URL not set")?,
        })
    }

    pub fn validate(&self) -> Result<()> {
        if self.internal_key.len() < 32 {
            anyhow::bail!("INTERNAL_KEY must be at least 32 bytes");
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn sample(key: &str) -> Config {
        Config { database_url: String::new(), port: 0, internal_key: key.into(), accounts_grpc_url: String::new() }
    }

    #[test]
    fn strong_key_passes() {
        assert!(sample(&"k".repeat(32)).validate().is_ok());
    }

    #[test]
    fn short_key_is_rejected() {
        assert!(sample("short").validate().is_err());
    }
}
```

- [ ] **Step 4: `error.rs`**

```rust
use axum::{Json, http::StatusCode, response::{IntoResponse, Response}};
use serde_json::json;

#[derive(Debug)]
pub enum AppError {
    Validation(String),
    NotFound(String),
    Conflict(String),
    Internal(anyhow::Error),
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, message) = match self {
            AppError::Validation(m) => (StatusCode::BAD_REQUEST, m),
            AppError::NotFound(m) => (StatusCode::NOT_FOUND, m),
            AppError::Conflict(m) => (StatusCode::CONFLICT, m),
            AppError::Internal(err) => {
                tracing::error!("internal error: {err:?}");
                (StatusCode::INTERNAL_SERVER_ERROR, "internal error".into())
            }
        };
        (status, Json(json!({ "error": message }))).into_response()
    }
}

impl From<sqlx::Error> for AppError {
    fn from(err: sqlx::Error) -> Self {
        AppError::Internal(err.into())
    }
}
```

- [ ] **Step 5: `lib.rs`, `main.rs`**

```rust
// lib.rs
pub mod config;
pub mod error;

use axum::{Router, routing::get};
use common::internal::{InternalKey, require_internal_key};
use config::Config;

pub fn build_app(pool: sqlx::PgPool, cfg: &Config) -> Router {
    let _ = pool; // used from Task 2 on
    let api = Router::new()
        .layer(axum::middleware::from_fn_with_state(InternalKey::new(cfg.internal_key.clone()), require_internal_key));
    Router::new().route("/health", get(|| async { "ok" })).merge(api)
}
```
```rust
// main.rs
#[tokio::main]
async fn main() -> anyhow::Result<()> {
    dotenvy::dotenv().ok();
    tracing_subscriber::fmt::init();
    let cfg = configs_service::config::Config::from_env()?;
    cfg.validate()?;
    let pool = sqlx::PgPool::connect(&cfg.database_url).await?;
    sqlx::migrate!("./migrations").run(&pool).await?;
    let app = configs_service::build_app(pool, &cfg);
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", cfg.port)).await?;
    tracing::info!("configs-service listening on {}", cfg.port);
    axum::serve(listener, app).await?;
    Ok(())
}
```
The `let _ = pool;` line disappears in Task 2 — it is not a placeholder, just keeps Task 1 warning-free.

- [ ] **Step 6: Test helpers + smoke test**

`tests/common/mod.rs`:
```rust
#![allow(dead_code)]

use axum_test::TestServer;
use configs_service::config::Config;
use uuid::Uuid;

pub use ::common::internal::ACCOUNT_ID_HEADER;

pub const TEST_INTERNAL_KEY: &str = "internal-key-internal-key-internal!!";

pub async fn test_pool() -> sqlx::PgPool {
    let url = std::env::var("CONFIGS_DATABASE_URL")
        .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/configs_db".into());
    let pool = sqlx::PgPool::connect(&url).await.expect("connect to configs_db");
    sqlx::migrate!("./migrations").run(&pool).await.expect("run migrations");
    pool
}

pub fn test_config() -> Config {
    Config {
        database_url: String::new(),
        port: 0,
        internal_key: TEST_INTERNAL_KEY.into(),
        accounts_grpc_url: String::new(),
    }
}

pub fn test_server(app: axum::Router) -> TestServer {
    let mut server = TestServer::new(app);
    server.add_header(::common::internal::INTERNAL_KEY_HEADER, TEST_INTERNAL_KEY);
    server
}

/// Accounts live in another service; here an account is just a fresh UUID.
pub fn new_account() -> Uuid {
    Uuid::new_v4()
}
```
`tests/slots.rs`:
```rust
mod common;

#[tokio::test]
async fn health_ok_and_api_requires_internal_key() {
    let pool = common::test_pool().await;
    let raw = axum_test::TestServer::new(configs_service::build_app(pool, &common::test_config()));
    raw.get("/health").await.assert_status_ok();
}
```
Task 2 Step 1 adds `raw.get("/configs").await.assert_status(axum::http::StatusCode::FORBIDDEN);` to this test (there is no `/configs` route to guard until then).

`.env.example`:
```
# configs-service
CONFIGS_DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/configs_db
CONFIGS_PORT=8082
```

- [ ] **Step 7: Run, commit**

Run: `cd backend && CONFIGS_DATABASE_URL=... CARGO_BUILD_JOBS=4 cargo test -p configs-service`
```bash
git add -A backend/Cargo.toml backend/Cargo.lock backend/configs-service backend/.env.example
git commit -m "feat(configs): scaffold service with schema, config validation and health check"
```

---

### Task 2: Slots — list, get, save

**Files:**
- Create: `src/slots/{mod,repo,handlers}.rs`, `src/sharing/{mod,codes}.rs` (codes only — handlers in Task 3)
- Modify: `src/lib.rs`, `tests/slots.rs`

**Interfaces:**
- Produces (`sharing::codes`): `ALPHABET: &[u8]`, `CODE_LEN = 8`, `new_code() -> String`, `normalize(&str) -> String`.
- Produces (`slots::repo`): `SlotSummary { slot: i16, name: String, updated_at, share_code: String, published: bool }`, `Slot { slot, name, data: serde_json::Value, updated_at, share_code }`, `list(&PgPool, Uuid) -> Vec<SlotSummary>`, `get(&PgPool, Uuid, i16) -> Option<Slot>`, `save(&PgPool, Uuid, i16, &str, &serde_json::Value) -> Slot` (upsert; share code created once, retried on the rare unique collision).
- Produces (`slots::handlers`): `validate_slot(i16) -> Result<i16, AppError>`, `validate_name(&str) -> Result<String, AppError>`, `MAX_DATA_BYTES = 256 * 1024`; `GET /configs`, `GET /configs/{slot}`, `PUT /configs/{slot}` `{name, data}` → 200 `Slot`.
- `SlotsState { pool }` (reused by sharing/showcase via their own states).

- [ ] **Step 1: Failing tests**

`src/sharing/codes.rs` tests:
```rust
#[test]
fn codes_use_only_the_unambiguous_alphabet() {
    for _ in 0..200 {
        let c = new_code();
        assert_eq!(c.len(), CODE_LEN);
        assert!(c.bytes().all(|b| ALPHABET.contains(&b)), "{c}");
    }
}

#[test]
fn normalize_trims_and_uppercases() {
    assert_eq!(normalize("  ab3k9mzq \n"), "AB3K9MZQ");
}
```
`tests/slots.rs` (also add the FORBIDDEN assertion to `health_ok_and_api_requires_internal_key`):
```rust
use axum::http::StatusCode;
use serde_json::json;

async fn server() -> axum_test::TestServer {
    common::test_server(configs_service::build_app(common::test_pool().await, &common::test_config()))
}

#[tokio::test]
async fn save_then_get_then_list() {
    let s = server().await;
    let me = common::new_account();
    let data = json!({ "modules": { "Hud": { "enabled": true } } });

    let saved = s.put("/configs/2").add_header(common::ACCOUNT_ID_HEADER, me.to_string())
        .json(&json!({ "name": "  pvp  ", "data": data })).await;
    saved.assert_status_ok();
    let saved: serde_json::Value = saved.json();
    assert_eq!(saved["name"], "pvp");
    assert_eq!(saved["share_code"].as_str().unwrap().len(), 8);

    let got: serde_json::Value = s.get("/configs/2").add_header(common::ACCOUNT_ID_HEADER, me.to_string()).await.json();
    assert_eq!(got["data"], data);

    let list: Vec<serde_json::Value> = s.get("/configs").add_header(common::ACCOUNT_ID_HEADER, me.to_string()).await.json();
    assert_eq!(list.len(), 1);
    assert_eq!(list[0]["slot"], 2);
    assert_eq!(list[0]["published"], false);
    assert!(list[0].get("data").is_none(), "list must not ship full configs");
}

#[tokio::test]
async fn resave_keeps_share_code_and_overwrites_data() {
    let s = server().await;
    let me = common::new_account().to_string();
    let first: serde_json::Value = s.put("/configs/1").add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "a", "data": 1 })).await.json();
    let second: serde_json::Value = s.put("/configs/1").add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "b", "data": 2 })).await.json();
    assert_eq!(first["share_code"], second["share_code"]);
    assert_eq!(second["data"], 2);
}

#[tokio::test]
async fn validation_errors() {
    let s = server().await;
    let me = common::new_account().to_string();
    for slot in ["0", "5"] {
        s.put(&format!("/configs/{slot}")).add_header(common::ACCOUNT_ID_HEADER, &me)
            .json(&json!({ "name": "x", "data": {} })).await.assert_status(StatusCode::BAD_REQUEST);
    }
    s.put("/configs/1").add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "   ", "data": {} })).await.assert_status(StatusCode::BAD_REQUEST);
    s.put("/configs/1").add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "x".repeat(33), "data": {} })).await.assert_status(StatusCode::BAD_REQUEST);
    let huge = "x".repeat(256 * 1024 + 1);
    s.put("/configs/1").add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "x", "data": huge })).await.assert_status(StatusCode::BAD_REQUEST);
}

#[tokio::test]
async fn slots_are_private_and_need_identity() {
    let s = server().await;
    let owner = common::new_account().to_string();
    s.put("/configs/3").add_header(common::ACCOUNT_ID_HEADER, &owner)
        .json(&json!({ "name": "x", "data": {} })).await.assert_status_ok();
    s.get("/configs/3").add_header(common::ACCOUNT_ID_HEADER, common::new_account().to_string())
        .await.assert_status(StatusCode::NOT_FOUND);
    s.get("/configs").await.assert_status_unauthorized();
}
```
Run → FAIL.

- [ ] **Step 2: `sharing/codes.rs`**

```rust
use rand::RngExt;

/// No 0/O, 1/I/L — players type these by hand in chat.
pub const ALPHABET: &[u8] = b"23456789ABCDEFGHJKMNPQRSTUVWXYZ";
pub const CODE_LEN: usize = 8;

pub fn new_code() -> String {
    let mut rng = rand::rng();
    (0..CODE_LEN).map(|_| ALPHABET[rng.random_range(0..ALPHABET.len())] as char).collect()
}

pub fn normalize(code: &str) -> String {
    code.trim().to_uppercase()
}
```
`sharing/mod.rs`: `pub mod codes;` (Task 3 adds `pub mod handlers;`).

- [ ] **Step 3: `slots/repo.rs`**

```rust
use crate::sharing::codes::new_code;
use chrono::{DateTime, Utc};
use serde::Serialize;
use sqlx::PgPool;
use uuid::Uuid;

#[derive(Debug, Serialize, sqlx::FromRow)]
pub struct SlotSummary {
    pub slot: i16,
    pub name: String,
    pub updated_at: DateTime<Utc>,
    pub share_code: String,
    pub published: bool,
}

#[derive(Debug, Serialize, sqlx::FromRow)]
pub struct Slot {
    pub slot: i16,
    pub name: String,
    pub data: serde_json::Value,
    pub updated_at: DateTime<Utc>,
    pub share_code: String,
}

const SLOT_COLUMNS: &str = "slot_index AS slot, name, data, updated_at, share_code";

pub async fn list(pool: &PgPool, account_id: Uuid) -> Result<Vec<SlotSummary>, sqlx::Error> {
    sqlx::query_as(
        "SELECT s.slot_index AS slot, s.name, s.updated_at, s.share_code, (l.id IS NOT NULL) AS published
         FROM config_slots s
         LEFT JOIN showcase_listings l ON l.account_id = s.account_id AND l.slot_index = s.slot_index
         WHERE s.account_id = $1 ORDER BY s.slot_index",
    )
    .bind(account_id)
    .fetch_all(pool)
    .await
}

pub async fn get(pool: &PgPool, account_id: Uuid, slot: i16) -> Result<Option<Slot>, sqlx::Error> {
    sqlx::query_as(&format!("SELECT {SLOT_COLUMNS} FROM config_slots WHERE account_id = $1 AND slot_index = $2"))
        .bind(account_id)
        .bind(slot)
        .fetch_optional(pool)
        .await
}

fn is_share_code_collision(err: &sqlx::Error) -> bool {
    matches!(err, sqlx::Error::Database(db) if db.constraint() == Some("config_slots_share_code_key"))
}

pub async fn save(
    pool: &PgPool,
    account_id: Uuid,
    slot: i16,
    name: &str,
    data: &serde_json::Value,
) -> Result<Slot, sqlx::Error> {
    // share_code is only used on INSERT; an update keeps the existing one.
    // 31^8 codes make collisions vanishingly rare, but never fatal.
    let mut attempts = 0;
    loop {
        let result = sqlx::query_as(&format!(
            "INSERT INTO config_slots (account_id, slot_index, name, data, share_code)
             VALUES ($1, $2, $3, $4, $5)
             ON CONFLICT (account_id, slot_index)
             DO UPDATE SET name = EXCLUDED.name, data = EXCLUDED.data, updated_at = now()
             RETURNING {SLOT_COLUMNS}"
        ))
        .bind(account_id)
        .bind(slot)
        .bind(name)
        .bind(data)
        .bind(new_code())
        .fetch_one(pool)
        .await;
        match result {
            Err(err) if is_share_code_collision(&err) && attempts < 3 => attempts += 1,
            other => return other,
        }
    }
}
```
Verify the constraint name after migrating: `psql ... -c '\d config_slots'` → `config_slots_share_code_key` (Postgres default `<table>_<column>_key`).

- [ ] **Step 4: `slots/handlers.rs`**

```rust
use super::repo::{self, Slot, SlotSummary};
use crate::error::AppError;
use axum::{Json, extract::{Path, State}};
use common::internal::GatewayIdentity;
use serde::Deserialize;

pub const MAX_DATA_BYTES: usize = 256 * 1024;
const MAX_NAME_CHARS: usize = 32;

#[derive(Clone)]
pub struct SlotsState {
    pub pool: sqlx::PgPool,
}

pub fn validate_slot(slot: i16) -> Result<i16, AppError> {
    if (1..=4).contains(&slot) { Ok(slot) } else { Err(AppError::Validation("slot must be 1..4".into())) }
}

pub fn validate_name(name: &str) -> Result<String, AppError> {
    let name = name.trim();
    let len = name.chars().count();
    if len == 0 || len > MAX_NAME_CHARS {
        return Err(AppError::Validation(format!("name must be 1..{MAX_NAME_CHARS} characters")));
    }
    Ok(name.to_owned())
}

#[derive(Deserialize)]
pub struct SaveRequest {
    pub name: String,
    pub data: serde_json::Value,
}

pub async fn list(State(state): State<SlotsState>, id: GatewayIdentity) -> Result<Json<Vec<SlotSummary>>, AppError> {
    Ok(Json(repo::list(&state.pool, id.account_id).await?))
}

pub async fn get(
    State(state): State<SlotsState>,
    id: GatewayIdentity,
    Path(slot): Path<i16>,
) -> Result<Json<Slot>, AppError> {
    let slot = validate_slot(slot)?;
    repo::get(&state.pool, id.account_id, slot)
        .await?
        .map(Json)
        .ok_or_else(|| AppError::NotFound("slot is empty".into()))
}

pub async fn save(
    State(state): State<SlotsState>,
    id: GatewayIdentity,
    Path(slot): Path<i16>,
    Json(req): Json<SaveRequest>,
) -> Result<Json<Slot>, AppError> {
    let slot = validate_slot(slot)?;
    let name = validate_name(&req.name)?;
    let size = serde_json::to_vec(&req.data).map_err(|e| AppError::Internal(e.into()))?.len();
    if size > MAX_DATA_BYTES {
        return Err(AppError::Validation(format!("config data must be at most {MAX_DATA_BYTES} bytes")));
    }
    Ok(Json(repo::save(&state.pool, id.account_id, slot, &name, &req.data).await?))
}
```
A non-numeric `{slot}` is rejected by Axum's `Path` extractor with 400 already.

- [ ] **Step 5: Routes (`lib.rs`)**

```rust
pub mod sharing;
pub mod slots;
// ...
let slots_state = slots::handlers::SlotsState { pool: pool.clone() };
let api = Router::new()
    .route("/configs", get(slots::handlers::list))
    .route("/configs/{slot}", get(slots::handlers::get).put(slots::handlers::save))
    .with_state(slots_state)
    .layer(axum::extract::DefaultBodyLimit::max(slots::handlers::MAX_DATA_BYTES + 16 * 1024))
    .layer(axum::middleware::from_fn_with_state(InternalKey::new(cfg.internal_key.clone()), require_internal_key));
```
Note: with the body limit at 272 KiB, the 256 KiB+1 test body still fits the HTTP limit and is rejected by the explicit size check (400), not 413.

- [ ] **Step 6: Tests pass; commit**

```bash
git add -A backend/configs-service
git commit -m "feat(configs): four config slots per account with list, get and save"
```

---

### Task 3: Share codes — regenerate + load by code

**Files:**
- Create: `src/sharing/handlers.rs`, `tests/sharing.rs`
- Modify: `src/sharing/mod.rs`, `src/slots/repo.rs` (+ `by_share_code`, `regenerate_code`), `src/lib.rs`

**Interfaces:**
- Produces: `repo::by_share_code(&PgPool, &str) -> Option<SharedConfig { name, data, updated_at }>`, `repo::regenerate_code(&PgPool, Uuid, i16) -> Option<String>`.
- HTTP: `GET /configs/shared/{code}` (no identity needed) → `{name, data, updated_at}` — never the owner id; `POST /configs/{slot}/regenerate-code` → `{share_code}`; 404 for empty slot / unknown code.

- [ ] **Step 1: Failing tests (`tests/sharing.rs`)**

```rust
mod common;

use axum::http::StatusCode;
use serde_json::json;

async fn server() -> axum_test::TestServer {
    common::test_server(configs_service::build_app(common::test_pool().await, &common::test_config()))
}

async fn save(s: &axum_test::TestServer, owner: &str) -> String {
    let r: serde_json::Value = s.put("/configs/1").add_header(common::ACCOUNT_ID_HEADER, owner)
        .json(&json!({ "name": "legit", "data": { "k": 1 } })).await.json();
    r["share_code"].as_str().unwrap().to_owned()
}

#[tokio::test]
async fn anyone_can_load_by_code_case_insensitively_without_owner_leak() {
    let s = server().await;
    let code = save(&s, &common::new_account().to_string()).await;
    let res = s.get(&format!("/configs/shared/{}", code.to_lowercase())).await;
    res.assert_status_ok();
    let body: serde_json::Value = res.json();
    assert_eq!(body["name"], "legit");
    assert_eq!(body["data"], json!({ "k": 1 }));
    assert!(body.get("account_id").is_none());
}

#[tokio::test]
async fn regenerate_invalidates_the_old_code() {
    let s = server().await;
    let owner = common::new_account().to_string();
    let old = save(&s, &owner).await;
    let res = s.post("/configs/1/regenerate-code").add_header(common::ACCOUNT_ID_HEADER, &owner).await;
    res.assert_status_ok();
    let new = res.json::<serde_json::Value>()["share_code"].as_str().unwrap().to_owned();
    assert_ne!(old, new);
    s.get(&format!("/configs/shared/{old}")).await.assert_status(StatusCode::NOT_FOUND);
    s.get(&format!("/configs/shared/{new}")).await.assert_status_ok();
}

#[tokio::test]
async fn regenerate_on_empty_slot_is_404_and_needs_identity() {
    let s = server().await;
    s.post("/configs/4/regenerate-code").add_header(common::ACCOUNT_ID_HEADER, common::new_account().to_string())
        .await.assert_status(StatusCode::NOT_FOUND);
    s.post("/configs/4/regenerate-code").await.assert_status_unauthorized();
}

#[tokio::test]
async fn unknown_code_is_404() {
    server().await.get("/configs/shared/ZZZZZZZZ").await.assert_status(StatusCode::NOT_FOUND);
}
```

- [ ] **Step 2: Repo additions (`slots/repo.rs`)**

```rust
#[derive(Debug, Serialize, sqlx::FromRow)]
pub struct SharedConfig {
    pub name: String,
    pub data: serde_json::Value,
    pub updated_at: DateTime<Utc>,
}

pub async fn by_share_code(pool: &PgPool, code: &str) -> Result<Option<SharedConfig>, sqlx::Error> {
    sqlx::query_as("SELECT name, data, updated_at FROM config_slots WHERE share_code = $1")
        .bind(code)
        .fetch_optional(pool)
        .await
}

pub async fn regenerate_code(pool: &PgPool, account_id: Uuid, slot: i16) -> Result<Option<String>, sqlx::Error> {
    let mut attempts = 0;
    loop {
        let result = sqlx::query_scalar(
            "UPDATE config_slots SET share_code = $3 WHERE account_id = $1 AND slot_index = $2 RETURNING share_code",
        )
        .bind(account_id)
        .bind(slot)
        .bind(new_code())
        .fetch_optional(pool)
        .await;
        match result {
            Err(err) if is_share_code_collision(&err) && attempts < 3 => attempts += 1,
            other => return other,
        }
    }
}
```
If `repo.rs` passes 250 lines, move the two sharing queries into `sharing/repo.rs`... that would make `sharing/` 4 files (`mod, codes, handlers, repo`) — allowed.

- [ ] **Step 3: `sharing/handlers.rs`**

```rust
use super::codes::normalize;
use crate::error::AppError;
use crate::slots::handlers::{SlotsState, validate_slot};
use crate::slots::repo::{self, SharedConfig};
use axum::{Json, extract::{Path, State}};
use common::internal::GatewayIdentity;
use serde::Serialize;

#[derive(Serialize)]
pub struct ShareCodeResponse {
    pub share_code: String,
}

pub async fn load_shared(State(state): State<SlotsState>, Path(code): Path<String>) -> Result<Json<SharedConfig>, AppError> {
    repo::by_share_code(&state.pool, &normalize(&code))
        .await?
        .map(Json)
        .ok_or_else(|| AppError::NotFound("unknown share code".into()))
}

pub async fn regenerate(
    State(state): State<SlotsState>,
    id: GatewayIdentity,
    Path(slot): Path<i16>,
) -> Result<Json<ShareCodeResponse>, AppError> {
    let slot = validate_slot(slot)?;
    repo::regenerate_code(&state.pool, id.account_id, slot)
        .await?
        .map(|share_code| Json(ShareCodeResponse { share_code }))
        .ok_or_else(|| AppError::NotFound("slot is empty".into()))
}
```
Routes: `.route("/configs/shared/{code}", get(sharing::handlers::load_shared))` and `.route("/configs/{slot}/regenerate-code", post(sharing::handlers::regenerate))`. Axum 0.8 prefers the static segment `shared` over `{slot}`, and `/configs/{slot}` has no second segment, so no conflict.

- [ ] **Step 4: Tests pass; commit**

```bash
git add -A backend/configs-service
git commit -m "feat(configs): permanent share codes with regenerate and public load-by-code"
```

---

### Task 4: Author profiles over gRPC (accounts-service side)

**Files:**
- Modify: `backend/common/proto/accounts.proto`, `backend/accounts-service/src/grpc/mod.rs`, `backend/accounts-service/src/accounts/repo.rs`, `backend/accounts-service/src/main.rs`

**Interfaces:**
- Produces (proto): `rpc GetPublicProfiles(GetPublicProfilesRequest) returns (GetPublicProfilesReply)`; `GetPublicProfilesRequest { repeated string account_ids = 1; }`; `PublicProfile { string account_id = 1; string display_nick = 2; string avatar_url = 3; /* "" = none */ }`; `GetPublicProfilesReply { repeated PublicProfile profiles = 1; }`. Max 100 ids per call (`INVALID_ARGUMENT` above that). Unknown/invalid ids are skipped silently.
- Produces: `accounts::repo::public_profiles(&PgPool, &[Uuid]) -> Result<Vec<(Uuid, String, Option<String>)>, sqlx::Error>`; `grpc::AccountsGrpc::new(pool, avatar_base_url: String)`; `grpc::server(pool, avatar_base_url, internal_key)`.

- [ ] **Step 1: Proto**

Append to the service and messages:
```proto
  // Nick + avatar for showcase authors etc. Never returns email or role.
  rpc GetPublicProfiles(GetPublicProfilesRequest) returns (GetPublicProfilesReply);
```
```proto
message GetPublicProfilesRequest { repeated string account_ids = 1; }
message PublicProfile {
  string account_id = 1;
  string display_nick = 2;
  string avatar_url = 3; // empty string when the account has no avatar
}
message GetPublicProfilesReply { repeated PublicProfile profiles = 1; }
```

- [ ] **Step 2: Failing test — extend `authenticate_device_over_grpc`'s module with**

```rust
#[tokio::test]
async fn public_profiles_over_grpc() {
    let pool = pool().await;
    let a = crate::accounts::repo::create(&pool, &format!("p-{}@example.com", Uuid::new_v4()), "x", "Alice").await.unwrap();
    let svc = AccountsGrpc::new(pool.clone(), "http://cdn/avatars".into());

    let reply = svc.get_public_profiles(tonic::Request::new(GetPublicProfilesRequest {
        account_ids: vec![a.id.to_string(), Uuid::new_v4().to_string(), "garbage".into()],
    })).await.unwrap().into_inner();
    assert_eq!(reply.profiles.len(), 1);
    assert_eq!(reply.profiles[0].display_nick, "Alice");
    assert_eq!(reply.profiles[0].avatar_url, "");

    let too_many = (0..101).map(|_| Uuid::new_v4().to_string()).collect();
    let err = svc.get_public_profiles(tonic::Request::new(GetPublicProfilesRequest { account_ids: too_many }))
        .await.unwrap_err();
    assert_eq!(err.code(), tonic::Code::InvalidArgument);

    sqlx::query("DELETE FROM accounts WHERE id = $1").bind(a.id).execute(&pool).await.unwrap();
}
```
(Calling the trait method directly — no network needed for this one.) Update the existing test's `server(pool.clone(), KEY)` call to `server(pool.clone(), "http://cdn/avatars".into(), KEY)`.

- [ ] **Step 3: Repo + service**

`accounts/repo.rs`:
```rust
pub async fn public_profiles(pool: &PgPool, ids: &[Uuid]) -> Result<Vec<(Uuid, String, Option<String>)>, sqlx::Error> {
    sqlx::query_as(
        "SELECT a.id, a.display_nick, av.s3_key FROM accounts a
         LEFT JOIN avatars av ON av.account_id = a.id
         WHERE a.id = ANY($1)",
    )
    .bind(ids)
    .fetch_all(pool)
    .await
}
```
`grpc/mod.rs`: `AccountsGrpc { pool, avatar_base_url: String }`, `server(pool, avatar_base_url: String, internal_key: &str)`, and:
```rust
const MAX_PROFILE_IDS: usize = 100;

async fn get_public_profiles(
    &self,
    request: Request<GetPublicProfilesRequest>,
) -> Result<Response<GetPublicProfilesReply>, Status> {
    let raw = request.into_inner().account_ids;
    if raw.len() > MAX_PROFILE_IDS {
        return Err(Status::invalid_argument("at most 100 account ids per call"));
    }
    let ids: Vec<Uuid> = raw.iter().filter_map(|s| Uuid::parse_str(s).ok()).collect();
    let rows = crate::accounts::repo::public_profiles(&self.pool, &ids).await.map_err(|err| {
        tracing::error!("get_public_profiles: {err:?}");
        Status::internal("internal error")
    })?;
    let profiles = rows
        .into_iter()
        .map(|(id, nick, key)| PublicProfile {
            account_id: id.to_string(),
            display_nick: nick,
            avatar_url: key.map(|k| format!("{}/{k}", self.avatar_base_url)).unwrap_or_default(),
        })
        .collect();
    Ok(Response::new(GetPublicProfilesReply { profiles }))
}
```
`main.rs`: `accounts_service::grpc::server(pool.clone(), cfg.avatar_base_url(), &cfg.internal_key)`.

- [ ] **Step 4: `cargo test -p common -p accounts-service` passes; commit**

```bash
git add -A backend/common backend/accounts-service
git commit -m "feat(accounts): GetPublicProfiles gRPC for showcase author info"
```

---

### Task 5: Showcase — publish, browse, copy

**Files:**
- Create: `src/showcase/{mod,repo,handlers,profiles}.rs`, `tests/showcase.rs`
- Modify: `src/lib.rs` (`build_app(pool, &cfg, profiles: Arc<dyn ProfileSource>)`), `src/main.rs`, `tests/slots.rs` + `tests/sharing.rs` (pass `common::no_profiles()`), `tests/common/mod.rs`

**Interfaces:**
- Produces (`showcase::profiles`): `Author { nick: String, avatar_url: Option<String> }` (Serialize), `trait ProfileSource { fn profiles<'a>(&'a self, ids: &'a [Uuid]) -> ProfilesFuture<'a>; }` with `ProfilesFuture<'a> = Pin<Box<dyn Future<Output = HashMap<Uuid, Author>> + Send + 'a>>`, `GrpcProfiles::connect_lazy(url, internal_key) -> anyhow::Result<Self>` (failures → empty map, logged).
- Produces (`showcase::repo`): `ListingRow { id, account_id, slot, title, description, copies_count, published_at, name }`, `publish(&PgPool, Uuid, i16, &str, &str) -> Option<Uuid>` (None = slot empty), `unpublish(&PgPool, Uuid, i16) -> bool`, `page(&PgPool, Sort, page: i64) -> Vec<ListingRow>` (fetches 21 to compute `has_more`), `detail(&PgPool, Uuid) -> Option<(ListingRow, serde_json::Value)>`, `copy_into(&PgPool, listing: Uuid, target_account: Uuid, target_slot: i16) -> CopyOutcome { Copied, ListingMissing, SlotOccupied }`.
- HTTP:
  - `POST /configs/{slot}/publish` `{title, description?}` → 200 `{listing_id}` (re-publishing updates title/description); 404 empty slot.
  - `DELETE /configs/{slot}/publish` → 204 / 404.
  - `GET /showcase?sort=new|popular&page=0` → `{items: [Listing], page, has_more}`, `Listing = {id, title, description, config_name, copies_count, published_at, author: Author|null}`. Public.
  - `GET /showcase/{id}` → `Listing` + `data`. Public. Hidden listings are 404.
  - `POST /showcase/{id}/copy` `{slot}` (identity) → 201 `Slot`; 409 if the target slot is occupied (spec: "копия в свободный слот"); 404 unknown listing.

- [ ] **Step 1: `profiles.rs`**

```rust
use common::internal::GrpcKeyAttach;
use common::pb::accounts::{GetPublicProfilesRequest, accounts_internal_client::AccountsInternalClient};
use serde::Serialize;
use std::{collections::HashMap, future::Future, pin::Pin};
use tonic::{service::interceptor::InterceptedService, transport::{Channel, Endpoint}};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize)]
pub struct Author {
    pub nick: String,
    pub avatar_url: Option<String>,
}

pub type ProfilesFuture<'a> = Pin<Box<dyn Future<Output = HashMap<Uuid, Author>> + Send + 'a>>;

pub trait ProfileSource: Send + Sync + 'static {
    fn profiles<'a>(&'a self, ids: &'a [Uuid]) -> ProfilesFuture<'a>;
}

pub struct GrpcProfiles {
    client: AccountsInternalClient<InterceptedService<Channel, GrpcKeyAttach>>,
}

impl GrpcProfiles {
    pub fn connect_lazy(url: &str, internal_key: &str) -> anyhow::Result<Self> {
        let channel = Endpoint::from_shared(url.to_owned())?
            .timeout(std::time::Duration::from_secs(3))
            .connect_lazy();
        Ok(GrpcProfiles { client: AccountsInternalClient::with_interceptor(channel, GrpcKeyAttach::new(internal_key)?) })
    }
}

impl ProfileSource for GrpcProfiles {
    fn profiles<'a>(&'a self, ids: &'a [Uuid]) -> ProfilesFuture<'a> {
        Box::pin(async move {
            let request = GetPublicProfilesRequest { account_ids: ids.iter().map(Uuid::to_string).collect() };
            match self.client.clone().get_public_profiles(request).await {
                Ok(reply) => reply
                    .into_inner()
                    .profiles
                    .into_iter()
                    .filter_map(|p| {
                        let id = Uuid::parse_str(&p.account_id).ok()?;
                        let avatar_url = (!p.avatar_url.is_empty()).then_some(p.avatar_url);
                        Some((id, Author { nick: p.display_nick, avatar_url }))
                    })
                    .collect(),
                Err(status) => {
                    // Showcase must stay browsable when accounts-service is down.
                    tracing::warn!("GetPublicProfiles failed: {status}");
                    HashMap::new()
                }
            }
        })
    }
}
```
Test helpers (`tests/common/mod.rs`):
```rust
use configs_service::showcase::profiles::{Author, ProfileSource, ProfilesFuture};
use std::{collections::HashMap, sync::Arc};

/// Every account is "Author-<first 4 chars of id>".
pub struct FakeProfiles;

impl ProfileSource for FakeProfiles {
    fn profiles<'a>(&'a self, ids: &'a [Uuid]) -> ProfilesFuture<'a> {
        Box::pin(async move {
            ids.iter()
                .map(|id| (*id, Author { nick: format!("Author-{}", &id.to_string()[..4]), avatar_url: None }))
                .collect::<HashMap<_, _>>()
        })
    }
}

pub fn no_profiles() -> Arc<dyn ProfileSource> {
    Arc::new(FakeProfiles)
}
```

- [ ] **Step 2: Failing tests (`tests/showcase.rs`)**

```rust
mod common;

use axum::http::StatusCode;
use serde_json::json;

async fn server() -> axum_test::TestServer {
    common::test_server(configs_service::build_app(common::test_pool().await, &common::test_config(), common::no_profiles()))
}

async fn publish(s: &axum_test::TestServer, owner: &str, title: &str) -> String {
    s.put("/configs/1").add_header(common::ACCOUNT_ID_HEADER, owner)
        .json(&json!({ "name": "cfg", "data": { "t": title } })).await.assert_status_ok();
    let r = s.post("/configs/1/publish").add_header(common::ACCOUNT_ID_HEADER, owner)
        .json(&json!({ "title": title, "description": "desc" })).await;
    r.assert_status_ok();
    r.json::<serde_json::Value>()["listing_id"].as_str().unwrap().to_owned()
}

#[tokio::test]
async fn published_listing_shows_up_publicly_with_author() {
    let s = server().await;
    let owner = common::new_account();
    let id = publish(&s, &owner.to_string(), "Best PvP").await;

    let page: serde_json::Value = s.get("/showcase?sort=new&page=0").await.json();
    let item = page["items"].as_array().unwrap().iter().find(|i| i["id"] == id).expect("listed");
    assert_eq!(item["title"], "Best PvP");
    assert_eq!(item["config_name"], "cfg");
    assert_eq!(item["author"]["nick"], format!("Author-{}", &owner.to_string()[..4]));
    assert!(item.get("account_id").is_none());

    let detail: serde_json::Value = s.get(&format!("/showcase/{id}")).await.json();
    assert_eq!(detail["data"], json!({ "t": "Best PvP" }));
}

#[tokio::test]
async fn unpublish_removes_listing() {
    let s = server().await;
    let owner = common::new_account().to_string();
    let id = publish(&s, &owner, "gone").await;
    s.delete("/configs/1/publish").add_header(common::ACCOUNT_ID_HEADER, &owner)
        .await.assert_status(StatusCode::NO_CONTENT);
    s.get(&format!("/showcase/{id}")).await.assert_status(StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn copy_goes_into_a_free_slot_and_counts() {
    let s = server().await;
    let id = publish(&s, &common::new_account().to_string(), "copy me").await;
    let me = common::new_account().to_string();

    let r = s.post(&format!("/showcase/{id}/copy")).add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "slot": 3 })).await;
    r.assert_status(StatusCode::CREATED);
    assert_eq!(r.json::<serde_json::Value>()["data"], json!({ "t": "copy me" }));

    s.post(&format!("/showcase/{id}/copy")).add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "slot": 3 })).await.assert_status(StatusCode::CONFLICT);

    let detail: serde_json::Value = s.get(&format!("/showcase/{id}")).await.json();
    assert_eq!(detail["copies_count"], 1);
}

#[tokio::test]
async fn publish_validation_and_auth() {
    let s = server().await;
    let owner = common::new_account().to_string();
    s.post("/configs/2/publish").add_header(common::ACCOUNT_ID_HEADER, &owner)
        .json(&json!({ "title": "x" })).await.assert_status(StatusCode::NOT_FOUND);
    s.put("/configs/2").add_header(common::ACCOUNT_ID_HEADER, &owner)
        .json(&json!({ "name": "n", "data": {} })).await;
    s.post("/configs/2/publish").add_header(common::ACCOUNT_ID_HEADER, &owner)
        .json(&json!({ "title": "  " })).await.assert_status(StatusCode::BAD_REQUEST);
    s.post("/configs/2/publish").json(&json!({ "title": "x" })).await.assert_status_unauthorized();
    s.get("/showcase?sort=bogus").await.assert_status(StatusCode::BAD_REQUEST);
}

#[tokio::test]
async fn popular_sort_orders_by_copies() {
    let s = server().await;
    let low = publish(&s, &common::new_account().to_string(), "low").await;
    let high = publish(&s, &common::new_account().to_string(), "high").await;
    for _ in 0..2 {
        s.post(&format!("/showcase/{high}/copy"))
            .add_header(common::ACCOUNT_ID_HEADER, common::new_account().to_string())
            .json(&json!({ "slot": 1 })).await.assert_status(StatusCode::CREATED);
    }
    let page: serde_json::Value = s.get("/showcase?sort=popular").await.json();
    let ids: Vec<&str> = page["items"].as_array().unwrap().iter().map(|i| i["id"].as_str().unwrap()).collect();
    let (hi, lo) = (ids.iter().position(|i| *i == high), ids.iter().position(|i| *i == low));
    assert!(hi < lo || lo.is_none(), "high-copy listing must come first");
}
```
(The shared test DB accumulates listings across runs, so tests look up their own ids rather than assuming exact page contents.)

- [ ] **Step 3: `showcase/repo.rs`**

```rust
use chrono::{DateTime, Utc};
use sqlx::PgPool;
use uuid::Uuid;

pub const PAGE_SIZE: i64 = 20;

#[derive(Debug, Clone, Copy)]
pub enum Sort {
    New,
    Popular,
}

#[derive(Debug, sqlx::FromRow)]
pub struct ListingRow {
    pub id: Uuid,
    pub account_id: Uuid,
    pub title: String,
    pub description: String,
    pub copies_count: i32,
    pub published_at: DateTime<Utc>,
    pub name: String,
}

const LISTING_SELECT: &str =
    "SELECT l.id, l.account_id, l.title, l.description, l.copies_count, l.published_at, s.name
     FROM showcase_listings l
     JOIN config_slots s ON s.account_id = l.account_id AND s.slot_index = l.slot_index";

pub async fn publish(pool: &PgPool, account_id: Uuid, slot: i16, title: &str, description: &str) -> Result<Option<Uuid>, sqlx::Error> {
    // The FK to config_slots makes this fail for an empty slot; check first for a clean 404.
    let exists: bool = sqlx::query_scalar("SELECT EXISTS(SELECT 1 FROM config_slots WHERE account_id = $1 AND slot_index = $2)")
        .bind(account_id).bind(slot).fetch_one(pool).await?;
    if !exists {
        return Ok(None);
    }
    sqlx::query_scalar(
        "INSERT INTO showcase_listings (account_id, slot_index, title, description) VALUES ($1, $2, $3, $4)
         ON CONFLICT (account_id, slot_index) DO UPDATE SET title = EXCLUDED.title, description = EXCLUDED.description
         RETURNING id",
    )
    .bind(account_id).bind(slot).bind(title).bind(description)
    .fetch_one(pool).await.map(Some)
}

pub async fn unpublish(pool: &PgPool, account_id: Uuid, slot: i16) -> Result<bool, sqlx::Error> {
    let r = sqlx::query("DELETE FROM showcase_listings WHERE account_id = $1 AND slot_index = $2")
        .bind(account_id).bind(slot).execute(pool).await?;
    Ok(r.rows_affected() == 1)
}

/// Returns up to PAGE_SIZE + 1 rows; the caller uses the extra one for `has_more`.
pub async fn page(pool: &PgPool, sort: Sort, page: i64) -> Result<Vec<ListingRow>, sqlx::Error> {
    let order = match sort {
        Sort::New => "l.published_at DESC",
        Sort::Popular => "l.copies_count DESC, l.published_at DESC",
    };
    sqlx::query_as(&format!("{LISTING_SELECT} WHERE NOT l.hidden ORDER BY {order} LIMIT $1 OFFSET $2"))
        .bind(PAGE_SIZE + 1)
        .bind(page * PAGE_SIZE)
        .fetch_all(pool)
        .await
}

pub async fn detail(pool: &PgPool, id: Uuid) -> Result<Option<(ListingRow, serde_json::Value)>, sqlx::Error> {
    let Some(row) = sqlx::query_as::<_, ListingRow>(&format!("{LISTING_SELECT} WHERE l.id = $1 AND NOT l.hidden"))
        .bind(id).fetch_optional(pool).await?
    else {
        return Ok(None);
    };
    let data: serde_json::Value = sqlx::query_scalar(
        "SELECT s.data FROM showcase_listings l JOIN config_slots s
         ON s.account_id = l.account_id AND s.slot_index = l.slot_index WHERE l.id = $1",
    )
    .bind(id).fetch_one(pool).await?;
    Ok(Some((row, data)))
}

pub enum CopyOutcome {
    Copied,
    ListingMissing,
    SlotOccupied,
}

pub async fn copy_into(pool: &PgPool, listing: Uuid, account_id: Uuid, slot: i16) -> Result<CopyOutcome, sqlx::Error> {
    let mut tx = pool.begin().await?;
    let Some((name, data)): Option<(String, serde_json::Value)> = sqlx::query_as(
        "SELECT s.name, s.data FROM showcase_listings l JOIN config_slots s
         ON s.account_id = l.account_id AND s.slot_index = l.slot_index WHERE l.id = $1 AND NOT l.hidden",
    )
    .bind(listing).fetch_optional(&mut *tx).await?
    else {
        return Ok(CopyOutcome::ListingMissing);
    };
    let inserted = sqlx::query(
        "INSERT INTO config_slots (account_id, slot_index, name, data, share_code) VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (account_id, slot_index) DO NOTHING",
    )
    .bind(account_id).bind(slot).bind(&name).bind(&data).bind(crate::sharing::codes::new_code())
    .execute(&mut *tx).await?;
    if inserted.rows_affected() == 0 {
        return Ok(CopyOutcome::SlotOccupied);
    }
    sqlx::query("UPDATE showcase_listings SET copies_count = copies_count + 1 WHERE id = $1")
        .bind(listing).execute(&mut *tx).await?;
    tx.commit().await?;
    Ok(CopyOutcome::Copied)
}
```
(~130 lines. `showcase/` is already at 4 files, so if it ever needs to grow, split a subfolder rather than adding a 5th file.)

- [ ] **Step 4: `showcase/handlers.rs`**

```rust
use super::profiles::{Author, ProfileSource};
use super::repo::{self, CopyOutcome, ListingRow, PAGE_SIZE, Sort};
use crate::error::AppError;
use crate::slots::handlers::validate_slot;
use axum::{Json, extract::{Path, Query, State}, http::StatusCode};
use chrono::{DateTime, Utc};
use common::internal::GatewayIdentity;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use uuid::Uuid;

#[derive(Clone)]
pub struct ShowcaseState {
    pub pool: sqlx::PgPool,
    pub profiles: Arc<dyn ProfileSource>,
}

#[derive(Serialize)]
pub struct Listing {
    pub id: Uuid,
    pub title: String,
    pub description: String,
    pub config_name: String,
    pub copies_count: i32,
    pub published_at: DateTime<Utc>,
    pub author: Option<Author>,
}

fn to_listing(row: ListingRow, author: Option<Author>) -> Listing {
    Listing {
        id: row.id,
        title: row.title,
        description: row.description,
        config_name: row.name,
        copies_count: row.copies_count,
        published_at: row.published_at,
        author,
    }
}

#[derive(Deserialize)]
pub struct PublishRequest {
    pub title: String,
    #[serde(default)]
    pub description: String,
}

pub async fn publish(
    State(state): State<ShowcaseState>,
    id: GatewayIdentity,
    Path(slot): Path<i16>,
    Json(req): Json<PublishRequest>,
) -> Result<Json<serde_json::Value>, AppError> {
    let slot = validate_slot(slot)?;
    let title = req.title.trim();
    if title.is_empty() || title.chars().count() > 64 {
        return Err(AppError::Validation("title must be 1..64 characters".into()));
    }
    if req.description.chars().count() > 500 {
        return Err(AppError::Validation("description must be at most 500 characters".into()));
    }
    let listing = repo::publish(&state.pool, id.account_id, slot, title, req.description.trim())
        .await?
        .ok_or_else(|| AppError::NotFound("slot is empty".into()))?;
    Ok(Json(serde_json::json!({ "listing_id": listing })))
}

pub async fn unpublish(State(state): State<ShowcaseState>, id: GatewayIdentity, Path(slot): Path<i16>) -> Result<StatusCode, AppError> {
    let slot = validate_slot(slot)?;
    if repo::unpublish(&state.pool, id.account_id, slot).await? {
        Ok(StatusCode::NO_CONTENT)
    } else {
        Err(AppError::NotFound("slot is not published".into()))
    }
}

#[derive(Deserialize)]
pub struct PageQuery {
    pub sort: Option<String>,
    pub page: Option<i64>,
}

#[derive(Serialize)]
pub struct PageResponse {
    pub items: Vec<Listing>,
    pub page: i64,
    pub has_more: bool,
}

pub async fn browse(State(state): State<ShowcaseState>, Query(q): Query<PageQuery>) -> Result<Json<PageResponse>, AppError> {
    let sort = match q.sort.as_deref() {
        None | Some("new") => Sort::New,
        Some("popular") => Sort::Popular,
        Some(_) => return Err(AppError::Validation("sort must be new or popular".into())),
    };
    let page = q.page.unwrap_or(0).clamp(0, 10_000);
    let mut rows = repo::page(&state.pool, sort, page).await?;
    let has_more = rows.len() as i64 > PAGE_SIZE;
    rows.truncate(PAGE_SIZE as usize);
    let ids: Vec<Uuid> = rows.iter().map(|r| r.account_id).collect();
    let authors = state.profiles.profiles(&ids).await;
    let items = rows.into_iter().map(|r| { let a = authors.get(&r.account_id).cloned(); to_listing(r, a) }).collect();
    Ok(Json(PageResponse { items, page, has_more }))
}

pub async fn detail(State(state): State<ShowcaseState>, Path(id): Path<Uuid>) -> Result<Json<serde_json::Value>, AppError> {
    let (row, data) = repo::detail(&state.pool, id).await?.ok_or_else(|| AppError::NotFound("no such listing".into()))?;
    let author = state.profiles.profiles(&[row.account_id]).await.remove(&row.account_id);
    let mut body = serde_json::to_value(to_listing(row, author)).map_err(|e| AppError::Internal(e.into()))?;
    body["data"] = data;
    Ok(Json(body))
}

#[derive(Deserialize)]
pub struct CopyRequest {
    pub slot: i16,
}

pub async fn copy(
    State(state): State<ShowcaseState>,
    id: GatewayIdentity,
    Path(listing): Path<Uuid>,
    Json(req): Json<CopyRequest>,
) -> Result<(StatusCode, Json<crate::slots::repo::Slot>), AppError> {
    let slot = validate_slot(req.slot)?;
    match repo::copy_into(&state.pool, listing, id.account_id, slot).await? {
        CopyOutcome::ListingMissing => Err(AppError::NotFound("no such listing".into())),
        CopyOutcome::SlotOccupied => Err(AppError::Conflict("target slot is not empty".into())),
        CopyOutcome::Copied => {
            let saved = crate::slots::repo::get(&state.pool, id.account_id, slot)
                .await?
                .ok_or_else(|| AppError::Internal(anyhow::anyhow!("copied slot vanished")))?;
            Ok((StatusCode::CREATED, Json(saved)))
        }
    }
}
```

- [ ] **Step 5: Wire routes and main**

`lib.rs` — `build_app(pool, cfg, profiles: Arc<dyn ProfileSource>)`:
```rust
let showcase_state = showcase::handlers::ShowcaseState { pool: pool.clone(), profiles };
let showcase_routes = Router::new()
    .route("/configs/{slot}/publish", post(showcase::handlers::publish).delete(showcase::handlers::unpublish))
    .route("/showcase", get(showcase::handlers::browse))
    .route("/showcase/{id}", get(showcase::handlers::detail))
    .route("/showcase/{id}/copy", post(showcase::handlers::copy))
    .with_state(showcase_state);
// merge showcase_routes into `api` before the DefaultBodyLimit / internal-key layers
```
`main.rs`: `let profiles = Arc::new(configs_service::showcase::profiles::GrpcProfiles::connect_lazy(&cfg.accounts_grpc_url, &cfg.internal_key)?);` and pass it. Update `tests/slots.rs`/`tests/sharing.rs` `build_app` calls to pass `common::no_profiles()`.

- [ ] **Step 6: Tests pass; commit**

```bash
git add -A backend/configs-service
git commit -m "feat(configs): public showcase with publish, browse, detail and copy-to-slot"
```

---

### Task 5b: Public profile data (for the site's `/u/:id`)

**Files:** accounts-service `src/accounts/handlers.rs` (+ `public_profile`), `src/accounts/repo.rs` (+ `account_rank`), `tests/auth_flow.rs`; configs-service `src/showcase/{repo,handlers}.rs` (`author` filter), `tests/showcase.rs`.

**Interfaces:**
- accounts-service `GET /users/{id}` — public, no identity needed (still behind the internal key): `200 { id, display_nick, avatar_url: string|null, created_at, badges: string[] }`, 404 for unknown/invalid ids. Never returns email or role. `badges`: `"early"` when the account is among the first 1000 by `created_at` (`SELECT count(*) FROM accounts WHERE created_at < $1` < 1000). Gateway routes `users` → accounts (already in the gateway plan's table).
- configs-service `GET /showcase?author={uuid}` — same page shape, filtered to that account's non-hidden listings; invalid uuid → 400.

- [ ] **Step 1: Failing tests** — `/users/{id}` returns nick without email and includes `early`; unknown id 404; `/showcase?author=` returns only that author's listings.
- [ ] **Step 2: Implement** (`repo::page` gains `author: Option<Uuid>` → `AND l.account_id = $3` when set).
- [ ] **Step 3: Commit** — `feat(backend): public profile endpoint and showcase author filter`

---

### Task 6: End-to-end through the gateway + docs

**Files:**
- Modify: `backend/STRUCTURE.md`, `TODO.md` (Фаза 10 status), `backend/.env.example`

- [ ] **Step 1: Manual e2e (three processes, real DBs)**

```bash
cd backend && set -a; . ./.env; set +a
CARGO_BUILD_JOBS=4 cargo build -p accounts-service -p configs-service -p gateway
./target/debug/accounts-service & ./target/debug/configs-service & ./target/debug/gateway &
sleep 3
# register + login as in gateway PLAN Task 11, then:
curl -s -X PUT localhost:8080/configs/1 -H "authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d '{"name":"pvp","data":{"Hud":true}}'                       # -> slot with share_code
curl -s localhost:8080/configs/shared/<CODE>                     # -> {name,data} without auth
curl -s -X POST localhost:8080/configs/1/publish -H "authorization: Bearer $TOKEN" \
  -H 'content-type: application/json' -d '{"title":"My PvP"}'
curl -s 'localhost:8080/showcase?sort=new'                       # -> author.nick = your nick (via gRPC)
kill %1 %2 %3
```
Expected: each call succeeds and the showcase item carries the real nick.

- [ ] **Step 2: Docs + commit**

`STRUCTURE.md`: mark configs-service implemented. `TODO.md`: Подсистема 1 backend done; next = frontend (`frontend/PLAN.md`) and mod integration.
```bash
git add -A backend/STRUCTURE.md TODO.md backend/.env.example
git commit -m "docs(backend): configs-service implemented; Подсистема 1 backend complete"
```

---

### Task 7: `IDDQD` easter egg — done

`GET /configs/shared/{code}`: when `normalize(code) == "IDDQD"` return `200 { name: "God mode", data: <joke config>, updated_at: "1993-12-10T00:00:00Z" }` without touching the DB (DOOM's release date). The joke config data: every module disabled except `ChinaHat` (exact JSON shape = the mod's config format; until the mod-integration plan defines it, use `{ "modules": { "ChinaHat": { "enabled": true } }, "note": "god mode: только шляпа" }`). Real codes can never be `IDDQD` (no `I` in the alphabet, length 5). Test + commit `feat(configs): IDDQD easter egg config`.

Implemented in `src/sharing/handlers.rs::load_shared` (checked before hitting `repo::by_share_code`); test in `tests/sharing.rs::iddqd_is_a_god_mode_easter_egg_without_touching_the_db`.
