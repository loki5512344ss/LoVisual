# Backend: API Gateway + auth hardening (Подсистема 1, часть 2) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** One public entry point (`gateway`) that authenticates every request once (site JWT or mod device token), rate-limits it, and proxies it to internal services — plus the accounts-service changes that make that safe: refresh-token rotation with an httpOnly cookie, persistent revocable device links, a gRPC `AuthenticateDevice` call, and `GET /me` for the site.

**Architecture:** The gateway is an Axum reverse proxy. External API = REST/JSON (site + mod). Internal: the gateway forwards REST to each service's HTTP port with two trusted headers (`x-lovisual-internal-key`, `x-lovisual-account-id`) and calls accounts-service over **gRPC (tonic)** to resolve device tokens. Services reject any request without the internal key, so they are useless if reached directly. Shared contract code (JWT, internal headers, protobuf) lives in a new `backend/common` crate — its second real consumer (the gateway) now exists, per `backend/STRUCTURE.md`.

**Why REST forwarding + gRPC, not "everything over gRPC":** accounts-service (and configs-service, next plan) already speak REST with typed Axum handlers and tests. Re-declaring every endpoint twice (REST in gateway, gRPC in service) doubles the surface for zero gain. gRPC is used where there is a real service-to-service call with no public REST equivalent: device-token resolution now, friend/presence lookups in Подсистема 2 later.

**Tech Stack:** Rust 1.97 edition 2024, Axum 0.8, tonic/tonic-prost/tonic-prost-build 0.14 + prost 0.14 (requires `protoc` on PATH — present, libprotoc 36.1), reqwest 0.13 (`default-features = false, features = ["stream"]`), governor 0.10 (keyed rate limiters), tower-http 0.7 (cors), axum-extra 0.12 (`cookie`), time 0.3, sha2 0.11 + hex 0.4, base64 0.22, rand 0.10, subtle 2. **All verified by compiling spikes on 2026-09-24** (tonic server+client with interceptors, governor keyed `check_key` + `wait_time_from`, cookie builder, sha2/hex, `rand::rng().fill`).

## Global Constraints

- Everything in `backend/PLAN.md`'s Global Constraints still applies (no unwrap on request data, argon2id, TDD, ≤250 lines/file, ≤4 files per folder — subfolders don't count, no God-structs, no blocking I/O in async).
- Always build with `CARGO_BUILD_JOBS=4` (the dev machine ran out of memory at higher parallelism).
- Integration tests: `DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/accounts_db`, MinIO `http://localhost:9000` (both via SSH tunnels to the test VDS).
- Commits: English, conventional style, stage explicit paths only (`git add -A backend/...`), never anything under `mod/`.
- Header names are exact: `x-lovisual-internal-key`, `x-lovisual-account-id`. Device tokens start with `lvd_`, refresh tokens with `lvr_`. Refresh cookie: `lv_refresh`, `Path=/auth`, `HttpOnly`, `SameSite=Strict`, `Secure` unless `COOKIE_SECURE=false`.
- `INTERNAL_KEY` and `JWT_SECRET` are each ≥32 bytes; every binary refuses to start otherwise.

---

## File Structure

```
backend/
  Cargo.toml                    # members += "common", "gateway"
  .env.example                  # + INTERNAL_KEY, GRPC_PORT, COOKIE_SECURE, gateway vars
  common/
    Cargo.toml
    build.rs                    # compiles proto/accounts.proto
    proto/accounts.proto        # AccountsInternal.AuthenticateDevice
    src/
      lib.rs                    # pub mod jwt, internal; pub mod pb
      jwt.rs                    # (moved from accounts-service) Claims, issue/verify access token, bearer_token
      internal.rs               # header consts, require_internal_key middleware, GatewayIdentity, gRPC key interceptors
  accounts-service/
    migrations/
      0002_refresh_tokens.sql
      0003_device_links_token_unique.sql
    src/
      lib.rs                    # build_app wraps routes in require_internal_key
      main.rs                   # runs HTTP + gRPC servers
      config.rs                 # + internal_key, grpc_port, cookie_secure
      error.rs
      auth/{mod,password,handlers,tokens}.rs   # jwt.rs deleted; tokens.rs = opaque tokens + refresh storage
      device/{mod,handlers,store,links}.rs     # links.rs = device_links repo
      accounts/{mod,model,repo,handlers}.rs    # handlers.rs = GET /me
      avatars/...                              # unchanged except GatewayIdentity
      grpc/mod.rs               # AccountsInternal server impl
    tests/
      common/mod.rs             # + test_server(), register_account()
      auth_flow.rs  (+ auth_flow/refresh.rs)
      device_flow.rs (+ device_flow/links.rs)
      avatar_upload.rs
      smoke.rs
  gateway/
    Cargo.toml
    PLAN.md                     # this file
    src/
      main.rs
      lib.rs                    # build_app(&Config, Arc<dyn DeviceAuthenticator>) -> Router
      config.rs
      proxy/{mod,routes,forward}.rs
      identity/{mod,device}.rs
      rate_limit/{mod,rules}.rs
    tests/
      common/mod.rs             # fake upstream echo server, test_config
      proxy.rs
      identity.rs
      rate_limit.rs
```

---

### Task 1: `common` crate — shared JWT, internal contract, protobuf

**Files:**
- Create: `backend/common/Cargo.toml`, `backend/common/build.rs`, `backend/common/proto/accounts.proto`
- Create: `backend/common/src/lib.rs`, `backend/common/src/jwt.rs`, `backend/common/src/internal.rs`
- Modify: `backend/Cargo.toml` (members), `backend/accounts-service/Cargo.toml` (dep `common = { path = "../common" }`)
- Modify: `backend/accounts-service/src/auth/jwt.rs` → keep only `bearer_account_id` + its tests, re-export the rest from `common::jwt`

**Interfaces:**
- Produces (`common::jwt`): `enum TokenType { Access, Refresh }`, `struct Claims { sub, exp, token_type }`, `issue_access_token(Uuid, &str) -> String`, `issue_refresh_token(Uuid, &str) -> String` (temporary — deleted in Task 4), `verify_token(&str, &str, TokenType) -> Option<Claims>`, `bearer_token(&HeaderMap) -> Option<&str>`.
- Produces (`common::internal`): `INTERNAL_KEY_HEADER = "x-lovisual-internal-key"`, `ACCOUNT_ID_HEADER = "x-lovisual-account-id"`, `DEVICE_TOKEN_PREFIX = "lvd_"`, `struct InternalKey` (`InternalKey::new(String)`), `async fn require_internal_key(State<InternalKey>, Request, Next) -> Response`, `struct GatewayIdentity { pub account_id: Uuid }` (Axum extractor), `struct GrpcKeyCheck`, `struct GrpcKeyAttach` (tonic interceptors).
- Produces (`common::pb::accounts`): tonic-generated `accounts_internal_server::{AccountsInternal, AccountsInternalServer}`, `accounts_internal_client::AccountsInternalClient`, `AuthenticateDeviceRequest { device_token }`, `AuthenticateDeviceReply { account_id }`.

- [ ] **Step 1: Create the crate files**

`backend/common/Cargo.toml`:
```toml
[package]
name = "common"
version = "0.1.0"
edition = "2024"

[dependencies]
axum = "0.8"
jsonwebtoken = { version = "11", default-features = false, features = ["rust_crypto"] }
serde = { version = "1", features = ["derive"] }
serde_json = "1"
chrono = "0.4"
uuid = { version = "1", features = ["v4", "serde"] }
subtle = "2"
tonic = "0.14"
tonic-prost = "0.14"
prost = "0.14"

[build-dependencies]
tonic-prost-build = "0.14"

[dev-dependencies]
tokio = { version = "1", features = ["macros", "rt-multi-thread"] }
axum-test = "21"
```

`backend/common/build.rs`:
```rust
fn main() -> Result<(), Box<dyn std::error::Error>> {
    tonic_prost_build::compile_protos("proto/accounts.proto")?;
    Ok(())
}
```

`backend/common/proto/accounts.proto`:
```proto
syntax = "proto3";
package accounts.v1;

// Internal-only API of accounts-service. Never exposed publicly; every call
// must carry the x-lovisual-internal-key metadata entry.
service AccountsInternal {
  // Resolves a mod's long-lived device token to its account and bumps
  // device_links.last_seen. UNAUTHENTICATED if the token is unknown/revoked.
  rpc AuthenticateDevice(AuthenticateDeviceRequest) returns (AuthenticateDeviceReply);
}

message AuthenticateDeviceRequest { string device_token = 1; }
message AuthenticateDeviceReply { string account_id = 1; }
```

`backend/common/src/lib.rs`:
```rust
pub mod internal;
pub mod jwt;

pub mod pb {
    pub mod accounts {
        tonic::include_proto!("accounts.v1");
    }
}
```

In `backend/Cargo.toml` set `members = ["accounts-service", "common"]`.

- [ ] **Step 2: Move the JWT core into `common/src/jwt.rs`**

Move from `accounts-service/src/auth/jwt.rs` verbatim: `TokenType`, `Claims`, `issue`, `issue_access_token`, `issue_refresh_token`, `verify_token` and their tests (every test except the `bearer_*` ones). Add the header parser (it replaces the ad-hoc parsing inside `bearer_account_id`):

```rust
use axum::http::{HeaderMap, header::AUTHORIZATION};

/// The raw token from `Authorization: Bearer <token>`, if the header is
/// present, valid ASCII, uses the Bearer scheme and is non-empty.
pub fn bearer_token(headers: &HeaderMap) -> Option<&str> {
    let value = headers.get(AUTHORIZATION)?.to_str().ok()?;
    let token = value.strip_prefix("Bearer ")?.trim();
    (!token.is_empty()).then_some(token)
}
```

Tests to add in `common/src/jwt.rs`:
```rust
#[test]
fn bearer_token_extracts_the_token() {
    let mut h = HeaderMap::new();
    h.insert(AUTHORIZATION, "Bearer abc.def".parse().unwrap());
    assert_eq!(bearer_token(&h), Some("abc.def"));
}

#[test]
fn bearer_token_rejects_missing_wrong_scheme_and_empty() {
    let mut h = HeaderMap::new();
    assert_eq!(bearer_token(&h), None);
    h.insert(AUTHORIZATION, "Basic abc".parse().unwrap());
    assert_eq!(bearer_token(&h), None);
    h.insert(AUTHORIZATION, "Bearer   ".parse().unwrap());
    assert_eq!(bearer_token(&h), None);
}
```

- [ ] **Step 3: Shrink `accounts-service/src/auth/jwt.rs`**

```rust
pub use common::jwt::*;

use crate::error::AppError;
use axum::http::HeaderMap;
use uuid::Uuid;

/// Account id from `Authorization: Bearer <access token>`. Every failure mode
/// is the same `AppError::Unauthorized`. (Deleted in Task 2 — the gateway
/// authenticates and services read `GatewayIdentity` instead.)
pub fn bearer_account_id(headers: &HeaderMap, secret: &str) -> Result<Uuid, AppError> {
    let token = bearer_token(headers).ok_or(AppError::Unauthorized)?;
    let claims = verify_token(token, secret, TokenType::Access).ok_or(AppError::Unauthorized)?;
    Uuid::parse_str(&claims.sub).map_err(|_| AppError::Unauthorized)
}
```
Keep the three existing `bearer_*` tests below it unchanged. Remove `jsonwebtoken` from `accounts-service/Cargo.toml` only if nothing else in the crate uses it (`grep -rn jsonwebtoken backend/accounts-service/src`).

