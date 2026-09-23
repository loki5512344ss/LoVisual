#[tokio::test]
async fn migrations_create_accounts_table() {
    let database_url = std::env::var("DATABASE_URL")
        .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
    let pool = sqlx::PgPool::connect(&database_url).await.expect("connect");

    sqlx::migrate!("./migrations").run(&pool).await.expect("migrate");

    let row: (i64,) = sqlx::query_as("SELECT count(*) FROM accounts")
        .fetch_one(&pool)
        .await
        .expect("accounts table must exist");
    assert_eq!(row.0, 0);
}
