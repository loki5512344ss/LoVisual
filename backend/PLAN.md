# Backend: accounts-service (Подсистема 1, часть 1) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standalone `accounts-service` (Rust/Axum) with email+password auth, JWT issue/refresh, OAuth-2-style device-code linking for the mod, and avatar upload — the foundation the gateway, configs-service and the site will build on.

**Architecture:** Single Axum binary, `sqlx` against PostgreSQL (own `accounts_db`), `argon2` for password hashing, `jsonwebtoken` for access/refresh tokens, avatars stored in S3-compatible storage via the `aws-sdk-s3` crate (MinIO-compatible). No gateway yet — this service is directly reachable on its own port for this plan; the gateway that fronts it (routing + JWT check for the site/mod) is a **separate follow-up plan**, as is `configs-service` and the React frontend. This keeps each plan independently testable, per the microservices decomposition agreed in `/storage/project/jvm/LoVisual/TODO.md` (Фаза 10).

**Tech Stack:** Rust (stable, edition 2024), Axum 0.8, sqlx 0.9 (postgres, runtime-tokio, tls-rustls, macros, migrate), tokio, argon2 0.6, jsonwebtoken 11 (rust_crypto backend), aws-sdk-s3, serde/serde_json, uuid (v4), rand 0.10 (for device/share codes), tower-http 0.7 (trace, cors), dotenvy. All versions verified by compiling on 2026-09-23 — see the note after Task 1 Step 1 for exact API deltas from older docs/tutorials.

## Global Constraints

