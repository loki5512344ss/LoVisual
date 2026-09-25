mod common;

use axum_test::TestServer;

#[tokio::test]
async fn health_returns_ok() {
    // NOTE: this test does not touch the DB — pass a pool that is never
    // queried. sqlx::PgPool::connect_lazy never opens a connection until
    // a query runs, so this is safe without a running Postgres.
    let pool = sqlx::PgPool::connect_lazy("postgres://user:pass@localhost/db").expect("lazy pool");
    let app = accounts_service::build_app(pool, &common::test_config());
    let server = TestServer::new(app);

    let response = server.get("/health").await;
    response.assert_status_ok();
    response.assert_text("ok");
}

#[tokio::test]
async fn migrations_create_accounts_table() {
    let pool = common::test_pool().await;

    let row: (Option<String>,) = sqlx::query_as("SELECT to_regclass('accounts')::text")
        .fetch_one(&pool)
        .await
        .expect("query must succeed");
    assert!(
        row.0.is_some(),
        "accounts table must exist after migrations run"
    );
}

#[tokio::test]
async fn api_routes_require_internal_key_but_health_does_not() {
    let pool = common::test_pool().await;
    // A raw server: no internal key header on any request, as if the
    // service were reached directly, bypassing the gateway.
    let server =
        axum_test::TestServer::new(accounts_service::build_app(pool, &common::test_config()));
    server.get("/health").await.assert_status_ok();
    server
        .post("/device/code")
        .await
        .assert_status(axum::http::StatusCode::FORBIDDEN);
}

#[tokio::test]
async fn public_profile_is_reachable_without_identity_but_never_leaks_private_fields() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (id, _email) = common::register_account(&server).await;

    // No identity header: public data must not need a logged-in caller.
    let res = server.get(&format!("/users/{id}")).await;
    res.assert_status_ok();
    let body: serde_json::Value = res.json();
    assert_eq!(body["id"], id.to_string());
    assert_eq!(body["display_nick"], "Tester");
    assert!(body["avatar_url"].is_null());
    assert!(body.get("email").is_none(), "email must never be public");
    assert!(body.get("role").is_none(), "role must never be public");
    assert!(
        body["badges"]
            .as_array()
            .expect("badges array")
            .iter()
            .any(|b| b == "early")
    );

    server
        .get(&format!("/users/{}", uuid::Uuid::new_v4()))
        .await
        .assert_status(axum::http::StatusCode::NOT_FOUND);
    server
        .get("/users/not-a-uuid")
        .await
        .assert_status(axum::http::StatusCode::NOT_FOUND);

    sqlx::query("DELETE FROM accounts WHERE id = $1")
        .bind(id)
        .execute(&common::test_pool().await)
        .await
        .expect("cleanup");
}
