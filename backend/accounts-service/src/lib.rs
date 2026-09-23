pub mod auth;
pub mod config;

use axum::{routing::get, Router};

pub fn build_app(_pool: sqlx::PgPool) -> Router {
    Router::new().route("/health", get(|| async { "ok" }))
}
