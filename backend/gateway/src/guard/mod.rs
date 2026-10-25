//! Outermost request filter: answers the gateway's own jokes and refuses paths
//! whose meaning changes once the upstream URL is built. reqwest/`url` (WHATWG)
//! resolves `..`, `%2e%2e` and backslashes, while rate-limit rules match the raw
//! path — so without this, `POST /auth/x/../login` reaches `/auth/login` without
//! its 5/min limit.

pub mod honeypot;

use crate::rate_limit::client_ip;
use axum::{
    Json,
    extract::{Request, State},
    http::StatusCode,
    middleware::Next,
    response::{IntoResponse, Response},
};
use serde_json::json;

/// A path segment with `%2e`/`%2E` decoded — the only escape WHATWG URL
/// parsing treats as a dot when resolving dot-segments.
fn decode_dots(segment: &str) -> String {
    segment.to_ascii_lowercase().replace("%2e", ".")
}

fn segments(path: &str) -> impl Iterator<Item = &str> {
    path.strip_prefix('/').unwrap_or(path).split('/')
}

/// True when the upstream could see a different path than the one the
/// gateway routed and rate-limited.
pub fn is_ambiguous(path: &str) -> bool {
    if path == "/" {
        return false;
    }
    segments(path).any(|raw| {
        let lower = raw.to_ascii_lowercase();
        let dots = decode_dots(raw);
        raw.is_empty()
            || dots == "."
            || dots == ".."
            || raw.contains('\\')
            || lower.contains("%2f")
            || lower.contains("%5c")
    })
}

const TEAPOT: &str = "Я чайник. Кофе не варю, зато LoVisual бесплатный: https://github.com/loki5512344ss/LoVisual/releases\n";

fn teapot() -> Response {
    (
        StatusCode::IM_A_TEAPOT,
        [(
            axum::http::header::CONTENT_TYPE,
            "text/plain; charset=utf-8",
        )],
        TEAPOT,
    )
        .into_response()
}

pub async fn reject_ambiguous_paths(
    State(trust_proxy): State<bool>,
    req: Request,
    next: Next,
) -> Response {
    let path = req.uri().path();
    if path == "/coffee" {
        return teapot();
    }
    if honeypot::is_env_probe(path) {
        tracing::info!(ip = %client_ip(&req, trust_proxy), path, "honeypot hit");
        return honeypot::fake_env();
    }
    if is_ambiguous(path) {
        return (
            StatusCode::BAD_REQUEST,
            Json(json!({ "error": "bad path" })),
        )
            .into_response();
    }
    next.run(req).await
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn classifies_paths() {
        for bad in [
            "/a/../b",
            "/a/%2E%2e/b",
            "/a/.%2E",
            "/a//b",
            "/a%2Fb",
            "/a\\b",
            "/a/",
        ] {
            assert!(is_ambiguous(bad), "{bad}");
        }
        for ok in [
            "/",
            "/auth/login",
            "/configs/shared/AB2C",
            "/a/..b",
            "/a/.x",
        ] {
            assert!(!is_ambiguous(ok), "{ok}");
        }
    }
}