- All money/user-facing strings: Russian (matches project convention seen in `TODO.md`/`mod/`), but code identifiers/comments in English (matches existing mod codebase style).
- No panics on user input — every handler returns a typed error mapped to an HTTP status, never `.unwrap()` on request data.
- Passwords: argon2id, never logged, never returned in any response.
- Every new module gets tests before being wired into `main.rs` (TDD, per project convention already established in `mod/`: "Тесты обязательны для новых модулей").
- Max 250 lines per file (facades/delegates excepted) — see `TODO.md`'s "Правила платформы" section; every file created by this plan stays under that on its own, but keep it in mind if a later task grows one.
- Max 4 files per directory (subdirectories don't count against this) — same rule as the mod, see `TODO.md`. Every module directory in this plan (`auth/`, `accounts/`, `device/`, `avatars/`) stays at 3-4 files; if a later task needs a 5th file in one, split a meaningfully-named subdirectory instead of just adding the file.
- No `.clone()` to route around the borrow checker — take a reference instead; no blocking I/O inside an `async fn`; no God-structs (state is split per domain: `AuthState`, `DeviceState`, `AvatarState`, never one `AppState` holding everything) — see `TODO.md`'s anti-pattern list.
- Commit after every passing task, in this repo (`/storage/project/jvm/LoVisual`), path `backend/`.

---

## File Structure

```
backend/
  accounts-service/
    Cargo.toml
    migrations/
      0001_init.sql
    src/
      main.rs              # axum app assembly, router, DB pool, S3 client
      config.rs             # env var loading (DATABASE_URL, JWT_SECRET, S3_*, PORT)
      error.rs              # AppError enum -> IntoResponse
      db.rs                 # PgPool type alias + connect()
      auth/
        mod.rs               # re-exports
        password.rs          # hash_password, verify_password (argon2)
        jwt.rs                # issue_access_token, issue_refresh_token, verify_access_token
        handlers.rs           # POST /auth/register, /auth/login, /auth/refresh, /auth/logout
      device/
        mod.rs
        handlers.rs           # POST /device/code, /device/confirm, /device/token
        store.rs              # in-memory device-code store (DashMap) with TTL
      avatars/
        mod.rs
        handlers.rs           # POST /avatars, multipart upload
        storage.rs            # S3 put_object wrapper
      accounts/
        mod.rs
        model.rs              # Account struct, row mapping
        repo.rs                # sqlx queries: create, find_by_email, find_by_id, update_avatar_url
```

---

### Task 1: Project scaffold + health check

**Files:**
- Create: `backend/accounts-service/Cargo.toml`
- Create: `backend/accounts-service/src/main.rs`
- Create: `backend/accounts-service/src/config.rs`
- Create: `backend/.env.example`
- Test: `backend/accounts-service/tests/health.rs`

**Interfaces:**
- Produces: `fn build_app(pool: sqlx::PgPool) -> axum::Router` (used by every later task to mount new routes; tests call this directly instead of spawning a real server)
- Produces: `struct Config { database_url: String, jwt_secret: String, port: u16, s3_endpoint: String, s3_bucket: String, s3_access_key: String, s3_secret_key: String }` with `Config::from_env() -> anyhow::Result<Config>`

- [ ] **Step 1: Create the crate**

```bash
mkdir -p /storage/project/jvm/LoVisual/backend/accounts-service/src
cd /storage/project/jvm/LoVisual/backend/accounts-service
cat > Cargo.toml << 'EOF'
[package]
name = "accounts-service"
version = "0.1.0"
edition = "2024"

[dependencies]
axum = { version = "0.8", features = ["multipart"] }
tokio = { version = "1", features = ["rt-multi-thread", "macros"] }
tower-http = { version = "0.7", features = ["trace", "cors"] }
tracing = "0.1"
tracing-subscriber = "0.3"
serde = { version = "1", features = ["derive"] }
serde_json = "1"
sqlx = { version = "0.9", default-features = false, features = ["runtime-tokio", "tls-rustls", "postgres", "uuid", "chrono", "macros", "migrate"] }
uuid = { version = "1", features = ["v4", "serde"] }
chrono = { version = "0.4", features = ["serde"] }
argon2 = "0.6"
jsonwebtoken = { version = "11", default-features = false, features = ["rust_crypto"] }
rand = "0.10"
dashmap = "6"
aws-sdk-s3 = "1"
aws-config = "1"
anyhow = "1"
dotenvy = "0.15"

[dev-dependencies]
axum-test = "21"
EOF
```

> Versions verified by actually compiling each one (not from memory) on
> 2026-09-23 against Rust 1.97.1 — several had breaking changes since the
> versions this plan originally targeted, called out inline where the code
> differs from what you'd expect from older docs/tutorials:
> - **sqlx 0.8 → 0.9**: the combined `runtime-tokio-rustls` feature was
>   split into separate `runtime-tokio` + `tls-rustls` features (hence
>   `default-features = false` above — the new defaults pull in `any`/mysql/
>   sqlite machinery this service doesn't need). `FromRow`/`query_as`/
>   `PgPoolOptions` themselves are unchanged.
> - **argon2 0.5 → 0.6**: `SaltString`/`PasswordHash` moved and the manual
>   two-argument `hash_password(bytes, &salt)` call is gone — see Task 3,
>   the salt is now generated internally by `hash_password(bytes)` alone.
> - **jsonwebtoken 9 → 11**: now requires explicitly picking a crypto
>   backend feature (`rust_crypto` here — pure Rust, no OpenSSL/BoringSSL
>   toolchain needed) or it panics at runtime with "Could not automatically
>   determine the process-level CryptoProvider". `encode`/`decode` calls
>   themselves are unchanged.
> - **rand 0.8 → 0.10**: `rand::thread_rng()` → `rand::rng()`, and
>   `Rng::gen_range` moved to the `RngExt` trait as `random_range` — see
>   Task 7's `random_user_code()`.
> - **axum-test 16 → 21**: `TestServer::new(app)` now returns `Self`
>   directly instead of `Result<Self, _>` — drop the `.unwrap()` every test
>   in this plan that constructs one.
> - **`thiserror` dropped** from the dependency list — nothing in this plan
>   actually derives `thiserror::Error` (`AppError` in Task 6 is hand-written),
>   so it was dead weight; add it back only if a later task needs it.

- [ ] **Step 2: Write `config.rs`**

```rust
// backend/accounts-service/src/config.rs
use anyhow::{Context, Result};

#[derive(Clone)]
pub struct Config {
    pub database_url: String,
    pub jwt_secret: String,
    pub port: u16,
    pub s3_endpoint: String,
    pub s3_bucket: String,
    pub s3_access_key: String,
    pub s3_secret_key: String,
}

impl Config {
    pub fn from_env() -> Result<Config> {
        Ok(Config {
            database_url: std::env::var("DATABASE_URL")
                .context("DATABASE_URL not set")?,
            jwt_secret: std::env::var("JWT_SECRET")
                .context("JWT_SECRET not set")?,
            port: std::env::var("PORT")
                .unwrap_or_else(|_| "8081".into())
                .parse()
                .context("PORT must be a number")?,
            s3_endpoint: std::env::var("S3_ENDPOINT")
                .context("S3_ENDPOINT not set")?,
            s3_bucket: std::env::var("S3_BUCKET")
                .context("S3_BUCKET not set")?,
            s3_access_key: std::env::var("S3_ACCESS_KEY")
                .context("S3_ACCESS_KEY not set")?,
            s3_secret_key: std::env::var("S3_SECRET_KEY")
                .context("S3_SECRET_KEY not set")?,
        })
    }
}
```

- [ ] **Step 3: Write `main.rs` with a health route**

```rust
// backend/accounts-service/src/main.rs
mod config;

use axum::{routing::get, Router};

pub fn build_app(_pool: sqlx::PgPool) -> Router {
    Router::new().route("/health", get(|| async { "ok" }))
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt::init();
    dotenvy::dotenv().ok();
    let cfg = config::Config::from_env()?;

    let pool = sqlx::postgres::PgPoolOptions::new()
        .max_connections(10)
        .connect(&cfg.database_url)
        .await?;

    let app = build_app(pool);
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", cfg.port)).await?;
    tracing::info!("accounts-service listening on {}", cfg.port);
    axum::serve(listener, app).await?;
    Ok(())
}
```

- [ ] **Step 4: Create `backend/.env.example`**

```bash
cat > /storage/project/jvm/LoVisual/backend/.env.example << 'EOF'
DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/accounts_db
JWT_SECRET=change-me-to-a-long-random-string
PORT=8081
S3_ENDPOINT=http://localhost:9000
S3_BUCKET=lovisual-avatars
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin
EOF
```

- [ ] **Step 5: Write the failing test**

```rust
// backend/accounts-service/tests/health.rs
// Requires `pub mod` exposure — add `pub mod config;` and make build_app pub
// in a lib target. Add to Cargo.toml under [package]:
//   [lib]
//   name = "accounts_service"
//   path = "src/lib.rs"
// and move the `build_app` fn (and `mod config;`) into src/lib.rs, with
// main.rs reduced to `use accounts_service::{build_app, config};` + the
// existing #[tokio::main] body unchanged.

use axum_test::TestServer;

#[tokio::test]
async fn health_returns_ok() {
    // NOTE: this test does not touch the DB — pass a pool that is never
    // queried. sqlx::PgPool::connect_lazy never opens a connection until
    // a query runs, so this is safe without a running Postgres.
    let pool = sqlx::PgPool::connect_lazy("postgres://user:pass@localhost/db")
        .expect("lazy pool");
    let app = accounts_service::build_app(pool);
    let server = TestServer::new(app);

    let response = server.get("/health").await;
    response.assert_status_ok();
    response.assert_text("ok");
}
```

- [ ] **Step 6: Move `build_app`/`mod config` into `src/lib.rs` as described in the test's NOTE**

```rust
// backend/accounts-service/src/lib.rs
pub mod config;

use axum::{routing::get, Router};

pub fn build_app(_pool: sqlx::PgPool) -> Router {
    Router::new().route("/health", get(|| async { "ok" }))
}
```

```rust
// backend/accounts-service/src/main.rs
use accounts_service::{build_app, config::Config};

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt::init();
    dotenvy::dotenv().ok();
    let cfg = Config::from_env()?;

    let pool = sqlx::postgres::PgPoolOptions::new()
        .max_connections(10)
        .connect(&cfg.database_url)
        .await?;

    let app = build_app(pool);
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", cfg.port)).await?;
    tracing::info!("accounts-service listening on {}", cfg.port);
    axum::serve(listener, app).await?;
    Ok(())
}
```

Add to `Cargo.toml` (after `[package]`, before `[dependencies]`):

```toml
[lib]
name = "accounts_service"
path = "src/lib.rs"
```

- [ ] **Step 7: Run the test**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && cargo test --test health`
Expected: `test health_returns_ok ... ok`

- [ ] **Step 8: Commit**

```bash
cd /storage/project/jvm/LoVisual
git add backend/accounts-service backend/.env.example
git commit -m "feat(backend): scaffold accounts-service with health check"
```

---

### Task 2: Database schema — `accounts` table + migration runner

**Files:**
- Create: `backend/accounts-service/migrations/0001_init.sql`
- Modify: `backend/accounts-service/src/lib.rs` (run migrations on startup is done in `main.rs`, not `build_app` — tests don't need a live DB for Task 1's test)
- Modify: `backend/accounts-service/Cargo.toml` (sqlx already has `migrate` via the `sqlx` feature `migrate`; add it)
- Test: `backend/accounts-service/tests/db_migration.rs`

**Interfaces:**
- Produces: `accounts` table with columns `id uuid PK, email text UNIQUE, password_hash text, display_nick text, role text NOT NULL DEFAULT 'user', can_publish_addons boolean NOT NULL DEFAULT true, created_at timestamptz NOT NULL DEFAULT now()`
- Produces: `avatars` table `account_id uuid PK REFERENCES accounts(id), s3_key text, uploaded_at timestamptz NOT NULL DEFAULT now()`
- Produces: `device_links` table `id uuid PK, account_id uuid REFERENCES accounts(id), device_token_hash text, linked_at timestamptz NOT NULL DEFAULT now(), last_seen timestamptz`
- This task requires a real local Postgres reachable at `DATABASE_URL` — later tasks' tests assume the same. Document the prerequisite once here.

- [ ] **Step 1: Add the `migrate` feature to sqlx**

Edit `backend/accounts-service/Cargo.toml`, change the sqlx line to:

```toml
sqlx = { version = "0.8", features = ["runtime-tokio-rustls", "postgres", "uuid", "chrono", "macros", "migrate"] }
```

- [ ] **Step 2: Write the migration**

```sql
-- backend/accounts-service/migrations/0001_init.sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    display_nick TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user',
    can_publish_addons BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE avatars (
    account_id UUID PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    s3_key TEXT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE device_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    device_token_hash TEXT NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen TIMESTAMPTZ
);

CREATE INDEX idx_device_links_account_id ON device_links(account_id);
```

`gen_random_uuid()` needs the `pgcrypto` extension. Add it at the top of the same file:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

(Prepend this line to `0001_init.sql`, before `CREATE TABLE accounts`.)

- [ ] **Step 3: Ensure a local Postgres exists for tests**

Document the one-time local setup (not a plan step to automate — infra prerequisite):

```bash
# One-time, run manually before continuing:
docker run -d --name lovisual-pg -e POSTGRES_USER=lovisual -e POSTGRES_PASSWORD=lovisual \
  -e POSTGRES_DB=accounts_db -p 5432:5432 postgres:16
```

- [ ] **Step 4: Write the failing test**

```rust
// backend/accounts-service/tests/db_migration.rs
#[tokio::test]
async fn migrations_create_accounts_table() {
    let database_url = std::env::var("DATABASE_URL")
        .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
    let pool = sqlx::PgPool::connect(&database_url).await.expect("connect");

    sqlx::migrate!("./migrations").run(&pool).await.expect("migrate");

    let row: (i64,) = sqlx::query_as("SELECT count(*) FROM accounts")
        .fetch_one(&pool)
        .await
        .expect("accounts table must exist");
    assert_eq!(row.0, 0);
}
```

- [ ] **Step 5: Run test to verify it fails (before migrations exist it errors; run once the SQL file above is in place — this step confirms the table truly gets created, not that it's absent)**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/accounts_db cargo test --test db_migration`
Expected: PASS (this task's "test" is really a smoke test that migrations apply cleanly — there's no meaningful pre-migration state to assert against, so we go straight to green and treat this as an integration checkpoint, not strict TDD red/green)

- [ ] **Step 6: Commit**

```bash
cd /storage/project/jvm/LoVisual
git add backend/accounts-service
git commit -m "feat(backend): accounts/avatars/device_links schema + migration"
```

---

### Task 3: Password hashing module

**Files:**
- Create: `backend/accounts-service/src/auth/mod.rs`
- Create: `backend/accounts-service/src/auth/password.rs`
- Modify: `backend/accounts-service/src/lib.rs` (add `pub mod auth;`)

**Interfaces:**
- Produces: `pub fn hash_password(plain: &str) -> Result<String, argon2::password_hash::Error>`
- Produces: `pub fn verify_password(plain: &str, hash: &str) -> bool`
- Consumed by: Task 5 (register/login handlers)

- [ ] **Step 1: Write the failing test**

```rust
// backend/accounts-service/src/auth/password.rs (tests at the bottom of the same file)
use argon2::{Argon2, PasswordHasher, PasswordVerifier};
use argon2::password_hash::phc::PasswordHash;

pub fn hash_password(plain: &str) -> Result<String, argon2::password_hash::Error> {
    let argon2 = Argon2::default();
    Ok(argon2.hash_password(plain.as_bytes())?.to_string())
}

pub fn verify_password(plain: &str, hash: &str) -> bool {
    let Ok(parsed) = PasswordHash::new(hash) else { return false };
    Argon2::default()
        .verify_password(plain.as_bytes(), &parsed)
        .is_ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn hash_then_verify_succeeds() {
        let hash = hash_password("correct-horse-battery-staple").unwrap();
        assert!(verify_password("correct-horse-battery-staple", &hash));
    }

    #[test]
    fn wrong_password_fails_verify() {
        let hash = hash_password("correct-horse-battery-staple").unwrap();
        assert!(!verify_password("wrong-password", &hash));
    }

    #[test]
    fn hash_is_not_the_plain_password() {
        let hash = hash_password("secret123").unwrap();
        assert_ne!(hash, "secret123");
    }
}
```

- [ ] **Step 2: Wire the module**

```rust
// backend/accounts-service/src/auth/mod.rs
pub mod password;
```

Add `pub mod auth;` to `backend/accounts-service/src/lib.rs` (near `pub mod config;`).

- [ ] **Step 3: Run tests**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && cargo test password::`
Expected: 3 tests pass

- [ ] **Step 4: Commit**

```bash
cd /storage/project/jvm/LoVisual
git add backend/accounts-service/src
git commit -m "feat(backend): argon2id password hashing"
```

---

### Task 4: JWT issue/verify

**Files:**
- Create: `backend/accounts-service/src/auth/jwt.rs`
- Modify: `backend/accounts-service/src/auth/mod.rs` (add `pub mod jwt;`)

**Interfaces:**
- Consumes: nothing new (takes `account_id: uuid::Uuid`, `secret: &str` as plain params — no dependency on DB types)
- Produces: `pub struct Claims { pub sub: String, pub exp: usize, pub token_type: String }`
- Produces: `pub fn issue_access_token(account_id: Uuid, secret: &str) -> String` (15 min expiry, `token_type: "access"`)
- Produces: `pub fn issue_refresh_token(account_id: Uuid, secret: &str) -> String` (30 day expiry, `token_type: "refresh"`)
- Produces: `pub fn verify_token(token: &str, secret: &str) -> Option<Claims>` — returns `None` on any invalid/expired/malformed token, never panics

- [ ] **Step 1: Write the failing test**

```rust
// backend/accounts-service/src/auth/jwt.rs
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, Validation};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub sub: String,
    pub exp: usize,
    pub token_type: String,
}

