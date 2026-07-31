//! Route definitions.
//!
//! Top-level router; each domain (auth, profile, configs, irc) will add its
//! own sub-router here.

pub mod health;

use axum::Router;

/// Compose the full route tree.
pub fn router() -> Router {
    Router::new().merge(health::router())
}
