pub mod config;
pub mod presence;

use axum::{
    Router,
    extract::DefaultBodyLimit,
    routing::{get, post},
};
use common::internal::{InternalKey, require_internal_key};
use config::Config;
use presence::store::Store;
use std::sync::Arc;
use tower_http::trace::TraceLayer;

/// Presence requests are tiny JSON (a ≤64-char payload and ≤40 uuids).
const MAX_BODY_BYTES: usize = 8 * 1024;

pub fn build_app(cfg: &Config) -> (Router, Arc<Store>) {
    let store = Arc::new(Store::default());
    let api = Router::new()
        .route("/presence/gui", post(presence::handlers::sync))
        .with_state(Arc::clone(&store))
        .layer(DefaultBodyLimit::max(MAX_BODY_BYTES))
        .layer(axum::middleware::from_fn_with_state(
            InternalKey::new(cfg.internal_key.clone()),
            require_internal_key,
        ));
    let app = Router::new()
        .route("/health", get(|| async { "ok" }))
        .merge(api)
        .layer(TraceLayer::new_for_http());
    (app, store)
}