fn issue(account_id: Uuid, secret: &str, token_type: &str, ttl_seconds: i64) -> String {
    let exp = (chrono::Utc::now() + chrono::Duration::seconds(ttl_seconds)).timestamp() as usize;
    let claims = Claims { sub: account_id.to_string(), exp, token_type: token_type.into() };
    encode(&Header::default(), &claims, &EncodingKey::from_secret(secret.as_bytes()))
        .expect("encoding a well-formed Claims struct cannot fail")
}

pub fn issue_access_token(account_id: Uuid, secret: &str) -> String {
    issue(account_id, secret, "access", 15 * 60)
}

pub fn issue_refresh_token(account_id: Uuid, secret: &str) -> String {
    issue(account_id, secret, "refresh", 30 * 24 * 60 * 60)
}

pub fn verify_token(token: &str, secret: &str) -> Option<Claims> {
    decode::<Claims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &Validation::default(),
    )
    .ok()
    .map(|data| data.claims)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn access_token_round_trips() {
        let id = Uuid::new_v4();
        let token = issue_access_token(id, "test-secret");
        let claims = verify_token(&token, "test-secret").expect("should decode");
        assert_eq!(claims.sub, id.to_string());
        assert_eq!(claims.token_type, "access");
    }

    #[test]
    fn wrong_secret_fails_verify() {
        let token = issue_access_token(Uuid::new_v4(), "test-secret");
        assert!(verify_token(&token, "other-secret").is_none());
    }

    #[test]
    fn garbage_token_fails_verify() {
        assert!(verify_token("not.a.jwt", "test-secret").is_none());
    }
}
```

- [ ] **Step 2: Wire the module**

Add `pub mod jwt;` to `backend/accounts-service/src/auth/mod.rs`.

- [ ] **Step 3: Run tests**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && cargo test jwt::`
Expected: 3 tests pass

- [ ] **Step 4: Commit**

```bash
cd /storage/project/jvm/LoVisual
git add backend/accounts-service/src
git commit -m "feat(backend): JWT access/refresh token issue and verify"
```

---

### Task 5: Accounts repository

