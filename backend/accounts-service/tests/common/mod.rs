#![allow(dead_code, unused_imports)] // each test binary uses a different subset

use accounts_service::config::Config;
use axum_test::TestServer;
use uuid::Uuid;

// `::` — the workspace crate, not this module (which every test binary
// mounts as `mod common;`).
pub use ::common::internal::{ACCOUNT_ID_HEADER, INTERNAL_KEY_HEADER};

pub fn init_test_logging() {
    let _ = tracing_subscriber::fmt::try_init();
}

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
        avatar_public_base_url: String::new(),
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

/// Creates the test avatar bucket if it does not exist yet, so the avatar
/// tests are self-contained: any S3-compatible backend (MinIO, SeaweedFS)
/// works without external `mc mb` bootstrap. Mirrors `S3Storage::from_config`.
/// Retries briefly so a just-started container does not race the test.
pub async fn ensure_avatar_bucket() {
    let creds =
        aws_sdk_s3::config::Credentials::new("minioadmin", "minioadmin", None, None, "static");
    let config = aws_sdk_s3::config::Builder::new()
        .endpoint_url("http://localhost:9000")
        .credentials_provider(creds)
        .region(aws_sdk_s3::config::Region::new("us-east-1"))
        .force_path_style(true)
        .behavior_version(aws_sdk_s3::config::BehaviorVersion::latest())
        .build();
    let client = aws_sdk_s3::Client::from_conf(config);
    let mut last_err = String::new();
    for _ in 0..30 {
        // Probe writability, not just bucket existence: a freshly started
        // S3 backend may still be electing volumes ("Not enough data nodes"
        // on SeaweedFS) right after create_bucket succeeds.
        match async {
            // Already-exists is success (MinIO: BucketAlreadyOwnedByYou,
            // SeaweedFS: BucketAlreadyExists); anything else aborts.
            if let Err(e) = client
                .create_bucket()
                .bucket("lovisual-avatars-test")
                .send()
                .await
            {
                let msg = format!("{e:?}");
                if !msg.contains("BucketAlready") {
                    return Err(msg);
                }
            }
            client
                .put_object()
                .bucket("lovisual-avatars-test")
                .key(".probe")
                .body(aws_sdk_s3::primitives::ByteStream::from_static(b"probe"))
                .send()
                .await
                .map_err(|e| format!("{e:?}"))?;
            let _ = client
                .delete_object()
                .bucket("lovisual-avatars-test")
                .key(".probe")
                .send()
                .await;
            Ok::<(), String>(())
        }
        .await
        {
            Ok(_) => return,
            Err(msg) => {
                last_err = msg;
                tokio::time::sleep(std::time::Duration::from_secs(1)).await;
            }
        }
    }
    panic!("S3 backend is not writable: {last_err}");
}
