mod common;

use axum_test::TestServer;

#[tokio::test]
async fn health_returns_ok() {
    // NOTE: this test does not touch the DB — pass a pool that is never
    // queried. sqlx::PgPool::connect_lazy never opens a connection until
    // a query runs, so this is safe without a running Postgres.
    let pool = sqlx::PgPool::connect_lazy("postgres://user:pass@localhost/db")
        .expect("lazy pool");
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
    assert!(row.0.is_some(), "accounts table must exist after migrations run");
}

#[tokio::test]
async fn api_routes_require_internal_key_but_health_does_not() {
    let pool = common::test_pool().await;
    // A raw server: no internal key header on any request, as if the
    // service were reached directly, bypassing the gateway.
    let server = axum_test::TestServer::new(accounts_service::build_app(pool, &common::test_config()));
    server.get("/health").await.assert_status_ok();
    server
        .post("/device/code")
        .await
        .assert_status(axum::http::StatusCode::FORBIDDEN);
}
