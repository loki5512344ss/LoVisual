pub mod config;
pub mod error;
pub mod sharing;
pub mod showcase;
pub mod slots;

use axum::{
    Router,
    extract::DefaultBodyLimit,
    routing::{get, post},
};
use common::internal::{InternalKey, require_internal_key};
use config::Config;
use showcase::profiles::ProfileSource;
use std::sync::Arc;

pub fn build_app(pool: sqlx::PgPool, cfg: &Config, profiles: Arc<dyn ProfileSource>) -> Router {
    let slots_state = slots::handlers::SlotsState { pool: pool.clone() };
    let showcase_state = showcase::handlers::ShowcaseState { pool, profiles };
    let showcase_routes = Router::new()
        .route(
            "/configs/{slot}/publish",
            post(showcase::handlers::publish).delete(showcase::handlers::unpublish),
        )
        .route("/showcase", get(showcase::handlers::browse))
        .route("/showcase/{id}", get(showcase::handlers::detail))
        .route("/showcase/{id}/copy", post(showcase::handlers::copy))
        .with_state(showcase_state);
    let api = Router::new()
        .route("/configs", get(slots::handlers::list))
        .route(
            "/configs/{slot}",
            get(slots::handlers::get).put(slots::handlers::save),
        )
        .route(
            "/configs/shared/{code}",
            get(sharing::handlers::load_shared),
        )
        .route(
            "/configs/{slot}/regenerate-code",
            post(sharing::handlers::regenerate),
        )
        .with_state(slots_state)
        .merge(showcase_routes)
        .layer(DefaultBodyLimit::max(
            slots::handlers::MAX_DATA_BYTES + 16 * 1024,
        ))
        .layer(axum::middleware::from_fn_with_state(
            InternalKey::new(cfg.internal_key.clone()),
            require_internal_key,
        ));
    Router::new()
        .route("/health", get(|| async { "ok" }))
        .merge(api)
}
