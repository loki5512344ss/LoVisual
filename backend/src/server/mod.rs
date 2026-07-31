//! Server assembly: router, listener, graceful shutdown.

use anyhow::Context as _;
use axum::Router;
use tokio::net::TcpListener;
use tower_http::cors::CorsLayer;
use tower_http::trace::TraceLayer;

use super::routes;
use config::Config;

pub mod config;

/// Build the application router for a given config.
pub fn app(config: &Config) -> Router {
    Router::new()
        .merge(routes::router())
        .layer(CorsLayer::new().allow_origin(config.cors_origins.clone()))
        .layer(TraceLayer::new_for_http())
}

/// Bind and serve the application until a shutdown signal arrives.
pub async fn serve(config: Config) -> anyhow::Result<()> {
    let listener = TcpListener::bind(config.listen_addr())
        .await
        .context("failed to bind listener")?;

    axum::serve(listener, app(&config))
        .with_graceful_shutdown(shutdown_signal())
        .await
        .context("server error")
}

/// Resolves on SIGTERM/SIGINT.
async fn shutdown_signal() {
    let ctrl_c = async {
        tokio::signal::ctrl_c()
            .await
            .expect("failed to install ctrl-c handler");
    };

    #[cfg(unix)]
    let terminate = async {
        tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
            .expect("failed to install SIGTERM handler")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        _ = ctrl_c => {},
        _ = terminate => {},
    }
}