**Files:**
- Create: `backend/accounts-service/src/accounts/mod.rs`
- Create: `backend/accounts-service/src/accounts/model.rs`
- Create: `backend/accounts-service/src/accounts/repo.rs`
- Modify: `backend/accounts-service/src/lib.rs` (add `pub mod accounts;`)

**Interfaces:**
- Consumes: `sqlx::PgPool` (from Task 1/2)
- Produces: `pub struct Account { pub id: Uuid, pub email: String, pub password_hash: String, pub display_nick: String, pub role: String, pub can_publish_addons: bool, pub created_at: chrono::DateTime<chrono::Utc> }`
- Produces: `pub async fn create(pool: &PgPool, email: &str, password_hash: &str, nick: &str) -> Result<Account, sqlx::Error>`
- Produces: `pub async fn find_by_email(pool: &PgPool, email: &str) -> Result<Option<Account>, sqlx::Error>`
- Produces: unique-violation on duplicate email surfaces as `sqlx::Error::Database` — callers (Task 6) match on `.constraint() == Some("accounts_email_key")`

- [ ] **Step 1: Write the model**

```rust
// backend/accounts-service/src/accounts/model.rs
use chrono::{DateTime, Utc};
use serde::Serialize;
use uuid::Uuid;

#[derive(Debug, Clone, sqlx::FromRow, Serialize)]
pub struct Account {
    pub id: Uuid,
    pub email: String,
    #[serde(skip_serializing)]
    pub password_hash: String,
    pub display_nick: String,
    pub role: String,
    pub can_publish_addons: bool,
    pub created_at: DateTime<Utc>,
}
```

- [ ] **Step 2: Write the failing test (integration, needs the live Postgres from Task 2)**

```rust
// backend/accounts-service/src/accounts/repo.rs
use super::model::Account;
use sqlx::PgPool;
use uuid::Uuid;

pub async fn create(
    pool: &PgPool,
    email: &str,
    password_hash: &str,
    nick: &str,
) -> Result<Account, sqlx::Error> {
    sqlx::query_as::<_, Account>(
        "INSERT INTO accounts (email, password_hash, display_nick)
         VALUES ($1, $2, $3)
         RETURNING id, email, password_hash, display_nick, role, can_publish_addons, created_at",
    )
    .bind(email)
    .bind(password_hash)
    .bind(nick)
    .fetch_one(pool)
    .await
}

pub async fn find_by_email(pool: &PgPool, email: &str) -> Result<Option<Account>, sqlx::Error> {
    sqlx::query_as::<_, Account>(
        "SELECT id, email, password_hash, display_nick, role, can_publish_addons, created_at
         FROM accounts WHERE email = $1",
    )
    .bind(email)
    .fetch_optional(pool)
    .await
}

pub async fn find_by_id(pool: &PgPool, id: Uuid) -> Result<Option<Account>, sqlx::Error> {
    sqlx::query_as::<_, Account>(
        "SELECT id, email, password_hash, display_nick, role, can_publish_addons, created_at
         FROM accounts WHERE id = $1",
    )
    .bind(id)
    .fetch_optional(pool)
    .await
}

#[cfg(test)]
mod tests {
    use super::*;

    async fn test_pool() -> PgPool {
        let url = std::env::var("DATABASE_URL")
            .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
        let pool = PgPool::connect(&url).await.expect("connect");
        sqlx::migrate!("./migrations").run(&pool).await.expect("migrate");
        pool
    }

    #[tokio::test]
    async fn create_then_find_by_email_round_trips() {
        let pool = test_pool().await;
        let email = format!("test-{}@example.com", Uuid::new_v4());

        let created = create(&pool, &email, "hash123", "TestNick").await.unwrap();
        assert_eq!(created.email, email);
        assert_eq!(created.role, "user");
        assert!(created.can_publish_addons);

        let found = find_by_email(&pool, &email).await.unwrap().expect("must exist");
        assert_eq!(found.id, created.id);

        sqlx::query("DELETE FROM accounts WHERE id = $1")
            .bind(created.id)
            .execute(&pool)
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn find_by_email_returns_none_for_missing() {
        let pool = test_pool().await;
        let result = find_by_email(&pool, "does-not-exist@example.com").await.unwrap();
        assert!(result.is_none());
    }
}
```

- [ ] **Step 3: Wire the module**

```rust
// backend/accounts-service/src/accounts/mod.rs
pub mod model;
pub mod repo;
```

Add `pub mod accounts;` to `backend/accounts-service/src/lib.rs`.

- [ ] **Step 4: Run tests**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/accounts_db cargo test accounts::repo::`
Expected: 2 tests pass

- [ ] **Step 5: Commit**

```bash
cd /storage/project/jvm/LoVisual
git add backend/accounts-service/src
git commit -m "feat(backend): accounts repository (create/find_by_email/find_by_id)"
```

---

### Task 6: `POST /auth/register` and `POST /auth/login`

**Files:**
- Create: `backend/accounts-service/src/error.rs`
- Create: `backend/accounts-service/src/auth/handlers.rs`
- Modify: `backend/accounts-service/src/auth/mod.rs` (add `pub mod handlers;`)
- Modify: `backend/accounts-service/src/lib.rs` (`build_app` now takes routes + real pool; add `.route("/auth/register", post(...))` etc.)
- Test: `backend/accounts-service/tests/auth_flow.rs`

**Interfaces:**
- Consumes: `auth::password::{hash_password, verify_password}` (Task 3), `auth::jwt::{issue_access_token, issue_refresh_token}` (Task 4), `accounts::repo::{create, find_by_email}` (Task 5)
- Produces: `AppError` enum implementing `axum::response::IntoResponse`, variants `Validation(String) -> 400`, `Conflict(String) -> 409`, `Unauthorized -> 401`, `Internal(anyhow::Error) -> 500`
- Produces: JSON contract — `POST /auth/register` body `{email, password, nick}` → `201 {id, email, display_nick}`; `POST /auth/login` body `{email, password}` → `200 {access_token, refresh_token}`

- [ ] **Step 1: Write `error.rs`**

```rust
// backend/accounts-service/src/error.rs
use axum::{http::StatusCode, response::{IntoResponse, Response}, Json};
use serde_json::json;

#[derive(Debug)]
pub enum AppError {
    Validation(String),
    Conflict(String),
    Unauthorized,
    Internal(anyhow::Error),
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, message) = match self {
            AppError::Validation(msg) => (StatusCode::BAD_REQUEST, msg),
            AppError::Conflict(msg) => (StatusCode::CONFLICT, msg),
            AppError::Unauthorized => (StatusCode::UNAUTHORIZED, "unauthorized".into()),
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
        if let sqlx::Error::Database(ref db_err) = err {
            if db_err.constraint() == Some("accounts_email_key") {
                return AppError::Conflict("email already registered".into());
            }
        }
        AppError::Internal(err.into())
    }
}
```

- [ ] **Step 2: Write the failing test**

```rust
// backend/accounts-service/tests/auth_flow.rs
use axum_test::TestServer;
use serde_json::json;
use uuid::Uuid;

async fn test_pool() -> sqlx::PgPool {
    let url = std::env::var("DATABASE_URL")
        .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
    let pool = sqlx::PgPool::connect(&url).await.expect("connect");
    sqlx::migrate!("../migrations").run(&pool).await.expect("migrate");
    pool
}