- [ ] **Step 4: Write `common/src/internal.rs` tests first**

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use axum::{Router, routing::get};
    use axum_test::TestServer;

    const KEY: &str = "internal-key-internal-key-internal!!";

    fn app() -> Router {
        Router::new()
            .route("/whoami", get(|id: GatewayIdentity| async move { id.account_id.to_string() }))
            .route("/open", get(|| async { "open" }))
            .layer(axum::middleware::from_fn_with_state(InternalKey::new(KEY.into()), require_internal_key))
    }

    #[tokio::test]
    async fn request_without_internal_key_is_forbidden() {
        let server = TestServer::new(app());
        server.get("/open").await.assert_status(axum::http::StatusCode::FORBIDDEN);
    }

    #[tokio::test]
    async fn request_with_wrong_internal_key_is_forbidden() {
        let server = TestServer::new(app());
        server.get("/open").add_header(INTERNAL_KEY_HEADER, "nope")
            .await.assert_status(axum::http::StatusCode::FORBIDDEN);
    }

    #[tokio::test]
    async fn correct_key_passes_and_identity_is_read() {
        let server = TestServer::new(app());
        let id = Uuid::new_v4();
        let res = server.get("/whoami")
            .add_header(INTERNAL_KEY_HEADER, KEY)
            .add_header(ACCOUNT_ID_HEADER, id.to_string())
            .await;
        res.assert_status_ok();
        assert_eq!(res.text(), id.to_string());
    }

    #[tokio::test]
    async fn missing_or_garbage_identity_is_401() {
        let server = TestServer::new(app());
        server.get("/whoami").add_header(INTERNAL_KEY_HEADER, KEY)
            .await.assert_status_unauthorized();
        server.get("/whoami").add_header(INTERNAL_KEY_HEADER, KEY)
            .add_header(ACCOUNT_ID_HEADER, "not-a-uuid")
            .await.assert_status_unauthorized();
    }

    #[test]
    fn grpc_key_check_accepts_only_the_right_key() {
        let mut check = GrpcKeyCheck::new(KEY);
        let mut attach = GrpcKeyAttach::new(KEY).unwrap();
        let ok = attach.call(tonic::Request::new(())).unwrap();
        assert!(check.call(ok).is_ok());
        assert!(check.call(tonic::Request::new(())).is_err());
    }
}
```

Run: `cd backend && CARGO_BUILD_JOBS=4 cargo test -p common` → FAIL (items not defined).

- [ ] **Step 5: Implement `common/src/internal.rs`**

```rust
//! Contract between the gateway and internal services. Services trust the
//! identity header ONLY because `require_internal_key` guarantees the request
//! came through the gateway (which strips client-supplied copies of both).

use axum::{
    Json,
    extract::{FromRequestParts, Request, State},
    http::{StatusCode, request::Parts},
    middleware::Next,
    response::{IntoResponse, Response},
};
use serde_json::json;
use std::sync::Arc;
use subtle::ConstantTimeEq;
use tonic::{Status, metadata::{Ascii, MetadataValue}, service::Interceptor};
use uuid::Uuid;

pub const INTERNAL_KEY_HEADER: &str = "x-lovisual-internal-key";
pub const ACCOUNT_ID_HEADER: &str = "x-lovisual-account-id";
pub const DEVICE_TOKEN_PREFIX: &str = "lvd_";

fn keys_match(given: &[u8], expected: &[u8]) -> bool {
    given.ct_eq(expected).into()
}

#[derive(Clone)]
pub struct InternalKey(Arc<str>);

impl InternalKey {
    pub fn new(key: String) -> Self {
        InternalKey(key.into())
    }
}

pub async fn require_internal_key(State(key): State<InternalKey>, req: Request, next: Next) -> Response {
    let ok = req
        .headers()
        .get(INTERNAL_KEY_HEADER)
        .is_some_and(|v| keys_match(v.as_bytes(), key.0.as_bytes()));
    if !ok {
        return (StatusCode::FORBIDDEN, Json(json!({ "error": "forbidden" }))).into_response();
    }
    next.run(req).await
}

/// The authenticated account, as resolved by the gateway. Rejects with 401
/// when the gateway forwarded the request anonymously.
pub struct GatewayIdentity {
    pub account_id: Uuid,
}

impl<S: Send + Sync> FromRequestParts<S> for GatewayIdentity {
    type Rejection = Response;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        parts
            .headers
            .get(ACCOUNT_ID_HEADER)
            .and_then(|v| v.to_str().ok())
            .and_then(|s| Uuid::parse_str(s).ok())
            .map(|account_id| GatewayIdentity { account_id })
            .ok_or_else(|| {
                (StatusCode::UNAUTHORIZED, Json(json!({ "error": "unauthorized" }))).into_response()
            })
    }
}

/// Server-side tonic interceptor: rejects calls without the internal key.
#[derive(Clone)]
pub struct GrpcKeyCheck(Arc<str>);

impl GrpcKeyCheck {
    pub fn new(key: &str) -> Self {
        GrpcKeyCheck(key.into())
    }
}

impl Interceptor for GrpcKeyCheck {
    fn call(&mut self, req: tonic::Request<()>) -> Result<tonic::Request<()>, Status> {
        match req.metadata().get(INTERNAL_KEY_HEADER) {
            Some(v) if keys_match(v.as_bytes(), self.0.as_bytes()) => Ok(req),
            _ => Err(Status::permission_denied("missing or invalid internal key")),
        }
    }
}

/// Client-side tonic interceptor: attaches the internal key to every call.
#[derive(Clone)]
pub struct GrpcKeyAttach(MetadataValue<Ascii>);

impl GrpcKeyAttach {
    pub fn new(key: &str) -> Result<Self, tonic::metadata::errors::InvalidMetadataValue> {
        Ok(GrpcKeyAttach(MetadataValue::try_from(key)?))
    }
}

impl Interceptor for GrpcKeyAttach {
    fn call(&mut self, mut req: tonic::Request<()>) -> Result<tonic::Request<()>, Status> {
        req.metadata_mut().insert(INTERNAL_KEY_HEADER, self.0.clone());
        Ok(req)
    }
}
```

- [ ] **Step 6: Run everything**

Run: `cd backend && CARGO_BUILD_JOBS=4 cargo test -p common && DATABASE_URL=postgres://lovisual:lovisual@localhost:5432/accounts_db CARGO_BUILD_JOBS=4 cargo test -p accounts-service`
Expected: all pass (accounts-service suite unchanged in behavior).

- [ ] **Step 7: Commit**

```bash
git add -A backend/Cargo.toml backend/Cargo.lock backend/common backend/accounts-service
git commit -m "feat(backend): add common crate with shared JWT, internal gateway contract and accounts proto"
```

---

### Task 2: accounts-service only accepts gateway traffic

**Files:**
- Modify: `backend/accounts-service/src/config.rs` (+ `internal_key`, validate ≥32 bytes)
- Modify: `backend/accounts-service/src/lib.rs` (wrap all routes except `/health` in `require_internal_key`)
- Modify: `backend/accounts-service/src/device/handlers.rs`, `src/avatars/handlers.rs` (use `GatewayIdentity`)
- Delete: `backend/accounts-service/src/auth/jwt.rs` (`auth/mod.rs` drops `pub mod jwt;`; callers import `common::jwt` directly)
- Modify: `backend/.env.example`, `tests/common/mod.rs`, all integration tests

**Interfaces:**
- Consumes: `common::internal::{InternalKey, require_internal_key, GatewayIdentity, INTERNAL_KEY_HEADER, ACCOUNT_ID_HEADER}`.
- Produces: `Config.internal_key: String` (env `INTERNAL_KEY`); tests/common `TEST_INTERNAL_KEY`, `test_server(Router) -> TestServer` (adds the key to every request), `register_account(&TestServer) -> (Uuid, String /*email*/)`.

- [ ] **Step 1: Config — failing tests first**

Add to `config.rs` tests (and `internal_key` to the `config_with_secret` helper, set to `"k".repeat(32)`):
```rust
#[test]
fn short_internal_key_is_rejected() {
    let mut cfg = config_with_secret(&"x".repeat(32));
    cfg.internal_key = "short".into();
    assert!(cfg.validate().is_err());
}
```
Then add the field (`internal_key: std::env::var("INTERNAL_KEY").context("INTERNAL_KEY not set")?`) and in `validate`:
```rust
if self.internal_key.len() < MIN_JWT_SECRET_BYTES {
    anyhow::bail!("INTERNAL_KEY must be at least {MIN_JWT_SECRET_BYTES} bytes");
}
```
`.env.example`: add
```
# Shared secret between gateway and internal services (openssl rand -hex 32)
INTERNAL_KEY=
```

- [ ] **Step 2: Wire the middleware in `lib.rs`**

```rust
let api = Router::new()
    .merge(auth_routes)
    .merge(device_routes)
    .merge(avatar_routes) // whatever Task 9 of backend/PLAN.md named it
    .layer(axum::middleware::from_fn_with_state(
        common::internal::InternalKey::new(cfg.internal_key.clone()),
        common::internal::require_internal_key,
    ));

Router::new()
    .route("/health", get(|| async { "ok" }))
    .merge(api)
```
`/health` stays reachable without the key (used by orchestration probes; reveals nothing).

- [ ] **Step 3: Replace bearer parsing with `GatewayIdentity`**

`device/handlers.rs::confirm` becomes:
```rust
pub async fn confirm(
    State(state): State<DeviceState>,
    identity: GatewayIdentity,
    Json(req): Json<ConfirmRequest>,
) -> Result<StatusCode, AppError> {
    if state.store.confirm(&req.user_code, identity.account_id) {
        Ok(StatusCode::OK)
    } else {
        Err(AppError::NotFound("unknown or expired user_code".into()))
    }
}
```
Do the same in the avatar upload handler: remove the `HeaderMap` param and the `bearer_account_id(...)` call, add `identity: GatewayIdentity` (must come before the `Multipart` extractor, which consumes the body), use `identity.account_id`. Drop `jwt_secret` from `AvatarState` if nothing else reads it. `DeviceState.jwt_secret` stays until Task 4 (still used by `token`). Delete `src/auth/jwt.rs` and switch remaining imports (`auth::handlers`, `device::handlers`) to `common::jwt::...`.

- [ ] **Step 4: Test helpers**

Append to `tests/common/mod.rs` (and set `internal_key: TEST_INTERNAL_KEY.into()` in `test_config()`):
```rust
use axum_test::TestServer;
use common::internal::INTERNAL_KEY_HEADER;
use uuid::Uuid;

pub const TEST_INTERNAL_KEY: &str = "internal-key-internal-key-internal!!";

/// A server that behaves like it sits behind the gateway.
pub fn test_server(app: axum::Router) -> TestServer {
    let mut server = TestServer::new(app);
    server.add_header(INTERNAL_KEY_HEADER, TEST_INTERNAL_KEY);
    server
}

/// Registers a fresh random account; returns its id and email.
pub async fn register_account(server: &TestServer) -> (Uuid, String) {
    let email = format!("t-{}@example.com", Uuid::new_v4());
    let res = server
        .post("/auth/register")
        .json(&serde_json::json!({ "email": email, "password": "correct-horse-battery-staple", "nick": "Tester" }))
        .await;
    res.assert_status(axum::http::StatusCode::CREATED);
    let body: serde_json::Value = res.json();
    (Uuid::parse_str(body["id"].as_str().unwrap()).unwrap(), email)
}
```

- [ ] **Step 5: Migrate integration tests**

Every `TestServer::new(app)` → `common::test_server(app)`. Where a test logged in only to get an access token for `/device/confirm` or `/avatars`, it now sends `.add_header(ACCOUNT_ID_HEADER, id.to_string())` with the id from `register_account`. `refresh_token_cannot_confirm_a_device_code` is replaced by:
```rust
#[tokio::test]
async fn confirm_without_identity_is_401() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let code: serde_json::Value = server.post("/device/code").await.json();
    server.post("/device/confirm")
        .json(&serde_json::json!({ "user_code": code["user_code"] }))
        .await
        .assert_status_unauthorized();
}
```
Add to `smoke.rs`:
```rust
#[tokio::test]
async fn api_routes_require_internal_key_but_health_does_not() {
    let pool = common::test_pool().await;
    let server = axum_test::TestServer::new(accounts_service::build_app(pool, &common::test_config()));
    server.get("/health").await.assert_status_ok();
    server.post("/device/code").await.assert_status(axum::http::StatusCode::FORBIDDEN);
}
```

- [ ] **Step 6: Run full suite, then commit**

Run: `DATABASE_URL=... CARGO_BUILD_JOBS=4 cargo test -p accounts-service` → all pass.
```bash
git add -A backend/accounts-service backend/.env.example backend/Cargo.lock
git commit -m "feat(accounts): accept only gateway traffic, read identity from gateway header"
```

---

### Task 3: Refresh tokens — rotation, httpOnly cookie, logout

**Files:**
- Create: `backend/accounts-service/migrations/0002_refresh_tokens.sql`
- Create: `backend/accounts-service/src/auth/tokens.rs`
- Modify: `src/auth/handlers.rs` (login sets cookie; `refresh`, `logout`), `src/auth/mod.rs`, `src/lib.rs`, `src/config.rs` (+ `cookie_secure`), `Cargo.toml` (+ `axum-extra = { version = "0.12", features = ["cookie"] }`, `time = "0.3"`, `sha2 = "0.11"`, `hex = "0.4"`, `base64 = "0.22"`)
- Create: `tests/auth_flow/refresh.rs` (declared via `mod refresh;` at the top of `tests/auth_flow.rs` — keeps `tests/` at 4 files)

