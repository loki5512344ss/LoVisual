#![allow(dead_code)]

use axum::{Json, Router, body::Bytes, extract::Request, routing::any};
use gateway::config::Config;
use gateway::identity::device::{DeviceAuth, DeviceAuthFuture, DeviceAuthenticator};
use std::sync::Arc;
use uuid::Uuid;

pub const JWT_SECRET: &str = "gateway-test-secret-gateway-test!!";
pub const INTERNAL_KEY: &str = "internal-key-internal-key-internal!!";

#[allow(unused_imports)] // used by identity.rs; other test crates don't need it
pub use ::common::jwt;

pub fn config(accounts: &str, configs: &str) -> Config {
    Config {
        port: 0,
        jwt_secret: JWT_SECRET.into(),
        internal_key: INTERNAL_KEY.into(),
        accounts_http_url: accounts.into(),
        accounts_grpc_url: String::new(),
        configs_http_url: configs.into(),
        site_origin: "http://localhost:5173".into(),
        downloads_dir: std::env::temp_dir()
            .join("lovisual-downloads-test")
            .display()
            .to_string(),
        trust_proxy: true,
    }
}

/// Accepts exactly one device token, mapped to one account.
pub struct FakeDevices {
    pub token: String,
    pub account: Uuid,
}

impl DeviceAuthenticator for FakeDevices {
    fn authenticate<'a>(&'a self, token: &'a str) -> DeviceAuthFuture<'a> {
        Box::pin(async move {
            if token == self.token {
                DeviceAuth::Valid(self.account)
            } else {
                DeviceAuth::Invalid
            }
        })
    }
}

/// accounts-service unreachable: every device token lookup fails.
pub struct DownDevices;

impl DeviceAuthenticator for DownDevices {
    fn authenticate<'a>(&'a self, _token: &'a str) -> DeviceAuthFuture<'a> {
        Box::pin(async { DeviceAuth::Unavailable })
    }
}

/// An access JWT signed with the right key but already expired.
pub fn expired_access_token(account: Uuid) -> String {
    let exp = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .expect("clock after epoch")
        .as_secs() as usize
        - 60;
    let claims = ::common::jwt::Claims {
        sub: account.to_string(),
        exp,
        token_type: ::common::jwt::TokenType::Access,
    };
    jsonwebtoken::encode(
        &jsonwebtoken::Header::new(jsonwebtoken::Algorithm::HS256),
        &claims,
        &jsonwebtoken::EncodingKey::from_secret(JWT_SECRET.as_bytes()),
    )
    .expect("encode test jwt")
}

pub fn no_devices() -> Arc<dyn DeviceAuthenticator> {
    Arc::new(FakeDevices {
        token: "lvd_none".into(),
        account: Uuid::nil(),
    })
}

/// Starts a fake service that echoes what it received as JSON; returns its base URL.
pub async fn spawn_echo() -> String {
    async fn echo(req: Request) -> Json<serde_json::Value> {
        let (parts, body) = req.into_parts();
        let body: Bytes = axum::body::to_bytes(body, usize::MAX)
            .await
            .unwrap_or_default();
        let header = |name: &str| {
            parts
                .headers
                .get(name)
                .and_then(|v| v.to_str().ok())
                .map(str::to_owned)
        };
        Json(serde_json::json!({
            "method": parts.method.as_str(),
            "path": parts.uri.path(),
            "query": parts.uri.query(),
            "body": String::from_utf8_lossy(&body),
            "account_id": header("x-lovisual-account-id"),
            "internal_key": header("x-lovisual-internal-key"),
            "authorization": header("authorization"),
        }))
    }
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let addr = listener.local_addr().unwrap();
    tokio::spawn(async move {
        axum::serve(listener, Router::new().fallback(any(echo)))
            .await
            .unwrap()
    });
    format!("http://{addr}")
}

/// Sends a request straight into the router, bypassing the test client's URL
/// handling, so the raw path (`..`, `%2e`, `//`) reaches the gateway as-is.
pub async fn raw(
    app: &axum::Router,
    method: &str,
    uri: &str,
    forwarded_for: &str,
) -> (axum::http::StatusCode, axum::http::HeaderMap, String) {
    use tower::ServiceExt;
    let req = axum::http::Request::builder()
        .method(method)
        .uri(uri)
        .header("x-forwarded-for", forwarded_for)
        .body(axum::body::Body::empty())
        .expect("valid raw request");
    let res = app.clone().oneshot(req).await.expect("infallible router");
    let (parts, body) = res.into_parts();
    let bytes = axum::body::to_bytes(body, usize::MAX)
        .await
        .unwrap_or_default();
    (
        parts.status,
        parts.headers,
        String::from_utf8_lossy(&bytes).into_owned(),
    )
}