#[tokio::test]
async fn register_then_login_succeeds() {
    let pool = test_pool().await;
    let app = accounts_service::build_app(pool.clone(), "test-secret".into());
    let server = TestServer::new(app);
    let email = format!("flow-{}@example.com", Uuid::new_v4());

    let register_response = server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "correct-horse-battery-staple", "nick": "Rider" }))
        .await;
    register_response.assert_status(axum::http::StatusCode::CREATED);

    let login_response = server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "correct-horse-battery-staple" }))
        .await;
    login_response.assert_status_ok();
    let body: serde_json::Value = login_response.json();
    assert!(body["access_token"].is_string());
    assert!(body["refresh_token"].is_string());

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn login_with_wrong_password_returns_401() {
    let pool = test_pool().await;
    let app = accounts_service::build_app(pool.clone(), "test-secret".into());
    let server = TestServer::new(app);
    let email = format!("wrongpw-{}@example.com", Uuid::new_v4());

    server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "right-password", "nick": "Rider" }))
        .await
        .assert_status(axum::http::StatusCode::CREATED);

    let login_response = server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "wrong-password" }))
        .await;
    login_response.assert_status(axum::http::StatusCode::UNAUTHORIZED);

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn duplicate_email_registration_returns_409() {
    let pool = test_pool().await;
    let app = accounts_service::build_app(pool.clone(), "test-secret".into());
    let server = TestServer::new(app);
    let email = format!("dup-{}@example.com", Uuid::new_v4());

    server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "password123", "nick": "First" }))
        .await
        .assert_status(axum::http::StatusCode::CREATED);

    let second = server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "password456", "nick": "Second" }))
        .await;
    second.assert_status(axum::http::StatusCode::CONFLICT);

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}
```

- [ ] **Step 3: Run the tests to see them fail to compile (build_app signature doesn't match yet)**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/accounts_db cargo test --test auth_flow`
Expected: compile error, `build_app` takes 1 argument but 2 were supplied

- [ ] **Step 4: Write the handlers**

```rust
// backend/accounts-service/src/auth/handlers.rs
use crate::accounts::repo;
use crate::auth::{jwt, password};
use crate::error::AppError;
use axum::{extract::State, http::StatusCode, Json};
use serde::{Deserialize, Serialize};

#[derive(Clone)]
pub struct AuthState {
    pub pool: sqlx::PgPool,
    pub jwt_secret: String,
}

#[derive(Deserialize)]
pub struct RegisterRequest {
    pub email: String,
    pub password: String,
    pub nick: String,
}

#[derive(Serialize)]
pub struct RegisterResponse {
    pub id: uuid::Uuid,
    pub email: String,
    pub display_nick: String,
}

pub async fn register(
    State(state): State<AuthState>,
    Json(req): Json<RegisterRequest>,
) -> Result<(StatusCode, Json<RegisterResponse>), AppError> {
    if req.password.len() < 8 {
        return Err(AppError::Validation("password must be at least 8 characters".into()));
    }
    let hash = password::hash_password(&req.password)
        .map_err(|e| AppError::Internal(anyhow::anyhow!(e)))?;
    let account = repo::create(&state.pool, &req.email, &hash, &req.nick).await?;
    Ok((
        StatusCode::CREATED,
        Json(RegisterResponse {
            id: account.id,
            email: account.email,
            display_nick: account.display_nick,
        }),
    ))
}

#[derive(Deserialize)]
pub struct LoginRequest {
    pub email: String,
    pub password: String,
}

#[derive(Serialize)]
pub struct LoginResponse {
    pub access_token: String,
    pub refresh_token: String,
}

pub async fn login(
    State(state): State<AuthState>,
    Json(req): Json<LoginRequest>,
) -> Result<Json<LoginResponse>, AppError> {
    let account = repo::find_by_email(&state.pool, &req.email)
        .await?
        .ok_or(AppError::Unauthorized)?;
    if !password::verify_password(&req.password, &account.password_hash) {
        return Err(AppError::Unauthorized);
    }
    Ok(Json(LoginResponse {
        access_token: jwt::issue_access_token(account.id, &state.jwt_secret),
        refresh_token: jwt::issue_refresh_token(account.id, &state.jwt_secret),
    }))
}
```

- [ ] **Step 5: Wire `build_app` in `lib.rs`**

```rust
// backend/accounts-service/src/lib.rs
pub mod config;
pub mod error;
pub mod auth;
pub mod accounts;

use auth::handlers::AuthState;
use axum::{routing::{get, post}, Router};

pub fn build_app(pool: sqlx::PgPool, jwt_secret: String) -> Router {
    let state = AuthState { pool, jwt_secret };
    Router::new()
        .route("/health", get(|| async { "ok" }))
        .route("/auth/register", post(auth::handlers::register))
        .route("/auth/login", post(auth::handlers::login))
        .with_state(state)
}
```

Add `pub mod handlers;` to `backend/accounts-service/src/auth/mod.rs`.

Update `backend/accounts-service/src/main.rs` call site: `build_app(pool, cfg.jwt_secret.clone())`.

Update Task 1's `tests/health.rs` call site to match the new signature: `accounts_service::build_app(pool, "test-secret".into())`.

- [ ] **Step 6: Run tests**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/accounts_db cargo test`
Expected: all tests pass (health, jwt, password, accounts::repo, auth_flow)

- [ ] **Step 7: Commit**

```bash
cd /storage/project/jvm/LoVisual
git add backend/accounts-service
git commit -m "feat(backend): POST /auth/register and /auth/login"
```

---

### Task 7: Device Authorization Grant (`/device/code`, `/device/confirm`, `/device/token`)

**Files:**
- Create: `backend/accounts-service/src/device/mod.rs`
- Create: `backend/accounts-service/src/device/store.rs`
- Create: `backend/accounts-service/src/device/handlers.rs`
- Modify: `backend/accounts-service/src/lib.rs` (mount routes, extend shared state)
- Test: `backend/accounts-service/tests/device_flow.rs`

**Interfaces:**
- Consumes: `jwt::verify_token` (Task 4, to authenticate the `/device/confirm` caller — that endpoint requires a valid site access token in `Authorization: Bearer`)
- Produces: `DeviceStore` (in-memory `DashMap<String, DeviceCodeEntry>`, keyed by `device_code`), `pub fn new_pair() -> (String, String)` returning `(device_code, user_code)` where `user_code` is formatted `XXXX-XXXX` from `[A-Z0-9]`
- Produces: `POST /device/code` → `201 {device_code, user_code, expires_in: 600}`
- Produces: `POST /device/confirm` (auth required) body `{user_code}` → `200 {}` or `404` if code unknown/expired
- Produces: `POST /device/token` body `{device_code}` → `200 {device_token}` once confirmed, `202 {}` (still pending) before confirmation, `404` if unknown/expired

- [ ] **Step 1: Write the device code store**

```rust
// backend/accounts-service/src/device/store.rs
use dashmap::DashMap;
use rand::Rng;
use std::sync::Arc;
use std::time::{Duration, Instant};
use uuid::Uuid;

#[derive(Clone)]
pub struct DeviceCodeEntry {
    pub user_code: String,
    pub confirmed_account_id: Option<Uuid>,
    pub expires_at: Instant,
}

#[derive(Clone, Default)]
pub struct DeviceStore {
    by_device_code: Arc<DashMap<String, DeviceCodeEntry>>,
}