**Interfaces:**
- Produces (`auth::tokens`): `new_opaque_token(prefix: &str) -> String` (prefix + 43-char base64url of 32 random bytes), `hash_token(&str) -> String` (hex SHA-256), `store_refresh(&PgPool, Uuid) -> Result<String, sqlx::Error>`, `enum RotateOutcome { Rotated { account_id: Uuid, new_token: String }, Invalid }`, `rotate_refresh(&PgPool, &str) -> Result<RotateOutcome, sqlx::Error>`, `revoke_refresh(&PgPool, &str) -> Result<(), sqlx::Error>`.
- Produces (HTTP): `POST /auth/login` → `200 {access_token}` + `Set-Cookie: lv_refresh=...`; `POST /auth/refresh` (cookie) → `200 {access_token}` + rotated cookie, or 401; `POST /auth/logout` → 204 + cookie removal. `LoginResponse` loses `refresh_token`.
- Produces: `Config.cookie_secure: bool` (env `COOKIE_SECURE`, default `true`); `AuthState::new(pool, jwt_secret, cookie_secure)`.

SHA-256 (not Argon2) is correct here: the tokens are 256-bit random, so there is nothing to brute-force; a slow hash would only add latency to every refresh.

- [ ] **Step 1: Migration**

```sql
-- Opaque refresh tokens (stored hashed). Rotated on every use; presenting an
-- already-rotated token revokes the whole account's sessions (theft signal).
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens(account_id);
```

- [ ] **Step 2: Failing unit tests in `auth/tokens.rs`**

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn opaque_tokens_are_prefixed_unique_and_long() {
        let a = new_opaque_token("lvr_");
        let b = new_opaque_token("lvr_");
        assert!(a.starts_with("lvr_"));
        assert_eq!(a.len(), 4 + 43);
        assert_ne!(a, b);
    }

    #[test]
    fn hash_is_stable_hex_sha256() {
        assert_eq!(hash_token("x"), hash_token("x"));
        assert_eq!(hash_token("x").len(), 64);
        assert_ne!(hash_token("x"), hash_token("y"));
    }
}
```
Run: `cargo test -p accounts-service --lib tokens` → FAIL.

- [ ] **Step 3: Implement `auth/tokens.rs`**

```rust
use base64::{Engine, engine::general_purpose::URL_SAFE_NO_PAD};
use rand::RngExt;
use sha2::{Digest, Sha256};
use sqlx::PgPool;
use uuid::Uuid;

pub fn new_opaque_token(prefix: &str) -> String {
    let mut bytes = [0u8; 32];
    rand::rng().fill(&mut bytes);
    format!("{prefix}{}", URL_SAFE_NO_PAD.encode(bytes))
}

pub fn hash_token(token: &str) -> String {
    hex::encode(Sha256::digest(token.as_bytes()))
}

pub async fn store_refresh(pool: &PgPool, account_id: Uuid) -> Result<String, sqlx::Error> {
    let token = new_opaque_token("lvr_");
    sqlx::query(
        "INSERT INTO refresh_tokens (account_id, token_hash, expires_at)
         VALUES ($1, $2, now() + interval '30 days')",
    )
    .bind(account_id)
    .bind(hash_token(&token))
    .execute(pool)
    .await?;
    Ok(token)
}

pub enum RotateOutcome {
    Rotated { account_id: Uuid, new_token: String },
    Invalid,
}

pub async fn rotate_refresh(pool: &PgPool, token: &str) -> Result<RotateOutcome, sqlx::Error> {
    let mut tx = pool.begin().await?;
    let row: Option<(Uuid, bool, bool)> = sqlx::query_as(
        "SELECT account_id, revoked_at IS NOT NULL, expires_at <= now()
         FROM refresh_tokens WHERE token_hash = $1 FOR UPDATE",
    )
    .bind(hash_token(token))
    .fetch_optional(&mut *tx)
    .await?;

    let outcome = match row {
        None => RotateOutcome::Invalid,
        Some((account_id, true, _)) => {
            // Reuse of a rotated token: someone else holds a copy. Kill all sessions.
            sqlx::query("UPDATE refresh_tokens SET revoked_at = now() WHERE account_id = $1 AND revoked_at IS NULL")
                .bind(account_id)
                .execute(&mut *tx)
                .await?;
            RotateOutcome::Invalid
        }
        Some((_, false, true)) => RotateOutcome::Invalid,
        Some((account_id, false, false)) => {
            sqlx::query("UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = $1")
                .bind(hash_token(token))
                .execute(&mut *tx)
                .await?;
            let new_token = new_opaque_token("lvr_");
            sqlx::query(
                "INSERT INTO refresh_tokens (account_id, token_hash, expires_at)
                 VALUES ($1, $2, now() + interval '30 days')",
            )
            .bind(account_id)
            .bind(hash_token(&new_token))
            .execute(&mut *tx)
            .await?;
            RotateOutcome::Rotated { account_id, new_token }
        }
    };
    tx.commit().await?;
    Ok(outcome)
}

pub async fn revoke_refresh(pool: &PgPool, token: &str) -> Result<(), sqlx::Error> {
    sqlx::query("UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = $1 AND revoked_at IS NULL")
        .bind(hash_token(token))
        .execute(pool)
        .await?;
    Ok(())
}
```
Add `pub mod tokens;` to `auth/mod.rs`.

- [ ] **Step 4: Failing integration tests — `tests/auth_flow/refresh.rs`**

```rust
use super::common;
use axum::http::StatusCode;
use axum_extra::extract::cookie::Cookie;
use serde_json::json;

async fn login(server: &axum_test::TestServer, email: &str) -> (String, String) {
    let res = server.post("/auth/login")
        .json(&json!({ "email": email, "password": "correct-horse-battery-staple" }))
        .await;
    res.assert_status_ok();
    let cookie = res.cookie("lv_refresh");
    assert!(cookie.http_only().unwrap_or(false));
    assert_eq!(cookie.path(), Some("/auth"));
    let body: serde_json::Value = res.json();
    assert!(body.get("refresh_token").is_none(), "refresh token must not be in the JSON body");
    (body["access_token"].as_str().unwrap().to_owned(), cookie.value().to_owned())
}

#[tokio::test]
async fn refresh_rotates_the_cookie_and_issues_a_new_access_token() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (_, email) = common::register_account(&server).await;
    let (_, refresh) = login(&server, &email).await;

    let res = server.post("/auth/refresh").add_cookie(Cookie::new("lv_refresh", refresh.clone())).await;
    res.assert_status_ok();
    assert!(res.json::<serde_json::Value>()["access_token"].is_string());
    assert_ne!(res.cookie("lv_refresh").value(), refresh);
}

#[tokio::test]
async fn reusing_a_rotated_refresh_token_revokes_all_sessions() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (_, email) = common::register_account(&server).await;
    let (_, first) = login(&server, &email).await;

    let second = server.post("/auth/refresh").add_cookie(Cookie::new("lv_refresh", first.clone()))
        .await.cookie("lv_refresh").value().to_owned();
    // Attacker replays the old token -> rejected, and the legit new one dies too.
    server.post("/auth/refresh").add_cookie(Cookie::new("lv_refresh", first))
        .await.assert_status(StatusCode::UNAUTHORIZED);
    server.post("/auth/refresh").add_cookie(Cookie::new("lv_refresh", second))
        .await.assert_status(StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn logout_revokes_the_refresh_token() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (_, email) = common::register_account(&server).await;
    let (_, refresh) = login(&server, &email).await;

    server.post("/auth/logout").add_cookie(Cookie::new("lv_refresh", refresh.clone()))
        .await.assert_status(StatusCode::NO_CONTENT);
    server.post("/auth/refresh").add_cookie(Cookie::new("lv_refresh", refresh))
        .await.assert_status(StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn refresh_without_cookie_or_with_garbage_is_401() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    server.post("/auth/refresh").await.assert_status(StatusCode::UNAUTHORIZED);
    server.post("/auth/refresh").add_cookie(Cookie::new("lv_refresh", "lvr_garbage"))
        .await.assert_status(StatusCode::UNAUTHORIZED);
}
```
Add `axum-extra = { version = "0.12", features = ["cookie"] }` to `[dev-dependencies]` too if the test crate can't see the normal dependency (it can — integration tests see `[dependencies]`). In `test_config()` set `cookie_secure: false`. In `tests/auth_flow.rs` add `mod refresh;` after `mod common;`, and change `register_then_login_succeeds` to assert `body["refresh_token"].is_null()`.

Run: `cargo test -p accounts-service --test auth_flow` → FAIL (routes missing).

- [ ] **Step 5: Implement handlers**

In `auth/handlers.rs` (add `pub cookie_secure: bool` to `AuthState`, set in `AuthState::new(pool, jwt_secret, cookie_secure)`):
```rust
use axum_extra::extract::cookie::{Cookie, CookieJar, SameSite};

pub const REFRESH_COOKIE: &str = "lv_refresh";

fn refresh_cookie(token: String, secure: bool) -> Cookie<'static> {
    Cookie::build((REFRESH_COOKIE, token))
        .http_only(true)
        .secure(secure)
        .same_site(SameSite::Strict)
        .path("/auth")
        .max_age(time::Duration::days(30))
        .build()
}

#[derive(Serialize)]
pub struct LoginResponse {
    pub access_token: String,
}

pub async fn login(
    State(state): State<AuthState>,
    jar: CookieJar,
    Json(req): Json<LoginRequest>,
) -> Result<(CookieJar, Json<LoginResponse>), AppError> {
    // ... unchanged lookup + dummy-hash + verify ...
    let refresh = tokens::store_refresh(&state.pool, account.id).await?;
    Ok((
        jar.add(refresh_cookie(refresh, state.cookie_secure)),
        Json(LoginResponse { access_token: jwt::issue_access_token(account.id, &state.jwt_secret) }),
    ))
}

pub async fn refresh(
    State(state): State<AuthState>,
    jar: CookieJar,
) -> Result<(CookieJar, Json<LoginResponse>), AppError> {
    let token = jar.get(REFRESH_COOKIE).map(|c| c.value().to_owned()).ok_or(AppError::Unauthorized)?;
    match tokens::rotate_refresh(&state.pool, &token).await? {
        tokens::RotateOutcome::Rotated { account_id, new_token } => Ok((
            jar.add(refresh_cookie(new_token, state.cookie_secure)),
            Json(LoginResponse { access_token: jwt::issue_access_token(account_id, &state.jwt_secret) }),
        )),
        tokens::RotateOutcome::Invalid => Err(AppError::Unauthorized),
    }
}

pub async fn logout(State(state): State<AuthState>, jar: CookieJar) -> Result<(CookieJar, StatusCode), AppError> {
    if let Some(cookie) = jar.get(REFRESH_COOKIE) {
        tokens::revoke_refresh(&state.pool, cookie.value()).await?;
    }
    Ok((jar.remove(Cookie::build(REFRESH_COOKIE).path("/auth")), StatusCode::NO_CONTENT))
}
```
Routes in `lib.rs`: `.route("/auth/refresh", post(auth::handlers::refresh)).route("/auth/logout", post(auth::handlers::logout))`. Config: `cookie_secure: std::env::var("COOKIE_SECURE").map(|v| v != "false").unwrap_or(true)`; `.env.example`: `COOKIE_SECURE=false  # true in production (HTTPS)`.

`handlers.rs` is already ~227 lines (after the final-review fixes added `validate_register`, the Argon2 semaphore and `spawn_blocking` wrappers), so this task WILL cross 250. Before adding the new handlers: move the cookie helpers (`REFRESH_COOKIE`, `refresh_cookie`) into `auth/tokens.rs`, and move the bounded Argon2 wrappers (`hash_limiter` semaphore + the async `hash_password`/`verify_password` methods) into `auth/password.rs` as a `PasswordHasher` struct (`new() -> Self`, `async fn hash(&self, String)`, `async fn verify(&self, String, String)`) that `AuthState` holds. `auth/` then has exactly 4 files: `mod, password, handlers, tokens`.

- [ ] **Step 6: Run full suite, commit**

