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

/// A slow/stalled client body must not hold a connection open forever.
const BODY_READ_TIMEOUT: Duration = Duration::from_secs(30);

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
    pub chat: String,
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
            chat: cfg.chat_http_url.trim_end_matches('/').to_owned(),
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
        Upstream::Chat => &up.chat,
    };
    let path_and_query = req.uri().path_and_query().map_or("/", |p| p.as_str());
    let url = format!("{base}{path_and_query}");

    let (parts, body) = req.into_parts();
    let bytes = match tokio::time::timeout(
        BODY_READ_TIMEOUT,
        axum::body::to_bytes(body, MAX_BODY_BYTES),
    )
    .await
    {
        Ok(Ok(bytes)) => bytes,
        Ok(Err(err)) => {
            // `to_bytes` reports both "over the limit" and "the connection
            // broke while reading" the same way; only the former is 413.
            let over_limit = std::error::Error::source(&err)
                .is_some_and(|e| e.is::<http_body_util::LengthLimitError>());
            return if over_limit {
                error(StatusCode::PAYLOAD_TOO_LARGE, "payload too large")
            } else {
                error(StatusCode::BAD_REQUEST, "invalid request body")
            };
        }
        Err(_) => return error(StatusCode::REQUEST_TIMEOUT, "request body read timed out"),
    };
    let mut headers = parts.headers;
    strip_hop_by_hop(&mut headers);
    headers.remove(header::HOST);
    headers.insert(INTERNAL_KEY_HEADER, up.internal_key.clone());

    let upstream = match up
        .client
        .request(parts.method, url)
        .headers(headers)
        .body(bytes)
        .send()
        .await
    {
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
