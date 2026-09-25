pub mod rules;

use crate::identity::Identity;
use axum::{
    Json,
    extract::{ConnectInfo, Request, State},
    http::{HeaderValue, StatusCode, header::RETRY_AFTER},
    middleware::Next,
    response::{IntoResponse, Response},
};
use governor::{
    DefaultKeyedRateLimiter, RateLimiter,
    clock::{Clock, DefaultClock},
};
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
            rules: rules::rules()
                .into_iter()
                .map(|r| {
                    let l = RateLimiter::keyed(r.quota);
                    (r, l)
                })
                .collect(),
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
        client_ip(req, self.trust_proxy)
    }
}

/// The caller's IP: the rightmost X-Forwarded-For entry (the one our own
/// proxy appended) when `trust_proxy`, else the socket peer.
pub fn client_ip(req: &Request, trust_proxy: bool) -> String {
    if trust_proxy
        && let Some(ip) = req
            .headers()
            .get("x-forwarded-for")
            .and_then(|v| v.to_str().ok())
            .and_then(|v| v.rsplit(',').next())
            .map(str::trim)
            .filter(|s| !s.is_empty())
    {
        return ip.to_owned();
    }
    req.extensions()
        .get::<ConnectInfo<SocketAddr>>()
        .map_or_else(|| "unknown".to_owned(), |c| c.0.ip().to_string())
}

fn too_many(wait: std::time::Duration) -> Response {
    let secs = wait.as_secs_f64().ceil().max(1.0) as u64;
    let mut res = (
        StatusCode::TOO_MANY_REQUESTS,
        Json(json!({ "error": "too many requests" })),
    )
        .into_response();
    res.headers_mut()
        .insert(RETRY_AFTER, HeaderValue::from(secs));
    res
}

pub async fn enforce(State(limits): State<Arc<RateLimits>>, req: Request, next: Next) -> Response {
    let ip_key = format!("ip:{}", limits.client_ip(&req));
    let account_key = req
        .extensions()
        .get::<Identity>()
        .and_then(|i| i.0)
        .map(|id| format!("acc:{id}"));
    let caller_key = account_key.unwrap_or_else(|| ip_key.clone());

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