```bash
git add -A backend/accounts-service backend/.env.example backend/Cargo.lock
git commit -m "feat(accounts): rotating opaque refresh tokens in httpOnly cookie, /auth/refresh and /auth/logout"
```

---

### Task 4: Persistent, revocable device links

**Files:**
- Create: `backend/accounts-service/migrations/0003_device_links_token_unique.sql`
- Create: `backend/accounts-service/src/device/links.rs`
- Modify: `src/device/handlers.rs` (`token` persists a link; `list_links`, `revoke_link`), `src/device/mod.rs`, `src/lib.rs`
- Modify: `backend/common/src/jwt.rs` (delete `issue_refresh_token`, `TokenType::Refresh` and tests that only exist for it)
- Create: `tests/device_flow/links.rs` (declared via `mod links;` in `tests/device_flow.rs`)

**Interfaces:**
- Consumes: `auth::tokens::{new_opaque_token, hash_token}`, `common::internal::{GatewayIdentity, DEVICE_TOKEN_PREFIX}`.
- Produces (`device::links`): `struct DeviceLink { id: Uuid, linked_at: DateTime<Utc>, last_seen: Option<DateTime<Utc>> }` (Serialize, FromRow), `create(&PgPool, Uuid) -> Result<String, sqlx::Error>` (returns the plain `lvd_...` token), `authenticate(&PgPool, &str) -> Result<Option<Uuid>, sqlx::Error>` (bumps `last_seen`), `list(&PgPool, Uuid) -> Result<Vec<DeviceLink>, sqlx::Error>`, `revoke(&PgPool, account_id: Uuid, link_id: Uuid) -> Result<bool, sqlx::Error>`.
- Produces (HTTP): `POST /device/token` → `200 {device_token: "lvd_..."}` once confirmed; `GET /device/links` → `[{id, linked_at, last_seen}]`; `DELETE /device/links/{id}` → 204, or 404 if not yours.
- `DeviceState` becomes `{ store: DeviceStore, pool: PgPool }` (no `jwt_secret`).

- [ ] **Step 1: Migration**

```sql
-- Device tokens are looked up by hash on every mod request (via gateway gRPC).
CREATE UNIQUE INDEX device_links_token_hash_idx ON device_links (device_token_hash);
```

- [ ] **Step 2: Failing integration tests — `tests/device_flow/links.rs`**

```rust
use super::common;
use axum::http::StatusCode;
use common::ACCOUNT_ID_HEADER; // re-export: add `pub use common::internal::ACCOUNT_ID_HEADER;` in tests/common/mod.rs

async fn link_device(server: &axum_test::TestServer, account: uuid::Uuid) -> String {
    let code: serde_json::Value = server.post("/device/code").await.json();
    server.post("/device/confirm")
        .add_header(ACCOUNT_ID_HEADER, account.to_string())
        .json(&serde_json::json!({ "user_code": code["user_code"] }))
        .await.assert_status_ok();
    let res = server.post("/device/token").json(&serde_json::json!({ "device_code": code["device_code"] })).await;
    res.assert_status_ok();
    res.json::<serde_json::Value>()["device_token"].as_str().unwrap().to_owned()
}

#[tokio::test]
async fn confirmed_device_gets_an_opaque_token_backed_by_a_link_row() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool.clone(), &common::test_config()));
    let (account, _) = common::register_account(&server).await;
    let token = link_device(&server, account).await;
    assert!(token.starts_with("lvd_"));

    let resolved = accounts_service::device::links::authenticate(&pool, &token).await.unwrap();
    assert_eq!(resolved, Some(account));
    let links: Vec<serde_json::Value> = server.get("/device/links")
        .add_header(ACCOUNT_ID_HEADER, account.to_string()).await.json();
    assert_eq!(links.len(), 1);
    assert!(links[0]["last_seen"].is_string(), "authenticate must bump last_seen");
}

#[tokio::test]
async fn revoking_a_link_kills_its_token_only() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool.clone(), &common::test_config()));
    let (account, _) = common::register_account(&server).await;
    let t1 = link_device(&server, account).await;
    let t2 = link_device(&server, account).await;

    let links: Vec<serde_json::Value> = server.get("/device/links")
        .add_header(ACCOUNT_ID_HEADER, account.to_string()).await.json();
    let first_id = links.iter()
        .find(|l| l["id"].is_string())
        .unwrap()["id"].as_str().unwrap().to_owned();
    server.delete(&format!("/device/links/{first_id}"))
        .add_header(ACCOUNT_ID_HEADER, account.to_string())
        .await.assert_status(StatusCode::NO_CONTENT);

    let alive = [
        accounts_service::device::links::authenticate(&pool, &t1).await.unwrap(),
        accounts_service::device::links::authenticate(&pool, &t2).await.unwrap(),
    ];
    assert_eq!(alive.iter().filter(|a| a.is_some()).count(), 1);
}

#[tokio::test]
async fn cannot_revoke_someone_elses_link() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (owner, _) = common::register_account(&server).await;
    let (stranger, _) = common::register_account(&server).await;
    link_device(&server, owner).await;
    let links: Vec<serde_json::Value> = server.get("/device/links")
        .add_header(ACCOUNT_ID_HEADER, owner.to_string()).await.json();
    let id = links[0]["id"].as_str().unwrap();

    server.delete(&format!("/device/links/{id}"))
        .add_header(ACCOUNT_ID_HEADER, stranger.to_string())
        .await.assert_status(StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn unknown_device_token_does_not_authenticate() {
    let pool = common::test_pool().await;
    let r = accounts_service::device::links::authenticate(&pool, "lvd_nope").await.unwrap();
    assert_eq!(r, None);
}
```
Update `full_device_link_flow` in `tests/device_flow.rs` to assert `device_token` starts with `lvd_` instead of decoding it as a JWT. Add `mod links;`.

Run → FAIL.

- [ ] **Step 3: Implement `device/links.rs`**

```rust
use crate::auth::tokens::{hash_token, new_opaque_token};
use chrono::{DateTime, Utc};
use common::internal::DEVICE_TOKEN_PREFIX;
use serde::Serialize;
use sqlx::PgPool;
use uuid::Uuid;

#[derive(Debug, Serialize, sqlx::FromRow)]
pub struct DeviceLink {
    pub id: Uuid,
    pub linked_at: DateTime<Utc>,
    pub last_seen: Option<DateTime<Utc>>,
}

pub async fn create(pool: &PgPool, account_id: Uuid) -> Result<String, sqlx::Error> {
    let token = new_opaque_token(DEVICE_TOKEN_PREFIX);
    sqlx::query("INSERT INTO device_links (account_id, device_token_hash) VALUES ($1, $2)")
        .bind(account_id)
        .bind(hash_token(&token))
        .execute(pool)
        .await?;
    Ok(token)
}

pub async fn authenticate(pool: &PgPool, token: &str) -> Result<Option<Uuid>, sqlx::Error> {
    sqlx::query_scalar(
        "UPDATE device_links SET last_seen = now() WHERE device_token_hash = $1 RETURNING account_id",
    )
    .bind(hash_token(token))
    .fetch_optional(pool)
    .await
}

pub async fn list(pool: &PgPool, account_id: Uuid) -> Result<Vec<DeviceLink>, sqlx::Error> {
    sqlx::query_as("SELECT id, linked_at, last_seen FROM device_links WHERE account_id = $1 ORDER BY linked_at DESC")
        .bind(account_id)
        .fetch_all(pool)
        .await
}

pub async fn revoke(pool: &PgPool, account_id: Uuid, link_id: Uuid) -> Result<bool, sqlx::Error> {
    let result = sqlx::query("DELETE FROM device_links WHERE id = $1 AND account_id = $2")
        .bind(link_id)
        .bind(account_id)
        .execute(pool)
        .await?;
    Ok(result.rows_affected() == 1)
}
```
`device/mod.rs`: `pub mod handlers; pub mod links; pub mod store;`.

- [ ] **Step 4: Handlers + routes**

In `device/handlers.rs`:
```rust
PollResult::Confirmed(account_id) => {
    let device_token = links::create(&state.pool, account_id).await?;
    Ok((StatusCode::OK, Json(Some(TokenResponse { device_token }))))
}
```
```rust
pub async fn list_links(
    State(state): State<DeviceState>,
    identity: GatewayIdentity,
) -> Result<Json<Vec<links::DeviceLink>>, AppError> {
    Ok(Json(links::list(&state.pool, identity.account_id).await?))
}

pub async fn revoke_link(
    State(state): State<DeviceState>,
    identity: GatewayIdentity,
    Path(link_id): Path<Uuid>,
) -> Result<StatusCode, AppError> {
    if links::revoke(&state.pool, identity.account_id, link_id).await? {
        Ok(StatusCode::NO_CONTENT)
    } else {
        Err(AppError::NotFound("no such device link".into()))
    }
}
```
Routes: `.route("/device/links", get(device::handlers::list_links)).route("/device/links/{id}", delete(device::handlers::revoke_link))`. `DeviceState { store: DeviceStore::default(), pool: pool.clone() }`.

- [ ] **Step 5: Remove the dead refresh-JWT code from `common::jwt`**

Delete `issue_refresh_token`, the `Refresh` variant and every test that used them (`grep -rn "Refresh\b\|issue_refresh_token" backend/` must return nothing outside comments). Keep the `token_type` claim (value always `"access"`) so future token kinds stay distinguishable.

- [ ] **Step 6: Full suite (`-p common` and `-p accounts-service`), commit**

```bash
git add -A backend/common backend/accounts-service backend/Cargo.lock
git commit -m "feat(accounts): persist device links with opaque hashed tokens, list and revoke endpoints"
```

---

### Task 5: accounts-service gRPC server (`AuthenticateDevice`)

**Files:**
- Create: `backend/accounts-service/src/grpc/mod.rs`
- Modify: `src/lib.rs` (`pub mod grpc;`), `src/main.rs` (serve HTTP + gRPC), `src/config.rs` (+ `grpc_port`, env `GRPC_PORT`, default `50051`), `Cargo.toml` (+ `tonic = "0.14"`, `tokio-stream = { version = "0.1", features = ["net"] }` in dev-deps), `backend/.env.example` (`GRPC_PORT=50051`)

**Interfaces:**
- Consumes: `device::links::authenticate`, `common::pb::accounts::*`, `common::internal::GrpcKeyCheck`.
- Produces: `grpc::AccountsGrpc::new(PgPool)`, `grpc::server(PgPool, &str /*internal key*/) -> InterceptedService<AccountsInternalServer<AccountsGrpc>, GrpcKeyCheck>`.

- [ ] **Step 1: Failing test (inside `src/grpc/mod.rs`, real DB, real network)**

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use common::internal::GrpcKeyAttach;
    use common::pb::accounts::accounts_internal_client::AccountsInternalClient;

    const KEY: &str = "internal-key-internal-key-internal!!";

    async fn pool() -> PgPool {
        let url = std::env::var("DATABASE_URL")
            .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
        let pool = PgPool::connect(&url).await.expect("connect");
        sqlx::migrate!("./migrations").run(&pool).await.expect("migrate");
        pool
    }

    #[tokio::test]
    async fn authenticate_device_over_grpc() {
        let pool = pool().await;
        let account = crate::accounts::repo::create(
            &pool, &format!("g-{}@example.com", Uuid::new_v4()), "x", "Grpc",
        ).await.unwrap();
        let token = crate::device::links::create(&pool, account.id).await.unwrap();

        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let addr = listener.local_addr().unwrap();
        tokio::spawn(
            tonic::transport::Server::builder()
                .add_service(server(pool.clone(), KEY))
                .serve_with_incoming(tokio_stream::wrappers::TcpListenerStream::new(listener)),
        );
        let channel = tonic::transport::Endpoint::from_shared(format!("http://{addr}")).unwrap().connect_lazy();
        let mut client = AccountsInternalClient::with_interceptor(channel.clone(), GrpcKeyAttach::new(KEY).unwrap());

        let reply = client
            .authenticate_device(AuthenticateDeviceRequest { device_token: token })
            .await.unwrap().into_inner();
        assert_eq!(reply.account_id, account.id.to_string());

        let err = client
            .authenticate_device(AuthenticateDeviceRequest { device_token: "lvd_unknown".into() })
            .await.unwrap_err();
        assert_eq!(err.code(), tonic::Code::Unauthenticated);

        let mut no_key = AccountsInternalClient::new(channel);
        let err = no_key
            .authenticate_device(AuthenticateDeviceRequest { device_token: "x".into() })
            .await.unwrap_err();
        assert_eq!(err.code(), tonic::Code::PermissionDenied);

        sqlx::query("DELETE FROM accounts WHERE id = $1").bind(account.id).execute(&pool).await.unwrap();
    }
}
```

- [ ] **Step 2: Implement**

```rust
use common::internal::GrpcKeyCheck;
use common::pb::accounts::accounts_internal_server::{AccountsInternal, AccountsInternalServer};
use common::pb::accounts::{AuthenticateDeviceReply, AuthenticateDeviceRequest};
use sqlx::PgPool;
use tonic::{Request, Response, Status, service::interceptor::InterceptedService};
use uuid::Uuid;

