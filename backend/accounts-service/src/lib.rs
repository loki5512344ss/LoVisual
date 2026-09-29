pub mod accounts;
pub mod auth;
pub mod avatars;
pub mod config;
pub mod device;
pub mod error;
pub mod grpc;

use accounts::handlers::AccountsState;
use auth::handlers::AuthState;
use avatars::{handlers::AvatarState, storage::S3Storage};
use axum::{
    Router,
    extract::DefaultBodyLimit,
    routing::{get, post},
};
use config::Config;
use device::{handlers::DeviceState, store::DeviceStore};

pub fn build_app(pool: sqlx::PgPool, cfg: &Config) -> Router {
    let auth_state = AuthState::new(pool.clone(), cfg.jwt_secret.clone(), cfg.cookie_secure);
    let device_state = DeviceState {
        store: DeviceStore::default(),
        pool: pool.clone(),
    };
    let avatar_state = AvatarState {
        pool: pool.clone(),
        storage: S3Storage::from_config(
            &cfg.s3_endpoint,
            &cfg.s3_access_key,
            &cfg.s3_secret_key,
            cfg.s3_bucket.clone(),
        ),
        base_url: cfg.avatar_base_url(),
    };
    let accounts_state = AccountsState {
        pool: pool.clone(),
        avatar_base_url: cfg.avatar_base_url(),
    };

    let auth_routes = Router::new()
        .route("/auth/register", post(auth::handlers::register))
        .route("/auth/login", post(auth::handlers::login))
        .route("/auth/refresh", post(auth::handlers::refresh))
        .route("/auth/logout", post(auth::handlers::logout))
        .with_state(auth_state);

    let device_routes = Router::new()
        .route("/device/code", post(device::handlers::create_code))
        .route("/device/confirm", post(device::handlers::confirm))
        .route("/device/token", post(device::handlers::token))
        .route("/device/links", get(device::handlers::list_links))
        .route(
            "/device/links/{id}",
            axum::routing::delete(device::handlers::revoke_link),
        )
        .with_state(device_state);

    let avatar_routes = Router::new()
        .route("/avatars", post(avatars::handlers::upload))
        // Public avatar read served back through the gateway; the `{*key}`
        // wildcard is the storage key (`avatars/<uuid>.png`).
        .route("/media/{*key}", get(avatars::handlers::serve))
        // Hard transport cap slightly above the 5 MB business limit (413 beyond it).
        .layer(DefaultBodyLimit::max(6 * 1024 * 1024))
        .with_state(avatar_state);

    let accounts_routes = Router::new()
        .route("/me", get(accounts::handlers::me))
        .route("/users/{id}", get(accounts::handlers::public_profile))
        .with_state(accounts_state);

    // Everything except /health is internal-only: reachable solely through
    // the gateway, which authenticates the caller and forwards the identity
    // header. Direct traffic (or spoofed headers) is rejected here.
    let api = Router::new()
        .merge(auth_routes)
        .merge(device_routes)
        .merge(avatar_routes)
        .merge(accounts_routes)
        .layer(axum::middleware::from_fn_with_state(
            common::internal::InternalKey::new(cfg.internal_key.clone()),
            common::internal::require_internal_key,
        ));

    Router::new()
        .route("/health", get(|| async { "ok" }))
        .merge(api)
}
