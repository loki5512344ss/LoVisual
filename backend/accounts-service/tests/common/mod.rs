#![allow(dead_code, unused_imports)] // each test binary uses a different subset

use accounts_service::config::Config;
use axum_test::TestServer;
use uuid::Uuid;

// `::` — the workspace crate, not this module (which every test binary
// mounts as `mod common;`).
pub use ::common::internal::{ACCOUNT_ID_HEADER, INTERNAL_KEY_HEADER};

pub async fn test_pool() -> sqlx::PgPool {
    let url = std::env::var("DATABASE_URL")
        .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
    let pool = sqlx::PgPool::connect(&url)
        .await
        .expect("connect to test database");
    sqlx::migrate!("./migrations")
        .run(&pool)
        .await
        .expect("run migrations");
    pool
}

pub fn test_config() -> Config {
    Config {
        database_url: String::new(),
        jwt_secret: "test-secret-test-secret-test-secret!".into(),
        internal_key: TEST_INTERNAL_KEY.into(),
        port: 0,
        grpc_port: 0,
        s3_endpoint: "http://localhost:9000".into(),
        s3_bucket: "lovisual-avatars-test".into(),
        s3_access_key: "minioadmin".into(),
        s3_secret_key: "minioadmin".into(),
        cookie_secure: false,
    }
}

pub const TEST_INTERNAL_KEY: &str = "internal-key-internal-key-internal!!";

/// A server that behaves like it sits behind the gateway.
pub fn test_server(app: axum::Router) -> TestServer {
    let mut server = TestServer::new(app);
    server.add_header(INTERNAL_KEY_HEADER, TEST_INTERNAL_KEY);
    server
}

/// Registers a fresh random account; returns its id and email.
pub async fn register_account(server: &TestServer) -> (Uuid, String) {
    let email = format!("t-{}@example.com", Uuid::new_v4());
    let res = server
        .post("/auth/register")
        .json(&serde_json::json!({ "email": email, "password": "correct-horse-battery-staple", "nick": "Tester" }))
        .await;
    res.assert_status(axum::http::StatusCode::CREATED);
    let body: serde_json::Value = res.json();
    (
        Uuid::parse_str(body["id"].as_str().unwrap()).unwrap(),
        email,
    )
}