pub struct AccountsGrpc {
    pool: PgPool,
}

impl AccountsGrpc {
    pub fn new(pool: PgPool) -> Self {
        AccountsGrpc { pool }
    }
}

pub fn server(pool: PgPool, internal_key: &str) -> InterceptedService<AccountsInternalServer<AccountsGrpc>, GrpcKeyCheck> {
    AccountsInternalServer::with_interceptor(AccountsGrpc::new(pool), GrpcKeyCheck::new(internal_key))
}

#[tonic::async_trait]
impl AccountsInternal for AccountsGrpc {
    async fn authenticate_device(
        &self,
        request: Request<AuthenticateDeviceRequest>,
    ) -> Result<Response<AuthenticateDeviceReply>, Status> {
        let token = request.into_inner().device_token;
        match crate::device::links::authenticate(&self.pool, &token).await {
            Ok(Some(account_id)) => Ok(Response::new(AuthenticateDeviceReply { account_id: account_id.to_string() })),
            Ok(None) => Err(Status::unauthenticated("unknown or revoked device token")),
            Err(err) => {
                tracing::error!("authenticate_device: {err:?}");
                Err(Status::internal("internal error"))
            }
        }
    }
}
```
(`Uuid` import only in tests if unused otherwise.)

- [ ] **Step 3: `main.rs` runs both servers**

```rust
let http = tokio::net::TcpListener::bind(("0.0.0.0", cfg.port)).await?;
let grpc_addr = std::net::SocketAddr::from(([0, 0, 0, 0], cfg.grpc_port));
let grpc = tonic::transport::Server::builder()
    .add_service(accounts_service::grpc::server(pool.clone(), &cfg.internal_key))
    .serve(grpc_addr);
let app = accounts_service::build_app(pool, &cfg);
tokio::try_join!(
    async { axum::serve(http, app).await.map_err(anyhow::Error::from) },
    async { grpc.await.map_err(anyhow::Error::from) },
)?;
```

- [ ] **Step 4: Tests pass; commit**

```bash
git add -A backend/accounts-service backend/.env.example backend/Cargo.lock
git commit -m "feat(accounts): internal gRPC AuthenticateDevice guarded by internal key"
```

---

### Task 6: `GET /me` for the site

**Files:**
- Create: `backend/accounts-service/src/accounts/handlers.rs`
- Modify: `src/accounts/mod.rs`, `src/accounts/repo.rs` (+ `avatar_key`), `src/lib.rs`, `tests/auth_flow.rs`

**Interfaces:**
- Produces: `GET /me` → `200 { id, email, display_nick, role, avatar_url: string|null, created_at }`; 401 without identity. `repo::avatar_key(&PgPool, Uuid) -> Result<Option<String>, sqlx::Error>`. `AccountsState { pool, avatar_base_url: String }` (from `cfg.avatar_base_url()` added in backend/PLAN.md Task 9).

- [ ] **Step 1: Failing test in `tests/auth_flow.rs`**

```rust
#[tokio::test]
async fn me_returns_profile_without_password_hash() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (id, email) = common::register_account(&server).await;

    let res = server.get("/me").add_header(common::ACCOUNT_ID_HEADER, id.to_string()).await;
    res.assert_status_ok();
    let body: serde_json::Value = res.json();
    assert_eq!(body["email"], email);
    assert_eq!(body["display_nick"], "Tester");
    assert_eq!(body["role"], "user");
    assert!(body["avatar_url"].is_null());
    assert!(body.get("password_hash").is_none());

    server.get("/me").await.assert_status_unauthorized();
}
```

- [ ] **Step 2: Implement**

`repo.rs`:
```rust
pub async fn avatar_key(pool: &PgPool, account_id: Uuid) -> Result<Option<String>, sqlx::Error> {
    sqlx::query_scalar("SELECT s3_key FROM avatars WHERE account_id = $1")
        .bind(account_id)
        .fetch_optional(pool)
        .await
}
```
`accounts/handlers.rs`:
```rust
use super::repo;
use crate::error::AppError;
use axum::{Json, extract::State};
use chrono::{DateTime, Utc};
use common::internal::GatewayIdentity;
use serde::Serialize;
use uuid::Uuid;

#[derive(Clone)]
pub struct AccountsState {
    pub pool: sqlx::PgPool,
    pub avatar_base_url: String,
}

#[derive(Serialize)]
pub struct MeResponse {
    pub id: Uuid,
    pub email: String,
    pub display_nick: String,
    pub role: String,
    pub avatar_url: Option<String>,
    pub created_at: DateTime<Utc>,
}

pub async fn me(State(state): State<AccountsState>, identity: GatewayIdentity) -> Result<Json<MeResponse>, AppError> {
    let account = repo::find_by_id(&state.pool, identity.account_id)
        .await?
        .ok_or(AppError::Unauthorized)?;
    let avatar_url = repo::avatar_key(&state.pool, account.id)
        .await?
        .map(|key| format!("{}/{key}", state.avatar_base_url));
    Ok(Json(MeResponse {
        id: account.id,
        email: account.email,
        display_nick: account.display_nick,
        role: account.role,
        avatar_url,
        created_at: account.created_at,
    }))
}
```
Route: `.route("/me", get(accounts::handlers::me))` with `AccountsState`, inside the internal-key-guarded `api` router. Add `pub use common::internal::ACCOUNT_ID_HEADER;` to `tests/common/mod.rs` if Task 4 didn't already.

- [ ] **Step 3: Tests pass; commit**

```bash
git add -A backend/accounts-service
git commit -m "feat(accounts): GET /me profile endpoint"
```

---

### Task 7: Gateway scaffold — config, health, `build_app`

**Files:**
- Create: `backend/gateway/Cargo.toml`, `src/main.rs`, `src/lib.rs`, `src/config.rs`, `tests/common/mod.rs`, `tests/proxy.rs` (health test only for now)
- Modify: `backend/Cargo.toml` (members += "gateway"), `backend/.env.example`

**Interfaces:**
- Produces: `Config { port: u16, jwt_secret, internal_key, accounts_http_url, accounts_grpc_url, configs_http_url, site_origin: String, trust_proxy: bool }`, `Config::from_env()`, `Config::validate()`; `build_app(cfg: &Config, devices: Arc<dyn DeviceAuthenticator>) -> Router` (Task 7 ships it with only `/health`, taking the `devices` arg already so later tasks don't change the signature — `DeviceAuthenticator` is defined in this task in `identity/device.rs` as the trait only, implementation in Task 9).

- [ ] **Step 1: `Cargo.toml`**

```toml
[package]
name = "gateway"
version = "0.1.0"
edition = "2024"

[lib]
name = "gateway"
path = "src/lib.rs"

[dependencies]
common = { path = "../common" }
axum = "0.8"
tokio = { version = "1", features = ["rt-multi-thread", "macros", "net", "time"] }
tower-http = { version = "0.7", features = ["cors", "trace"] }
tracing = "0.1"
tracing-subscriber = "0.3"
serde_json = "1"
uuid = { version = "1", features = ["v4"] }
reqwest = { version = "0.13", default-features = false, features = ["stream"] }
tonic = "0.14"
governor = "0.10"
anyhow = "1"
dotenvy = "0.15"

[dev-dependencies]
axum-test = "21"
```

- [ ] **Step 2: `config.rs` with tests first**

```rust
use anyhow::{Context, Result};

#[derive(Clone)]
pub struct Config {
    pub port: u16,
    pub jwt_secret: String,
    pub internal_key: String,
    pub accounts_http_url: String,
    pub accounts_grpc_url: String,
    pub configs_http_url: String,
    pub site_origin: String,
    /// Behind a reverse proxy (nginx/caddy) that appends the client IP to
    /// X-Forwarded-For. Never enable when the gateway is exposed directly.
    pub trust_proxy: bool,
}

fn var(name: &str) -> Result<String> {
    std::env::var(name).with_context(|| format!("{name} not set"))
}

impl Config {
    pub fn from_env() -> Result<Config> {
        Ok(Config {
            port: std::env::var("GATEWAY_PORT").unwrap_or_else(|_| "8080".into()).parse().context("GATEWAY_PORT")?,
            jwt_secret: var("JWT_SECRET")?,
            internal_key: var("INTERNAL_KEY")?,
            accounts_http_url: var("ACCOUNTS_HTTP_URL")?,
            accounts_grpc_url: var("ACCOUNTS_GRPC_URL")?,
            configs_http_url: var("CONFIGS_HTTP_URL")?,
            site_origin: var("SITE_ORIGIN")?,
            trust_proxy: std::env::var("TRUST_PROXY").is_ok_and(|v| v == "true"),
        })
    }