const TTL: Duration = Duration::from_secs(600);

fn random_user_code() -> String {
    use rand::RngExt;
    const ALPHABET: &[u8] = b"ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no O/0/I/1 confusion
    let mut rng = rand::rng();
    let mut part = |n: usize| -> String {
        (0..n).map(|_| ALPHABET[rng.random_range(0..ALPHABET.len())] as char).collect()
    };
    format!("{}-{}", part(4), part(4))
}

impl DeviceStore {
    pub fn create(&self) -> (String, String) {
        let device_code = Uuid::new_v4().to_string();
        let user_code = random_user_code();
        self.by_device_code.insert(
            device_code.clone(),
            DeviceCodeEntry {
                user_code: user_code.clone(),
                confirmed_account_id: None,
                expires_at: Instant::now() + TTL,
            },
        );
        (device_code, user_code)
    }

    /// Returns true if a matching, unexpired entry was found and confirmed.
    pub fn confirm(&self, user_code: &str, account_id: Uuid) -> bool {
        for mut entry in self.by_device_code.iter_mut() {
            if entry.user_code == user_code && entry.expires_at > Instant::now() {
                entry.confirmed_account_id = Some(account_id);
                return true;
            }
        }
        false
    }

    /// None = unknown/expired. Some(None) = pending. Some(Some(id)) = confirmed.
    pub fn poll(&self, device_code: &str) -> Option<Option<Uuid>> {
        let entry = self.by_device_code.get(device_code)?;
        if entry.expires_at <= Instant::now() {
            return None;
        }
        Some(entry.confirmed_account_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn create_returns_distinct_codes() {
        let store = DeviceStore::default();
        let (dc1, uc1) = store.create();
        let (dc2, uc2) = store.create();
        assert_ne!(dc1, dc2);
        assert_ne!(uc1, uc2);
    }

    #[test]
    fn confirm_then_poll_returns_account_id() {
        let store = DeviceStore::default();
        let (device_code, user_code) = store.create();
        let account_id = Uuid::new_v4();

        assert!(store.confirm(&user_code, account_id));
        assert_eq!(store.poll(&device_code), Some(Some(account_id)));
    }

    #[test]
    fn poll_before_confirm_is_pending() {
        let store = DeviceStore::default();
        let (device_code, _user_code) = store.create();
        assert_eq!(store.poll(&device_code), Some(None));
    }

    #[test]
    fn poll_unknown_code_returns_none() {
        let store = DeviceStore::default();
        assert_eq!(store.poll("does-not-exist"), None);
    }

    #[test]
    fn confirm_unknown_user_code_returns_false() {
        let store = DeviceStore::default();
        assert!(!store.confirm("ZZZZ-ZZZZ", Uuid::new_v4()));
    }
}
```

- [ ] **Step 2: Run the store's unit tests**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && cargo test device::store::`
Expected: 5 tests pass

- [ ] **Step 3: Write the handlers**

```rust
// backend/accounts-service/src/device/handlers.rs
use crate::device::store::DeviceStore;
use crate::error::AppError;
use axum::{extract::State, http::{HeaderMap, StatusCode}, Json};
use serde::{Deserialize, Serialize};

#[derive(Clone)]
pub struct DeviceState {
    pub store: DeviceStore,
    pub jwt_secret: String,
}

#[derive(Serialize)]
pub struct DeviceCodeResponse {
    pub device_code: String,
    pub user_code: String,
    pub expires_in: u64,
}

pub async fn create_code(
    State(state): State<DeviceState>,
) -> (StatusCode, Json<DeviceCodeResponse>) {
    let (device_code, user_code) = state.store.create();
    (
        StatusCode::CREATED,
        Json(DeviceCodeResponse { device_code, user_code, expires_in: 600 }),
    )
}

#[derive(Deserialize)]
pub struct ConfirmRequest {
    pub user_code: String,
}

fn account_id_from_auth_header(headers: &HeaderMap, secret: &str) -> Result<uuid::Uuid, AppError> {
    let header = headers
        .get(axum::http::header::AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .ok_or(AppError::Unauthorized)?;
    let token = header.strip_prefix("Bearer ").ok_or(AppError::Unauthorized)?;
    let claims = crate::auth::jwt::verify_token(token, secret).ok_or(AppError::Unauthorized)?;
    claims.sub.parse().map_err(|_| AppError::Unauthorized)
}

pub async fn confirm(
    State(state): State<DeviceState>,
    headers: HeaderMap,
    Json(req): Json<ConfirmRequest>,
) -> Result<StatusCode, AppError> {
    let account_id = account_id_from_auth_header(&headers, &state.jwt_secret)?;
    if state.store.confirm(&req.user_code, account_id) {
        Ok(StatusCode::OK)
    } else {
        Err(AppError::Validation("unknown or expired user_code".into()))
    }
}

#[derive(Deserialize)]
pub struct TokenRequest {
    pub device_code: String,
}

#[derive(Serialize)]
pub struct TokenResponse {
    pub device_token: String,
}

pub async fn token(
    State(state): State<DeviceState>,
    Json(req): Json<TokenRequest>,
) -> Result<(StatusCode, Json<Option<TokenResponse>>), AppError> {
    match state.store.poll(&req.device_code) {
        None => Err(AppError::Validation("unknown or expired device_code".into())),
        Some(None) => Ok((StatusCode::ACCEPTED, Json(None))),
        Some(Some(account_id)) => {
            // The long-lived device_token is just a refresh-style JWT for now;
            // Task 8+ (gateway plan) is where per-device revocation via
            // device_links.device_token_hash gets enforced on every request.
            let device_token = crate::auth::jwt::issue_refresh_token(account_id, &state.jwt_secret);
            Ok((StatusCode::OK, Json(Some(TokenResponse { device_token }))))
        }
    }
}
```

- [ ] **Step 4: Write the module file**

```rust
// backend/accounts-service/src/device/mod.rs
pub mod store;
pub mod handlers;
```

- [ ] **Step 5: Write the failing integration test**

```rust
// backend/accounts-service/tests/device_flow.rs
use axum_test::TestServer;
use serde_json::json;
use uuid::Uuid;

async fn test_pool() -> sqlx::PgPool {
    let url = std::env::var("DATABASE_URL")
        .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
    let pool = sqlx::PgPool::connect(&url).await.expect("connect");
    sqlx::migrate!("../migrations").run(&pool).await.expect("migrate");
    pool
}

#[tokio::test]
async fn full_device_link_flow() {
    let pool = test_pool().await;
    let app = accounts_service::build_app(pool.clone(), "test-secret".into());
    let server = TestServer::new(app);
    let email = format!("device-{}@example.com", Uuid::new_v4());

    server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "password123", "nick": "Rider" }))
        .await
        .assert_status(axum::http::StatusCode::CREATED);
    let login: serde_json::Value = server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "password123" }))
        .await
        .json();
    let access_token = login["access_token"].as_str().unwrap();

    let code_response: serde_json::Value =
        server.post("/device/code").await.json();
    let device_code = code_response["device_code"].as_str().unwrap();
    let user_code = code_response["user_code"].as_str().unwrap();

    let poll_before = server
        .post("/device/token")
        .json(&json!({ "device_code": device_code }))
        .await;
    poll_before.assert_status(axum::http::StatusCode::ACCEPTED);

    server
        .post("/device/confirm")
        .add_header(
            axum::http::header::AUTHORIZATION,
            format!("Bearer {access_token}"),
        )
        .json(&json!({ "user_code": user_code }))
        .await
        .assert_status_ok();

    let poll_after = server
        .post("/device/token")
        .json(&json!({ "device_code": device_code }))
        .await;
    poll_after.assert_status_ok();
    let token_body: serde_json::Value = poll_after.json();
    assert!(token_body["device_token"].is_string());

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}
```

- [ ] **Step 6: Wire routes in `lib.rs`**

```rust
// backend/accounts-service/src/lib.rs
pub mod config;
pub mod error;
pub mod auth;
pub mod accounts;
pub mod device;

use auth::handlers::AuthState;
use device::{handlers::DeviceState, store::DeviceStore};
use axum::{routing::{get, post}, Router};

pub fn build_app(pool: sqlx::PgPool, jwt_secret: String) -> Router {
    let auth_state = AuthState { pool: pool.clone(), jwt_secret: jwt_secret.clone() };
    let device_state = DeviceState { store: DeviceStore::default(), jwt_secret: jwt_secret.clone() };

    let auth_routes = Router::new()
        .route("/auth/register", post(auth::handlers::register))
        .route("/auth/login", post(auth::handlers::login))
        .with_state(auth_state);

    let device_routes = Router::new()
        .route("/device/code", post(device::handlers::create_code))
        .route("/device/confirm", post(device::handlers::confirm))
        .route("/device/token", post(device::handlers::token))
        .with_state(device_state);

    Router::new()
        .route("/health", get(|| async { "ok" }))
        .merge(auth_routes)
        .merge(device_routes)
}
```

- [ ] **Step 7: Run all tests**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/accounts_db cargo test`
Expected: all pass, including `full_device_link_flow`

- [ ] **Step 8: Commit**

```bash
cd /storage/project/jvm/LoVisual
git add backend/accounts-service
git commit -m "feat(backend): OAuth device authorization grant for mod login"
```

---

### Task 8: Avatar upload (`POST /avatars`)

**Files:**
- Create: `backend/accounts-service/src/avatars/mod.rs`
- Create: `backend/accounts-service/src/avatars/storage.rs`
- Create: `backend/accounts-service/src/avatars/handlers.rs`
- Modify: `backend/accounts-service/src/lib.rs` (mount route, extend `build_app` signature with S3 client)
- Test: `backend/accounts-service/tests/avatar_upload.rs`

**Interfaces:**
- Consumes: `jwt::verify_token` (Task 4, auth on this endpoint), `accounts::repo` pattern extended with `update_avatar` (new function in this task)
- Produces: `pub struct S3Storage { client: aws_sdk_s3::Client, bucket: String }` with `pub async fn put(&self, key: &str, bytes: Vec<u8>, content_type: &str) -> anyhow::Result<()>`
- Produces: `POST /avatars` (multipart, auth required, field name `file`) → `200 {avatar_url}`; rejects >5MB with `413`, rejects non-image magic bytes with `400`
- This task requires a local MinIO for tests: `docker run -d --name lovisual-minio -p 9000:9000 -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin minio/minio server /data` (one-time manual setup, same as Task 2's Postgres)

- [ ] **Step 1: Write the S3 storage wrapper**

```rust
// backend/accounts-service/src/avatars/storage.rs
use aws_sdk_s3::primitives::ByteStream;
use aws_sdk_s3::Client;

#[derive(Clone)]
pub struct S3Storage {
    pub client: Client,
    pub bucket: String,
}

impl S3Storage {
    pub async fn from_config(
        endpoint: &str,
        access_key: &str,
        secret_key: &str,
        bucket: String,
    ) -> Self {
        let creds = aws_sdk_s3::config::Credentials::new(access_key, secret_key, None, None, "static");
        let config = aws_sdk_s3::config::Builder::new()
            .endpoint_url(endpoint)
            .credentials_provider(creds)
            .region(aws_sdk_s3::config::Region::new("us-east-1"))
            .force_path_style(true)
            .behavior_version(aws_sdk_s3::config::BehaviorVersion::latest())
            .build();
        S3Storage { client: Client::from_conf(config), bucket }
    }

    pub async fn put(&self, key: &str, bytes: Vec<u8>, content_type: &str) -> anyhow::Result<()> {
        self.client
            .put_object()
            .bucket(&self.bucket)
            .key(key)
            .body(ByteStream::from(bytes))
            .content_type(content_type)
            .send()
            .await?;
        Ok(())
    }
}
```

- [ ] **Step 2: Write the magic-byte content sniffer + handler**

```rust
// backend/accounts-service/src/avatars/handlers.rs
use crate::avatars::storage::S3Storage;
use crate::error::AppError;
use axum::{extract::{Multipart, State}, http::HeaderMap, Json};
use serde::Serialize;

const MAX_AVATAR_BYTES: usize = 5 * 1024 * 1024;

#[derive(Clone)]
pub struct AvatarState {
    pub storage: S3Storage,
    pub jwt_secret: String,
    pub public_base_url: String,
}

fn sniff_image_type(bytes: &[u8]) -> Option<&'static str> {
    if bytes.starts_with(&[0x89, 0x50, 0x4E, 0x47]) {
        Some("image/png")
    } else if bytes.starts_with(&[0xFF, 0xD8, 0xFF]) {
        Some("image/jpeg")
    } else if bytes.len() > 12 && &bytes[8..12] == b"WEBP" {
        Some("image/webp")
    } else {
        None
    }
}

fn account_id_from_auth_header(headers: &HeaderMap, secret: &str) -> Result<uuid::Uuid, AppError> {
    let header = headers
        .get(axum::http::header::AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .ok_or(AppError::Unauthorized)?;
    let token = header.strip_prefix("Bearer ").ok_or(AppError::Unauthorized)?;
    let claims = crate::auth::jwt::verify_token(token, secret).ok_or(AppError::Unauthorized)?;
    claims.sub.parse().map_err(|_| AppError::Unauthorized)
}

#[derive(Serialize)]
pub struct AvatarResponse {
    pub avatar_url: String,
}

pub async fn upload(
    State(state): State<AvatarState>,
    headers: HeaderMap,
    mut multipart: Multipart,
) -> Result<Json<AvatarResponse>, AppError> {
    let account_id = account_id_from_auth_header(&headers, &state.jwt_secret)?;

    let field = multipart
        .next_field()
        .await
        .map_err(|e| AppError::Validation(e.to_string()))?
        .ok_or_else(|| AppError::Validation("missing file field".into()))?;

    let bytes = field
        .bytes()
        .await
        .map_err(|e| AppError::Validation(e.to_string()))?;

    if bytes.len() > MAX_AVATAR_BYTES {
        return Err(AppError::Validation("file too large (max 5MB)".into()));
    }

    let content_type = sniff_image_type(&bytes)
        .ok_or_else(|| AppError::Validation("unsupported image format".into()))?;

    let key = format!("avatars/{account_id}.bin");
    state
        .storage
        .put(&key, bytes.to_vec(), content_type)
        .await
        .map_err(AppError::Internal)?;

    Ok(Json(AvatarResponse {
        avatar_url: format!("{}/{}", state.public_base_url, key),
    }))
}
```

- [ ] **Step 3: Write the module file**

```rust
// backend/accounts-service/src/avatars/mod.rs
pub mod storage;
pub mod handlers;
```

- [ ] **Step 4: Write the failing test**

```rust
// backend/accounts-service/tests/avatar_upload.rs
use axum_test::TestServer;
use serde_json::json;
use uuid::Uuid;

async fn test_pool() -> sqlx::PgPool {
    let url = std::env::var("DATABASE_URL")
        .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
    let pool = sqlx::PgPool::connect(&url).await.expect("connect");
    sqlx::migrate!("../migrations").run(&pool).await.expect("migrate");
    pool
}

// 1x1 red pixel PNG
const TINY_PNG: &[u8] = &[
    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
    0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, 0x08, 0xD7, 0x63, 0xF8, 0xCF, 0xC0, 0x00,
    0x00, 0x03, 0x01, 0x01, 0x00, 0x18, 0xDD, 0x8D, 0xB0, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
    0x44, 0xAE, 0x42, 0x60, 0x82,
];

#[tokio::test]
async fn upload_valid_png_returns_avatar_url() {
    let pool = test_pool().await;
    let app = accounts_service::build_app(pool.clone(), "test-secret".into()).await;
    let server = TestServer::new(app);
    let email = format!("avatar-{}@example.com", Uuid::new_v4());

    server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "password123", "nick": "Rider" }))
        .await
        .assert_status(axum::http::StatusCode::CREATED);
    let login: serde_json::Value = server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "password123" }))
        .await
        .json();
    let access_token = login["access_token"].as_str().unwrap();

    let response = server
        .post("/avatars")
        .add_header(
            axum::http::header::AUTHORIZATION,
            format!("Bearer {access_token}"),
        )
        .multipart(
            axum_test::multipart::MultipartForm::new().add_part(
                "file",
                axum_test::multipart::Part::bytes(TINY_PNG.to_vec())
                    .file_name("avatar.png")
                    .mime_type("image/png"),
            ),
        )
        .await;
    response.assert_status_ok();
    let body: serde_json::Value = response.json();
    assert!(body["avatar_url"].as_str().unwrap().contains("avatars/"));

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn upload_non_image_bytes_returns_400() {
    let pool = test_pool().await;
    let app = accounts_service::build_app(pool.clone(), "test-secret".into()).await;
    let server = TestServer::new(app);
    let email = format!("badavatar-{}@example.com", Uuid::new_v4());

    server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "password123", "nick": "Rider" }))
        .await
        .assert_status(axum::http::StatusCode::CREATED);
    let login: serde_json::Value = server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "password123" }))
        .await
        .json();
    let access_token = login["access_token"].as_str().unwrap();

    let response = server
        .post("/avatars")
        .add_header(
            axum::http::header::AUTHORIZATION,
            format!("Bearer {access_token}"),
        )
        .multipart(
            axum_test::multipart::MultipartForm::new().add_part(
                "file",
                axum_test::multipart::Part::bytes(b"not an image".to_vec())
                    .file_name("fake.png")
                    .mime_type("image/png"),
            ),
        )
        .await;
    response.assert_status(axum::http::StatusCode::BAD_REQUEST);

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}
```

- [ ] **Step 5: Wire the route, make `build_app` async (S3 client setup is async) and thread config through**

```rust
// backend/accounts-service/src/lib.rs
pub mod config;
pub mod error;
pub mod auth;
pub mod accounts;
pub mod device;
pub mod avatars;

