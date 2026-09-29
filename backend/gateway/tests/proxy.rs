mod common;

use axum::http::StatusCode;

async fn server() -> axum_test::TestServer {
    let accounts = common::spawn_echo().await;
    let configs = common::spawn_echo().await;
    let app = gateway::build_app(&common::config(&accounts, &configs), common::no_devices());
    axum_test::TestServer::new(app)
}

#[tokio::test]
async fn health_is_ok() {
    server().await.get("/health").await.assert_status_ok();
}

#[tokio::test]
async fn forwards_method_path_query_and_body() {
    let res = server().await.put("/configs/2?x=1").text("payload").await;
    res.assert_status_ok();
    let echo: serde_json::Value = res.json();
    assert_eq!(echo["method"], "PUT");
    assert_eq!(echo["path"], "/configs/2");
    assert_eq!(echo["query"], "x=1");
    assert_eq!(echo["body"], "payload");
}

#[tokio::test]
async fn adds_internal_key_and_strips_spoofed_identity() {
    let res = server()
        .await
        .post("/auth/login")
        .add_header("x-lovisual-account-id", uuid::Uuid::new_v4().to_string())
        .add_header("x-lovisual-internal-key", "spoofed")
        .await;
    let echo: serde_json::Value = res.json();
    assert_eq!(echo["internal_key"], common::INTERNAL_KEY);
    assert!(echo["account_id"].is_null());
}

#[tokio::test]
async fn unknown_prefix_is_404() {
    server()
        .await
        .get("/nope")
        .await
        .assert_status(StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn dead_upstream_is_502() {
    let app = gateway::build_app(
        &common::config("http://127.0.0.1:1", "http://127.0.0.1:1"),
        common::no_devices(),
    );
    axum_test::TestServer::new(app)
        .get("/me")
        .await
        .assert_status(StatusCode::BAD_GATEWAY);
}

#[tokio::test]
async fn oversized_body_is_413() {
    let big = "x".repeat(6 * 1024 * 1024 + 1);
    server()
        .await
        .put("/configs/1")
        .text(big)
        .await
        .assert_status(StatusCode::PAYLOAD_TOO_LARGE);
}

#[tokio::test]
async fn cors_preflight_allows_only_the_site_origin() {
    let app = gateway::build_app(
        &common::config("http://127.0.0.1:1", "http://127.0.0.1:1"),
        common::no_devices(),
    );
    let s = axum_test::TestServer::new(app);
    let ok = s
        .method(axum::http::Method::OPTIONS, "/auth/login")
        .add_header("origin", "http://localhost:5173")
        .add_header("access-control-request-method", "POST")
        .await;
    assert_eq!(
        ok.header("access-control-allow-origin"),
        "http://localhost:5173"
    );
    assert_eq!(ok.header("access-control-allow-credentials"), "true");

    let evil = s
        .method(axum::http::Method::OPTIONS, "/auth/login")
        .add_header("origin", "https://evil.example")
        .add_header("access-control-request-method", "POST")
        .await;
    assert!(evil.maybe_header("access-control-allow-origin").is_none());
}

#[tokio::test]
async fn cors_exposes_retry_after_so_js_can_read_it() {
    let s = server().await;
    let res = s
        .post("/auth/login")
        .add_header("origin", "http://localhost:5173")
        .await;
    assert_eq!(res.header("access-control-expose-headers"), "retry-after");
}