    pub fn validate(&self) -> Result<()> {
        for (name, value) in [("JWT_SECRET", &self.jwt_secret), ("INTERNAL_KEY", &self.internal_key)] {
            if value.len() < 32 {
                anyhow::bail!("{name} must be at least 32 bytes");
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    pub fn sample() -> Config {
        Config {
            port: 0,
            jwt_secret: "j".repeat(32),
            internal_key: "k".repeat(32),
            accounts_http_url: String::new(),
            accounts_grpc_url: String::new(),
            configs_http_url: String::new(),
            site_origin: "http://localhost:5173".into(),
            trust_proxy: false,
        }
    }

    #[test]
    fn valid_config_passes() {
        assert!(sample().validate().is_ok());
    }

    #[test]
    fn weak_secrets_are_rejected() {
        let mut c = sample();
        c.internal_key = "short".into();
        assert!(c.validate().is_err());
        let mut c = sample();
        c.jwt_secret = "short".into();
        assert!(c.validate().is_err());
    }
}
```

- [ ] **Step 3: `identity/device.rs` trait (impl comes in Task 9) + `identity/mod.rs`**

```rust
use std::{future::Future, pin::Pin};
use uuid::Uuid;

pub enum DeviceAuth {
    Valid(Uuid),
    Invalid,
    /// accounts-service unreachable — the gateway answers 503, not 401,
    /// so the mod doesn't wrongly forget its token.
    Unavailable,
}

pub type DeviceAuthFuture<'a> = Pin<Box<dyn Future<Output = DeviceAuth> + Send + 'a>>;

pub trait DeviceAuthenticator: Send + Sync + 'static {
    fn authenticate<'a>(&'a self, token: &'a str) -> DeviceAuthFuture<'a>;
}
```
`identity/mod.rs`: `pub mod device;`.

- [ ] **Step 4: `lib.rs`, `main.rs`, health test**

`lib.rs`:
```rust
pub mod config;
pub mod identity;

use axum::{Router, routing::get};
use config::Config;
use identity::device::DeviceAuthenticator;
use std::sync::Arc;

pub fn build_app(_cfg: &Config, _devices: Arc<dyn DeviceAuthenticator>) -> Router {
    Router::new().route("/health", get(|| async { "ok" }))
}
```
`main.rs` (the gRPC authenticator doesn't exist yet — pass a stub that says `Unavailable`, replaced in Task 9 Step 5):
```rust
use gateway::identity::device::{DeviceAuth, DeviceAuthFuture, DeviceAuthenticator};
use std::sync::Arc;

struct NotYetWired;
impl DeviceAuthenticator for NotYetWired {
    fn authenticate<'a>(&'a self, _token: &'a str) -> DeviceAuthFuture<'a> {
        Box::pin(async { DeviceAuth::Unavailable })
    }
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    dotenvy::dotenv().ok();
    tracing_subscriber::fmt::init();
    let cfg = gateway::config::Config::from_env()?;
    cfg.validate()?;
    let app = gateway::build_app(&cfg, Arc::new(NotYetWired));
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", cfg.port)).await?;
    tracing::info!("gateway listening on {}", cfg.port);
    axum::serve(listener, app.into_make_service_with_connect_info::<std::net::SocketAddr>()).await?;
    Ok(())
}
```
`tests/common/mod.rs`:
```rust
#![allow(dead_code)]

use gateway::config::Config;
use gateway::identity::device::{DeviceAuth, DeviceAuthFuture, DeviceAuthenticator};
use std::sync::Arc;
use uuid::Uuid;

pub const JWT_SECRET: &str = "gateway-test-secret-gateway-test!!";
pub const INTERNAL_KEY: &str = "internal-key-internal-key-internal!!";

pub fn config(accounts: &str, configs: &str) -> Config {
    Config {
        port: 0,
        jwt_secret: JWT_SECRET.into(),
        internal_key: INTERNAL_KEY.into(),
        accounts_http_url: accounts.into(),
        accounts_grpc_url: String::new(),
        configs_http_url: configs.into(),
        site_origin: "http://localhost:5173".into(),
        trust_proxy: true,
    }
}

/// Accepts exactly one device token, mapped to one account.
pub struct FakeDevices {
    pub token: String,
    pub account: Uuid,
}

impl DeviceAuthenticator for FakeDevices {
    fn authenticate<'a>(&'a self, token: &'a str) -> DeviceAuthFuture<'a> {
        Box::pin(async move {
            if token == self.token { DeviceAuth::Valid(self.account) } else { DeviceAuth::Invalid }
        })
    }
}

pub fn no_devices() -> Arc<dyn DeviceAuthenticator> {
    Arc::new(FakeDevices { token: "lvd_none".into(), account: Uuid::nil() })
}
```
`tests/proxy.rs`:
```rust
mod common;

#[tokio::test]
async fn health_is_ok() {
    let app = gateway::build_app(&common::config("http://127.0.0.1:1", "http://127.0.0.1:1"), common::no_devices());
    axum_test::TestServer::new(app).get("/health").await.assert_status_ok();
}
```

- [ ] **Step 5: Run `cargo test -p gateway`, commit**

`.env.example` additions:
```
# gateway
GATEWAY_PORT=8080
ACCOUNTS_HTTP_URL=http://127.0.0.1:8081
ACCOUNTS_GRPC_URL=http://127.0.0.1:50051
CONFIGS_HTTP_URL=http://127.0.0.1:8082
SITE_ORIGIN=http://localhost:5173
TRUST_PROXY=false
```
```bash
git add -A backend/Cargo.toml backend/Cargo.lock backend/gateway backend/.env.example
git commit -m "feat(gateway): scaffold crate with config validation and health check"
```

---

### Task 8: Reverse proxy

**Files:**
- Create: `backend/gateway/src/proxy/mod.rs`, `proxy/routes.rs`, `proxy/forward.rs`
- Modify: `src/lib.rs`, `tests/common/mod.rs` (echo upstream), `tests/proxy.rs`

**Interfaces:**
- Produces: `proxy::routes::{Upstream, upstream_for(&str) -> Option<Upstream>}`; `proxy::forward::{Upstreams, proxy}` where `Upstreams { client: reqwest::Client, accounts: String, configs: String, internal_key: HeaderValue }` and `Upstreams::new(&Config) -> anyhow::Result<Self>`; `async fn proxy(State<Arc<Upstreams>>, Request) -> Response` mounted as the router `fallback`.
- Routing table: `auth, device, avatars, me, users` → accounts; `configs, showcase` → configs; anything else → 404 JSON.

- [ ] **Step 1: Echo upstream helper (tests/common/mod.rs)**

```rust
use axum::{Json, Router, body::Bytes, extract::Request, routing::any};

/// Starts a fake service that echoes what it received as JSON; returns its base URL.
pub async fn spawn_echo() -> String {
    async fn echo(req: Request) -> Json<serde_json::Value> {
        let (parts, body) = req.into_parts();
        let body: Bytes = axum::body::to_bytes(body, usize::MAX).await.unwrap_or_default();
        let header = |name: &str| parts.headers.get(name).and_then(|v| v.to_str().ok()).map(str::to_owned);
        Json(serde_json::json!({
            "method": parts.method.as_str(),
            "path": parts.uri.path(),
            "query": parts.uri.query(),
            "body": String::from_utf8_lossy(&body),
            "account_id": header("x-lovisual-account-id"),
            "internal_key": header("x-lovisual-internal-key"),
            "authorization": header("authorization"),
        }))
    }
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let addr = listener.local_addr().unwrap();
    tokio::spawn(async move { axum::serve(listener, Router::new().fallback(any(echo))).await.unwrap() });
    format!("http://{addr}")
}
```
Add `serde_json = "1"` to gateway `[dev-dependencies]` if not visible (it is a normal dep — fine).

- [ ] **Step 2: Failing tests (tests/proxy.rs)**

```rust
use axum::http::StatusCode;

async fn server() -> axum_test::TestServer {
    let accounts = common::spawn_echo().await;
    let configs = common::spawn_echo().await;
    let app = gateway::build_app(&common::config(&accounts, &configs), common::no_devices());
    axum_test::TestServer::new(app)
}

#[tokio::test]
async fn forwards_method_path_query_and_body() {
    let res = server().await.put("/configs/2?x=1").text("payload").await;
    res.assert_status_ok();
    let echo: serde_json::Value = res.json();
    assert_eq!(echo["method"], "PUT");
    assert_eq!(echo["path"], "/configs/2");
    assert_eq!(echo["query"], "x=1");
    assert_eq!(echo["body"], "payload");
}

#[tokio::test]
async fn adds_internal_key_and_strips_spoofed_identity() {
    let res = server().await
        .post("/auth/login")
        .add_header("x-lovisual-account-id", uuid::Uuid::new_v4().to_string())
        .add_header("x-lovisual-internal-key", "spoofed")
        .await;
    let echo: serde_json::Value = res.json();
    assert_eq!(echo["internal_key"], common::INTERNAL_KEY);
    assert!(echo["account_id"].is_null());
}

#[tokio::test]
async fn unknown_prefix_is_404() {
    server().await.get("/nope").await.assert_status(StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn dead_upstream_is_502() {
    let app = gateway::build_app(&common::config("http://127.0.0.1:1", "http://127.0.0.1:1"), common::no_devices());
    axum_test::TestServer::new(app).get("/me").await.assert_status(StatusCode::BAD_GATEWAY);
}

#[tokio::test]
async fn oversized_body_is_413() {
    let big = "x".repeat(6 * 1024 * 1024 + 1);
    server().await.put("/configs/1").text(big).await.assert_status(StatusCode::PAYLOAD_TOO_LARGE);
}
```
(`adds_internal_key_and_strips_spoofed_identity` fully passes only after Task 9 strips the identity header; in this task `forward` itself also removes `x-lovisual-account-id` — Task 9 moves that into the identity layer and re-adds the resolved value, so forward must NOT strip it after Task 9. Implement stripping in `forward` now and move it in Task 9 Step 4.)

- [ ] **Step 3: `proxy/routes.rs`**

```rust
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Upstream {
    Accounts,
    Configs,
}

pub fn upstream_for(path: &str) -> Option<Upstream> {
    match path.trim_start_matches('/').split('/').next()? {
        "auth" | "device" | "avatars" | "me" | "users" => Some(Upstream::Accounts),
        "configs" | "showcase" => Some(Upstream::Configs),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn routes_by_first_segment_only() {
        assert_eq!(upstream_for("/auth/login"), Some(Upstream::Accounts));
        assert_eq!(upstream_for("/me"), Some(Upstream::Accounts));
        assert_eq!(upstream_for("/configs/shared/ABC"), Some(Upstream::Configs));
        assert_eq!(upstream_for("/showcase"), Some(Upstream::Configs));
        assert_eq!(upstream_for("/authx"), None);
        assert_eq!(upstream_for("/"), None);
        assert_eq!(upstream_for("/health"), None);
    }
}
```

- [ ] **Step 4: `proxy/forward.rs`**

```rust
use super::routes::{Upstream, upstream_for};
use crate::config::Config;
use axum::{
    Json,
    body::Body,
    extract::{Request, State},
    http::{HeaderMap, HeaderName, HeaderValue, StatusCode, header},
    response::{IntoResponse, Response},
};
use common::internal::INTERNAL_KEY_HEADER;
use serde_json::json;
use std::{sync::Arc, time::Duration};

/// 5 MB avatar + multipart overhead; configs are far smaller.
pub const MAX_BODY_BYTES: usize = 6 * 1024 * 1024;

const HOP_BY_HOP: [HeaderName; 7] = [
    header::CONNECTION,
    header::PROXY_AUTHENTICATE,
    header::PROXY_AUTHORIZATION,
    header::TE,
    header::TRAILER,
    header::TRANSFER_ENCODING,
    header::UPGRADE,
];

pub struct Upstreams {
    pub client: reqwest::Client,
    pub accounts: String,
    pub configs: String,
    pub internal_key: HeaderValue,
}

impl Upstreams {
    pub fn new(cfg: &Config) -> anyhow::Result<Self> {
        Ok(Upstreams {
            client: reqwest::Client::builder()
                // Never follow redirects on behalf of the client — pass them through.
                .redirect(reqwest::redirect::Policy::none())
                .timeout(Duration::from_secs(30))
                .build()?,
            accounts: cfg.accounts_http_url.trim_end_matches('/').to_owned(),
            configs: cfg.configs_http_url.trim_end_matches('/').to_owned(),
            internal_key: HeaderValue::from_str(&cfg.internal_key)?,
        })
    }
}

fn error(status: StatusCode, message: &str) -> Response {
    (status, Json(json!({ "error": message }))).into_response()
}

fn strip_hop_by_hop(headers: &mut HeaderMap) {
    for name in &HOP_BY_HOP {
        headers.remove(name);
    }
    headers.remove("keep-alive");
}

pub async fn proxy(State(up): State<Arc<Upstreams>>, req: Request) -> Response {
    let Some(target) = upstream_for(req.uri().path()) else {
        return error(StatusCode::NOT_FOUND, "not found");
    };
    let base = match target {
        Upstream::Accounts => &up.accounts,
        Upstream::Configs => &up.configs,
    };
    let path_and_query = req.uri().path_and_query().map_or("/", |p| p.as_str());
    let url = format!("{base}{path_and_query}");

    let (parts, body) = req.into_parts();
    let Ok(bytes) = axum::body::to_bytes(body, MAX_BODY_BYTES).await else {
        return error(StatusCode::PAYLOAD_TOO_LARGE, "payload too large");
    };
    let mut headers = parts.headers;
    strip_hop_by_hop(&mut headers);
    headers.remove(header::HOST);
    headers.insert(INTERNAL_KEY_HEADER, up.internal_key.clone());

    let upstream = match up.client.request(parts.method, url).headers(headers).body(bytes).send().await {
        Ok(resp) => resp,
        Err(err) => {
            tracing::warn!("upstream {target:?} failed: {err}");
            return error(StatusCode::BAD_GATEWAY, "upstream unavailable");
        }
    };

    let mut response = Response::builder().status(upstream.status());
    for (name, value) in upstream.headers() {
        if !HOP_BY_HOP.contains(name) && name != "keep-alive" {
            response = response.header(name, value);
        }
    }
    response
        .body(Body::from_stream(upstream.bytes_stream()))
        .unwrap_or_else(|_| error(StatusCode::BAD_GATEWAY, "bad upstream response"))
}
```
In this task also add, at the top of `proxy` (moved to identity in Task 9): `headers.remove(common::internal::ACCOUNT_ID_HEADER);` right after `let mut headers = parts.headers;`.

`proxy/mod.rs`: `pub mod forward; pub mod routes;`. `lib.rs`:
```rust
pub mod proxy;
// ...
pub fn build_app(cfg: &Config, _devices: Arc<dyn DeviceAuthenticator>) -> Router {
    let upstreams = Arc::new(proxy::forward::Upstreams::new(cfg).expect("valid upstream config"));
    Router::new()
        .route("/health", get(|| async { "ok" }))
        .fallback(proxy::forward::proxy)
        .with_state(upstreams)
}
```
`Upstreams::new` only fails on an internal key that isn't a valid header value — a startup misconfiguration, so `expect` at boot is acceptable (not request data). Better: make `build_app` return `anyhow::Result<Router>` if the reviewer prefers — then update main/tests accordingly and keep it consistent in Tasks 9–11.

- [ ] **Step 5: Tests pass; commit**

```bash
git add -A backend/gateway backend/Cargo.lock
git commit -m "feat(gateway): reverse proxy to accounts and configs services"
```

---

### Task 9: Identity — JWT or device token, resolved once

**Files:**
- Modify: `backend/gateway/src/identity/mod.rs` (middleware), `identity/device.rs` (+ `GrpcDevices`), `src/lib.rs`, `src/main.rs`, `src/proxy/forward.rs` (remove the identity-header strip)
- Create: `backend/gateway/tests/identity.rs`

**Interfaces:**
- Consumes: `common::jwt::{bearer_token, verify_token, issue_access_token, TokenType}`, `common::internal::{ACCOUNT_ID_HEADER, INTERNAL_KEY_HEADER, DEVICE_TOKEN_PREFIX, GrpcKeyAttach}`, `common::pb::accounts::accounts_internal_client::AccountsInternalClient`.
- Produces: `identity::Identity(pub Option<Uuid>)` request extension (read by rate limiting in Task 10); `identity::IdentityState { jwt_secret: Arc<str>, devices: Arc<dyn DeviceAuthenticator> }`; `async fn identify(State<IdentityState>, Request, Next) -> Response`; `identity::device::GrpcDevices::connect_lazy(url: &str, internal_key: &str) -> anyhow::Result<GrpcDevices>`.
- Behaviour: no `Authorization` → anonymous. `Bearer lvd_...` → device lookup. Any other Bearer → JWT access token. Invalid credential → **401 at the gateway** (never forwarded). Device backend down → 503. `Authorization` is removed before forwarding.

- [ ] **Step 1: Failing tests (tests/identity.rs)**

```rust
mod common;

use axum::http::StatusCode;
use std::sync::Arc;
use uuid::Uuid;

async fn server(devices: common::FakeDevices) -> axum_test::TestServer {
    let echo = common::spawn_echo().await;
    let app = gateway::build_app(&common::config(&echo, &echo), Arc::new(devices));
    axum_test::TestServer::new(app)
}

fn devices() -> common::FakeDevices {
    common::FakeDevices { token: "lvd_good".into(), account: Uuid::new_v4() }
}

#[tokio::test]
async fn access_jwt_becomes_account_header_and_authorization_is_dropped() {
    let account = Uuid::new_v4();
    let token = common::jwt::issue_access_token(account, common::JWT_SECRET);
    let echo: serde_json::Value = server(devices()).await
        .get("/me").authorization_bearer(token).await.json();
    assert_eq!(echo["account_id"], account.to_string());
    assert!(echo["authorization"].is_null());
}

#[tokio::test]
async fn device_token_is_resolved_via_authenticator() {
    let d = devices();
    let account = d.account;
    let echo: serde_json::Value = server(d).await
        .get("/configs").authorization_bearer("lvd_good").await.json();
    assert_eq!(echo["account_id"], account.to_string());
}

#[tokio::test]
async fn bad_credentials_are_rejected_at_the_gateway() {
    let s = server(devices()).await;
    s.get("/me").authorization_bearer("lvd_bad").await.assert_status(StatusCode::UNAUTHORIZED);
    s.get("/me").authorization_bearer("not.a.jwt").await.assert_status(StatusCode::UNAUTHORIZED);
    let wrong_key = common::jwt::issue_access_token(Uuid::new_v4(), "another-secret-another-secret-12345");
    s.get("/me").authorization_bearer(wrong_key).await.assert_status(StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn anonymous_requests_pass_without_identity() {
    let echo: serde_json::Value = server(devices()).await.post("/auth/login").await.json();
    assert!(echo["account_id"].is_null());
}

#[tokio::test]
async fn spoofed_identity_header_never_reaches_the_service() {
    let echo: serde_json::Value = server(devices()).await
        .get("/me").add_header("x-lovisual-account-id", Uuid::new_v4().to_string()).await.json();
    assert!(echo["account_id"].is_null());
}
```
In `tests/common/mod.rs` add `pub use common::jwt;` — note the name clash: the test helper module is also called `common`. Rename the dependency import instead: `pub use ::common::jwt;` (leading `::` = the crate).

- [ ] **Step 2: Implement the middleware (`identity/mod.rs`)**

```rust
pub mod device;

use axum::{
    Json,
    extract::{Request, State},
    http::{HeaderValue, StatusCode, header::AUTHORIZATION},
    middleware::Next,
    response::{IntoResponse, Response},
};
use common::internal::{ACCOUNT_ID_HEADER, DEVICE_TOKEN_PREFIX, INTERNAL_KEY_HEADER};
use common::jwt::{TokenType, bearer_token, verify_token};
use device::{DeviceAuth, DeviceAuthenticator};
use serde_json::json;
use std::sync::Arc;
use uuid::Uuid;

/// Who made the request, as far as the gateway could verify.
#[derive(Clone, Copy)]
pub struct Identity(pub Option<Uuid>);

#[derive(Clone)]
pub struct IdentityState {
    pub jwt_secret: Arc<str>,
    pub devices: Arc<dyn DeviceAuthenticator>,
}

fn reject(status: StatusCode, message: &str) -> Response {
    (status, Json(json!({ "error": message }))).into_response()
}

pub async fn identify(State(state): State<IdentityState>, mut req: Request, next: Next) -> Response {
    // Client-supplied copies of trusted headers are never forwarded.
    req.headers_mut().remove(ACCOUNT_ID_HEADER);
    req.headers_mut().remove(INTERNAL_KEY_HEADER);

    let account = match bearer_token(req.headers()) {
        None => None,
        Some(token) if token.starts_with(DEVICE_TOKEN_PREFIX) => match state.devices.authenticate(token).await {
            DeviceAuth::Valid(id) => Some(id),
            DeviceAuth::Invalid => return reject(StatusCode::UNAUTHORIZED, "unauthorized"),
            DeviceAuth::Unavailable => return reject(StatusCode::SERVICE_UNAVAILABLE, "auth backend unavailable"),
        },
        Some(token) => match verify_token(token, &state.jwt_secret, TokenType::Access)
            .and_then(|claims| Uuid::parse_str(&claims.sub).ok())
        {
            Some(id) => Some(id),
            None => return reject(StatusCode::UNAUTHORIZED, "unauthorized"),
        },
    };

    req.headers_mut().remove(AUTHORIZATION);
    if let Some(id) = account {
        if let Ok(value) = HeaderValue::from_str(&id.to_string()) {
            req.headers_mut().insert(ACCOUNT_ID_HEADER, value);
        }
    }
    req.extensions_mut().insert(Identity(account));
    next.run(req).await
}
```
Remove the temporary `headers.remove(ACCOUNT_ID_HEADER)` from `proxy/forward.rs` (the middleware now owns that).

- [ ] **Step 3: gRPC authenticator (`identity/device.rs`, below the trait)**

```rust
use common::internal::GrpcKeyAttach;
use common::pb::accounts::{AuthenticateDeviceRequest, accounts_internal_client::AccountsInternalClient};
use tonic::{Code, service::interceptor::InterceptedService, transport::{Channel, Endpoint}};

pub struct GrpcDevices {
    client: AccountsInternalClient<InterceptedService<Channel, GrpcKeyAttach>>,
}

impl GrpcDevices {
    /// Lazy: the gateway boots even if accounts-service is still starting.
    pub fn connect_lazy(url: &str, internal_key: &str) -> anyhow::Result<Self> {
        let channel = Endpoint::from_shared(url.to_owned())?
            .timeout(std::time::Duration::from_secs(5))
            .connect_lazy();
        Ok(GrpcDevices { client: AccountsInternalClient::with_interceptor(channel, GrpcKeyAttach::new(internal_key)?) })
    }
}

impl DeviceAuthenticator for GrpcDevices {
    fn authenticate<'a>(&'a self, token: &'a str) -> DeviceAuthFuture<'a> {
        Box::pin(async move {
            let mut client = self.client.clone();
            match client.authenticate_device(AuthenticateDeviceRequest { device_token: token.to_owned() }).await {
                Ok(reply) => Uuid::parse_str(&reply.into_inner().account_id)
                    .map_or(DeviceAuth::Invalid, DeviceAuth::Valid),
                Err(status) if status.code() == Code::Unauthenticated => DeviceAuth::Invalid,
                Err(status) => {
                    tracing::warn!("AuthenticateDevice failed: {status}");
                    DeviceAuth::Unavailable
                }
            }
        })
    }
}
```
(`tonic`, `anyhow` are already gateway deps.)

- [ ] **Step 4: Wire it (`lib.rs`) and replace the stub in `main.rs`**

```rust
pub fn build_app(cfg: &Config, devices: Arc<dyn DeviceAuthenticator>) -> Router {
    let upstreams = Arc::new(proxy::forward::Upstreams::new(cfg).expect("valid upstream config"));
    let identity = identity::IdentityState { jwt_secret: cfg.jwt_secret.as_str().into(), devices };
    Router::new()
        .route("/health", get(|| async { "ok" }))
        .fallback(proxy::forward::proxy)
        .with_state(upstreams)
        .layer(axum::middleware::from_fn_with_state(identity, identity::identify))
}
```
`main.rs`: delete `NotYetWired`; use `Arc::new(GrpcDevices::connect_lazy(&cfg.accounts_grpc_url, &cfg.internal_key)?)`.

- [ ] **Step 5: `cargo test -p gateway` passes; commit**

```bash
git add -A backend/gateway
git commit -m "feat(gateway): resolve identity once from access JWT or device token via gRPC"
```

---

### Task 10: Rate limiting

**Files:**
- Create: `backend/gateway/src/rate_limit/mod.rs`, `rate_limit/rules.rs`, `backend/gateway/tests/rate_limit.rs`
- Modify: `src/lib.rs`, `src/main.rs`

**Interfaces:**
- Consumes: `identity::Identity` extension, `Config.trust_proxy`.
- Produces: `rate_limit::rules::{KeyBy, Rule, rules() -> Vec<Rule>}`, `rate_limit::RateLimits::new(trust_proxy: bool) -> Arc<RateLimits>`, `RateLimits::purge(&self)`, `async fn enforce(State<Arc<RateLimits>>, Request, Next) -> Response`.
- 429 body `{"error":"too many requests"}` with `Retry-After: <whole seconds, ≥1>`.

Limits (from `TODO.md` Фаза 10 §6; one deliberate deviation marked ★):

| Method + path | Quota | Key |
|---|---|---|
| POST `/auth/login` | 5/min | IP |
| POST `/auth/register` | 3/hour | IP |
| POST `/auth/refresh` | 30/min | IP |
| POST `/device/code` | 10/min | IP |
| POST `/device/token` | ★ 30/min | IP — the mod polls every 2–3 s for up to 10 min; 10/min would break linking |
| PUT `/configs/*` | 20/min | account (IP if anonymous) |
| GET `/configs/shared/*` | 30/min | IP |
| POST `/avatars` | 5/hour | account |
| GET `/showcase*` | 60/min | IP |
| everything (global, in addition) | 300/min | account or IP |

- [ ] **Step 1: `rules.rs` with tests**

```rust
use axum::http::Method;
use governor::Quota;
use std::num::NonZeroU32;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum KeyBy {
    Ip,
    Account,
}

pub struct Rule {
    pub method: Method,
    /// Exact path, or a prefix when it ends with '*'.
    pub pattern: &'static str,
    pub quota: Quota,
    pub key_by: KeyBy,
}

impl Rule {
    pub fn matches(&self, method: &Method, path: &str) -> bool {
        if &self.method != method {
            return false;
        }
        match self.pattern.strip_suffix('*') {
            Some(prefix) => path.starts_with(prefix),
            None => path == self.pattern,
        }
    }
}

fn n(v: u32) -> NonZeroU32 {
    NonZeroU32::new(v).expect("rate-limit constants are non-zero")
}

fn rule(method: Method, pattern: &'static str, quota: Quota, key_by: KeyBy) -> Rule {
    Rule { method, pattern, quota, key_by }
}

pub fn rules() -> Vec<Rule> {
    use KeyBy::*;
    vec![
        rule(Method::POST, "/auth/login", Quota::per_minute(n(5)), Ip),
        rule(Method::POST, "/auth/register", Quota::per_hour(n(3)), Ip),
        rule(Method::POST, "/auth/refresh", Quota::per_minute(n(30)), Ip),
        rule(Method::POST, "/device/code", Quota::per_minute(n(10)), Ip),
        rule(Method::POST, "/device/token", Quota::per_minute(n(30)), Ip),
        rule(Method::PUT, "/configs/*", Quota::per_minute(n(20)), Account),
        rule(Method::GET, "/configs/shared/*", Quota::per_minute(n(30)), Ip),
        rule(Method::POST, "/avatars", Quota::per_hour(n(5)), Account),
        rule(Method::GET, "/showcase*", Quota::per_minute(n(60)), Ip),
    ]
}

pub fn global_quota() -> Quota {
    Quota::per_minute(n(300))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn exact_and_prefix_matching() {
        let all = rules();
        let find = |m: Method, p: &str| all.iter().position(|r| r.matches(&m, p));
        assert_eq!(find(Method::POST, "/auth/login"), Some(0));
        assert_eq!(find(Method::GET, "/auth/login"), None);
        assert_eq!(find(Method::POST, "/auth/login/x"), None);
        assert!(find(Method::PUT, "/configs/3").is_some());
        assert!(find(Method::GET, "/configs/shared/ABCD").is_some());
        assert!(find(Method::GET, "/showcase?page=2").is_some() || find(Method::GET, "/showcase").is_some());
    }
}
```

- [ ] **Step 2: Failing integration tests (tests/rate_limit.rs)**

```rust
mod common;

use axum::http::StatusCode;

async fn server() -> axum_test::TestServer {
    let echo = common::spawn_echo().await;
    axum_test::TestServer::new(gateway::build_app(&common::config(&echo, &echo), common::no_devices()))
}

#[tokio::test]
async fn sixth_login_in_a_minute_from_one_ip_is_429_with_retry_after() {
    let s = server().await;
    for _ in 0..5 {
        s.post("/auth/login").add_header("x-forwarded-for", "203.0.113.7").await.assert_status_ok();
    }
    let res = s.post("/auth/login").add_header("x-forwarded-for", "203.0.113.7").await;
    res.assert_status(StatusCode::TOO_MANY_REQUESTS);
    let retry: u64 = res.header("retry-after").to_str().unwrap().parse().unwrap();
    assert!((1..=60).contains(&retry));
}

#[tokio::test]
async fn limits_are_per_ip() {
    let s = server().await;
    for _ in 0..5 {
        s.post("/auth/login").add_header("x-forwarded-for", "203.0.113.8").await;
    }
    s.post("/auth/login").add_header("x-forwarded-for", "203.0.113.9").await.assert_status_ok();
}

#[tokio::test]
async fn rightmost_forwarded_for_entry_is_used() {
    // A client can prepend fake entries; only the one our proxy appended counts.
    let s = server().await;
    for i in 0..5 {
        s.post("/auth/login")
            .add_header("x-forwarded-for", format!("10.0.0.{i}, 203.0.113.10"))
            .await.assert_status_ok();
    }
    s.post("/auth/login").add_header("x-forwarded-for", "1.1.1.1, 203.0.113.10")
        .await.assert_status(StatusCode::TOO_MANY_REQUESTS);
}
```

- [ ] **Step 3: Implement `rate_limit/mod.rs`**

```rust
pub mod rules;

use crate::identity::Identity;
use axum::{
    Json,
    extract::{ConnectInfo, Request, State},
    http::{HeaderValue, StatusCode, header::RETRY_AFTER},
    middleware::Next,
    response::{IntoResponse, Response},
};
use governor::{DefaultKeyedRateLimiter, RateLimiter, clock::{Clock, DefaultClock}};
use rules::{KeyBy, Rule};
use serde_json::json;
use std::{net::SocketAddr, sync::Arc};

pub struct RateLimits {
    rules: Vec<(Rule, DefaultKeyedRateLimiter<String>)>,
    global: DefaultKeyedRateLimiter<String>,
    trust_proxy: bool,
    clock: DefaultClock,
}

impl RateLimits {
    pub fn new(trust_proxy: bool) -> Arc<Self> {
        Arc::new(RateLimits {
            rules: rules::rules().into_iter().map(|r| { let l = RateLimiter::keyed(r.quota); (r, l) }).collect(),
            global: RateLimiter::keyed(rules::global_quota()),
            trust_proxy,
            clock: DefaultClock::default(),
        })
    }

    /// Drops idle keys so the maps don't grow forever. Call periodically.
    pub fn purge(&self) {
        for (_, limiter) in &self.rules {
            limiter.retain_recent();
            limiter.shrink_to_fit();
        }
        self.global.retain_recent();
        self.global.shrink_to_fit();
    }

    fn client_ip(&self, req: &Request) -> String {
        if self.trust_proxy {
            if let Some(ip) = req.headers().get("x-forwarded-for")
                .and_then(|v| v.to_str().ok())
                .and_then(|v| v.rsplit(',').next())
                .map(str::trim)
                .filter(|s| !s.is_empty())
            {
                return ip.to_owned();
            }
        }
        req.extensions()
            .get::<ConnectInfo<SocketAddr>>()
            .map_or_else(|| "unknown".to_owned(), |c| c.0.ip().to_string())
    }
}

fn too_many(wait: std::time::Duration) -> Response {
    let secs = wait.as_secs_f64().ceil().max(1.0) as u64;
    let mut res = (StatusCode::TOO_MANY_REQUESTS, Json(json!({ "error": "too many requests" }))).into_response();
    res.headers_mut().insert(RETRY_AFTER, HeaderValue::from(secs));
    res
}

pub async fn enforce(State(limits): State<Arc<RateLimits>>, req: Request, next: Next) -> Response {
    let ip_key = format!("ip:{}", limits.client_ip(&req));
    let account_key = req.extensions().get::<Identity>().and_then(|i| i.0).map(|id| format!("acc:{id}"));
    let caller_key = account_key.clone().unwrap_or_else(|| ip_key.clone());

    let (method, path) = (req.method().clone(), req.uri().path().to_owned());
    if let Some((rule, limiter)) = limits.rules.iter().find(|(r, _)| r.matches(&method, &path)) {
        let key = match rule.key_by {
            KeyBy::Ip => &ip_key,
            KeyBy::Account => &caller_key,
        };
        if let Err(not_until) = limiter.check_key(key) {
            return too_many(not_until.wait_time_from(limits.clock.now()));
        }
    }
    if let Err(not_until) = limits.global.check_key(&caller_key) {
        return too_many(not_until.wait_time_from(limits.clock.now()));
    }
    next.run(req).await
}
```
(`Rule` needs no `Clone`; `matches` takes `&Method`.)

- [ ] **Step 4: Wire it — rate limit runs after identity**

In `lib.rs` add `pub mod rate_limit;` and in `build_app` (layers run bottom-up: the last `.layer` is outermost, so identity runs first):
```rust
let limits = rate_limit::RateLimits::new(cfg.trust_proxy);
spawn_purger(Arc::clone(&limits));
Router::new()
    .route("/health", get(|| async { "ok" }))
    .fallback(proxy::forward::proxy)
    .with_state(upstreams)
    .layer(axum::middleware::from_fn_with_state(limits, rate_limit::enforce))
    .layer(axum::middleware::from_fn_with_state(identity, identity::identify))
```
```rust
fn spawn_purger(limits: Arc<rate_limit::RateLimits>) {
    if let Ok(handle) = tokio::runtime::Handle::try_current() {
        handle.spawn(async move {
            let mut tick = tokio::time::interval(std::time::Duration::from_secs(60));
            loop {
                tick.tick().await;
                limits.purge();
            }
        });
    }
}
```

- [ ] **Step 5: Tests pass; commit**

```bash
git add -A backend/gateway
git commit -m "feat(gateway): per-route and global rate limits with Retry-After"
```

---

### Task 11: CORS, end-to-end run, docs

**Files:**
- Modify: `backend/gateway/src/lib.rs` (CORS layer), `backend/gateway/tests/proxy.rs` (preflight test)
- Modify: `backend/STRUCTURE.md`, `TODO.md` (Фаза 10 status line), `backend/.env.example`

**Interfaces:**
- Produces: CORS for `SITE_ORIGIN` only, credentials allowed (refresh cookie), methods GET/POST/PUT/DELETE, headers `authorization`, `content-type`.

- [ ] **Step 1: Failing preflight test**

```rust
#[tokio::test]
async fn cors_preflight_allows_only_the_site_origin() {
    let app = gateway::build_app(&common::config("http://127.0.0.1:1", "http://127.0.0.1:1"), common::no_devices());
    let s = axum_test::TestServer::new(app);
    let ok = s.method(axum::http::Method::OPTIONS, "/auth/login")
        .add_header("origin", "http://localhost:5173")
        .add_header("access-control-request-method", "POST")
        .await;
    assert_eq!(ok.header("access-control-allow-origin"), "http://localhost:5173");
    assert_eq!(ok.header("access-control-allow-credentials"), "true");

    let evil = s.method(axum::http::Method::OPTIONS, "/auth/login")
        .add_header("origin", "https://evil.example")
        .add_header("access-control-request-method", "POST")
        .await;
    assert!(evil.maybe_header("access-control-allow-origin").is_none());
}
```

- [ ] **Step 2: Add the layer (outermost, so preflights skip identity/rate limits)**

```rust
use axum::http::{Method, header::{AUTHORIZATION, CONTENT_TYPE}};
use tower_http::cors::CorsLayer;

let cors = CorsLayer::new()
    .allow_origin(HeaderValue::from_str(&cfg.site_origin).expect("SITE_ORIGIN is a valid origin"))
    .allow_credentials(true)
    .allow_methods([Method::GET, Method::POST, Method::PUT, Method::DELETE])
    .allow_headers([AUTHORIZATION, CONTENT_TYPE]);
// ... existing chain ...
    .layer(cors)
```

- [ ] **Step 3: Manual end-to-end check (both services, real DB)**

```bash
cd backend
set -a; . ./.env; set +a      # local .env with real secrets (gitignored)
CARGO_BUILD_JOBS=4 cargo run -p accounts-service &
CARGO_BUILD_JOBS=4 cargo run -p gateway &
sleep 5
curl -s -X POST localhost:8080/auth/register -H 'content-type: application/json' \
  -d '{"email":"e2e@example.com","password":"correct-horse-battery-staple","nick":"E2E"}'
TOKEN=$(curl -s -c /tmp/lv.jar -X POST localhost:8080/auth/login -H 'content-type: application/json' \
  -d '{"email":"e2e@example.com","password":"correct-horse-battery-staple"}' | sed 's/.*"access_token":"\([^"]*\)".*/\1/')
curl -s localhost:8080/me -H "authorization: Bearer $TOKEN"          # -> profile JSON
curl -s -b /tmp/lv.jar -X POST localhost:8080/auth/refresh           # -> new access_token
curl -s -o /dev/null -w '%{http_code}\n' localhost:8081/me           # -> 403 (direct access blocked)
kill %1 %2
```
Expected: register 201, `/me` via gateway 200, refresh 200, direct service access 403. Delete the e2e account afterwards (`DELETE FROM accounts WHERE email='e2e@example.com'`).

- [ ] **Step 4: Docs**

- `backend/STRUCTURE.md`: mark `gateway/` and `common/` as implemented, describe the internal contract (internal key + identity header, gRPC for device auth) in 3–5 lines.
- `TODO.md` Фаза 10 status line: gateway + auth hardening done; next: `backend/configs-service/PLAN.md`.

- [ ] **Step 5: Full workspace test + commit**

Run: `cd backend && DATABASE_URL=... CARGO_BUILD_JOBS=4 cargo test --workspace` → all pass.
```bash
git add -A backend/gateway backend/STRUCTURE.md backend/.env.example TODO.md
git commit -m "feat(gateway): CORS for the site origin; docs for gateway and internal contract"
```

---

### Task 12: Easter eggs in the gateway

- `.env` honeypot — done together with the dot-segment fix (see `.superpowers` fix list; commit "feat(gateway): .env honeypot easter egg for traversal scanners").
- `GET /coffee` (any method) → `418 I'm a teapot`, `text/plain; charset=utf-8`: «Я чайник. Кофе не варю, зато LoVisual бесплатный: https://github.com/loki5512344ss/LoVisual/releases». Handled in the gateway before identity/rate limits, not forwarded. Test + commit `feat(gateway): 418 teapot at /coffee`.

---

## Deferred (not in this plan)

- Admin role enforcement at the gateway (`/admin/*`) — Подсистема 3 plan; `Identity` will then carry the role (add `role` to the access-token claims).
- WebSocket proxying for `chat-service` — Подсистема 2 plan.
- Caching `AuthenticateDevice` results (short TTL) if the per-request gRPC call ever shows up in latency numbers — measure first.
- Redis-backed rate limits for multi-node deploys — single node for now, per TODO §6.
