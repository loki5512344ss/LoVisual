pub mod config;
pub mod guard;
pub mod identity;
pub mod proxy;
pub mod rate_limit;

use axum::{
    Router,
    http::{
        HeaderValue, Method,
        header::{AUTHORIZATION, CONTENT_TYPE, RETRY_AFTER},
    },
    routing::get,
};
use config::Config;
use identity::device::DeviceAuthenticator;
use std::sync::Arc;
use tower_http::{cors::CorsLayer, services::ServeDir};

pub fn build_app(cfg: &Config, devices: Arc<dyn DeviceAuthenticator>) -> Router {
    let upstreams = Arc::new(proxy::forward::Upstreams::new(cfg).expect("valid upstream config"));
    let identity = identity::IdentityState {
        jwt_secret: cfg.jwt_secret.as_str().into(),
        devices,
    };
    let limits = rate_limit::RateLimits::new(cfg.trust_proxy);
    spawn_purger(Arc::clone(&limits));
    let cors = CorsLayer::new()
        // Array form = AllowOrigin::list: the header is only sent back when the
        // request's Origin actually matches (the HeaderValue form would echo
        // the configured origin unconditionally).
        .allow_origin([
            HeaderValue::from_str(&cfg.site_origin).expect("SITE_ORIGIN is a valid origin")
        ])
        .allow_credentials(true)
        .allow_methods([Method::GET, Method::POST, Method::PUT, Method::DELETE])
        .allow_headers([AUTHORIZATION, CONTENT_TYPE])
        // Without this, browser JS can't read Retry-After on a 429 despite
        // the response carrying it — cross-origin responses only expose a
        // fixed default header set unless the server opts more in.
        .expose_headers([RETRY_AFTER]);
    // Layers run bottom-up: the last `.layer` is outermost. Request order is
    // therefore cors → guard → identify (resolve only) → rate limit →
    // authorize → upstream: a bad credential is counted against the caller's
    // limit before it gets rejected, and CORS preflights skip all of it.
    Router::new()
        .route("/health", get(|| async { "ok" }))
        // The mod jar: one flat file from DOWNLOADS_DIR, streamed (no buffering
        // of a ~60 MB body in memory). ServeDir resolves paths inside that
        // directory only and 404s anything else; the dot-segment guard above
        // already refuses `..`.
        .nest_service(
            "/downloads",
            ServeDir::new(&cfg.downloads_dir).append_index_html_on_directories(false),
        )
        .fallback(proxy::forward::proxy)
        .with_state(upstreams)
        .layer(axum::middleware::from_fn(identity::authorize))
        .layer(axum::middleware::from_fn_with_state(
            limits,
            rate_limit::enforce,
        ))
        .layer(axum::middleware::from_fn_with_state(
            identity,
            identity::identify,
        ))
        .layer(axum::middleware::from_fn_with_state(
            cfg.trust_proxy,
            guard::reject_ambiguous_paths,
        ))
        .layer(cors)
}

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
