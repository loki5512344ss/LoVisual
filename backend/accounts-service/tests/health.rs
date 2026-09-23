use axum_test::TestServer;

#[tokio::test]
async fn health_returns_ok() {
    // NOTE: this test does not touch the DB — pass a pool that is never
    // queried. sqlx::PgPool::connect_lazy never opens a connection until
    // a query runs, so this is safe without a running Postgres.
    let pool = sqlx::PgPool::connect_lazy("postgres://user:pass@localhost/db")
        .expect("lazy pool");
    let app = accounts_service::build_app(pool);
    let server = TestServer::new(app);

    let response = server.get("/health").await;
    response.assert_status_ok();
    response.assert_text("ok");
}