use auth::handlers::AuthState;
use avatars::{handlers::AvatarState, storage::S3Storage};
use device::{handlers::DeviceState, store::DeviceStore};
use axum::{extract::DefaultBodyLimit, routing::{get, post}, Router};
use config::Config;

pub async fn build_app(pool: sqlx::PgPool, cfg: Config) -> Router {
    let auth_state = AuthState { pool: pool.clone(), jwt_secret: cfg.jwt_secret.clone() };
    let device_state = DeviceState { store: DeviceStore::default(), jwt_secret: cfg.jwt_secret.clone() };
    let storage = S3Storage::from_config(
        &cfg.s3_endpoint,
        &cfg.s3_access_key,
        &cfg.s3_secret_key,
        cfg.s3_bucket.clone(),
    )
    .await;
    let avatar_state = AvatarState {
        storage,
        jwt_secret: cfg.jwt_secret.clone(),
        public_base_url: cfg.s3_endpoint.clone(),
    };

    let auth_routes = Router::new()
        .route("/auth/register", post(auth::handlers::register))
        .route("/auth/login", post(auth::handlers::login))
        .with_state(auth_state);

    let device_routes = Router::new()
        .route("/device/code", post(device::handlers::create_code))
        .route("/device/confirm", post(device::handlers::confirm))
        .route("/device/token", post(device::handlers::token))
        .with_state(device_state);

    let avatar_routes = Router::new()
        .route("/avatars", post(avatars::handlers::upload))
        .layer(DefaultBodyLimit::max(6 * 1024 * 1024))
        .with_state(avatar_state);

    Router::new()
        .route("/health", get(|| async { "ok" }))
        .merge(auth_routes)
        .merge(device_routes)
        .merge(avatar_routes)
}
```

`build_app` is now `async` and takes `Config` instead of `(pool, jwt_secret)` — update every call site:
- `main.rs`: `let app = build_app(pool, cfg).await;`
- `tests/health.rs`, `tests/auth_flow.rs`, `tests/device_flow.rs`: build a `Config` with test values instead of passing `"test-secret".into()` directly, e.g.:

```rust
fn test_config() -> accounts_service::config::Config {
    accounts_service::config::Config {
        database_url: String::new(), // unused, pool is passed separately
        jwt_secret: "test-secret".into(),
        port: 0,
        s3_endpoint: "http://localhost:9000".into(),
        s3_bucket: "lovisual-avatars-test".into(),
        s3_access_key: "minioadmin".into(),
        s3_secret_key: "minioadmin".into(),
    }
}
```

and change every `accounts_service::build_app(pool.clone(), "test-secret".into())` to `accounts_service::build_app(pool.clone(), test_config()).await`.

- [ ] **Step 6: Create the test bucket once, manually**

```bash
docker exec lovisual-minio mkdir -p /data/lovisual-avatars-test
```

- [ ] **Step 7: Run all tests**

Run: `cd /storage/project/jvm/LoVisual/backend/accounts-service && DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/accounts_db S3_BUCKET=lovisual-avatars-test cargo test`
Expected: all pass

- [ ] **Step 8: Commit**

```bash
cd /storage/project/jvm/LoVisual
git add backend/accounts-service
git commit -m "feat(backend): avatar upload with magic-byte validation and S3 storage"
```

---

## What this plan does not cover (deliberately, next plans)

- **API Gateway** (routing, single JWT check point, rate limiting per `TODO.md` Фаза 10 §6) — separate plan, depends on this one existing.
- **`configs-service`** (4 config slots, share codes, showcase) — separate plan, same pattern as this one.
- **React/Vite/Tailwind frontend** — separate plan, depends on the gateway plan for a stable API surface.
- **Refresh-token rotation/revocation and `device_links` persistence** (currently `/device/token` issues a JWT but never writes a row to the `device_links` table, so the "revoke a device" admin feature from `TODO.md` has nothing to revoke yet) — flagged here so it isn't forgotten; fold it into the gateway plan's auth-hardening pass, since revocation checks belong at the point every request is authenticated (the gateway), not duplicated in each service.
