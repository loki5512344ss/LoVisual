use super::model::Account;
use sqlx::PgPool;
use uuid::Uuid;

// Emails are stored lowercase and compared via the `lower(email)` unique
// index (see migrations/0001_init.sql). Every entry point normalizes here.
pub fn normalize_email(email: &str) -> String {
    email.trim().to_lowercase()
}

pub async fn create(
    pool: &PgPool,
    email: &str,
    password_hash: &str,
    nick: &str,
) -> Result<Account, sqlx::Error> {
    sqlx::query_as::<_, Account>(
        "INSERT INTO accounts (email, password_hash, display_nick)
         VALUES ($1, $2, $3)
         RETURNING id, email, password_hash, display_nick, role, can_publish_addons,
                   email_verified_at, created_at",
    )
    .bind(normalize_email(email))
    .bind(password_hash)
    .bind(nick)
    .fetch_one(pool)
    .await
}

pub async fn find_by_email(pool: &PgPool, email: &str) -> Result<Option<Account>, sqlx::Error> {
    sqlx::query_as::<_, Account>(
        "SELECT id, email, password_hash, display_nick, role, can_publish_addons,
                email_verified_at, created_at
         FROM accounts WHERE lower(email) = $1",
    )
    .bind(normalize_email(email))
    .fetch_optional(pool)
    .await
}

pub async fn find_by_id(pool: &PgPool, id: Uuid) -> Result<Option<Account>, sqlx::Error> {
    sqlx::query_as::<_, Account>(
        "SELECT id, email, password_hash, display_nick, role, can_publish_addons,
                email_verified_at, created_at
         FROM accounts WHERE id = $1",
    )
    .bind(id)
    .fetch_optional(pool)
    .await
}

pub async fn set_avatar(pool: &PgPool, account_id: Uuid, s3_key: &str) -> Result<(), sqlx::Error> {
    sqlx::query(
        "INSERT INTO avatars (account_id, s3_key) VALUES ($1, $2)
         ON CONFLICT (account_id) DO UPDATE SET s3_key = EXCLUDED.s3_key, uploaded_at = now()",
    )
    .bind(account_id)
    .bind(s3_key)
    .execute(pool)
    .await?;
    Ok(())
}

/// Deletes the `avatars` row for one account. Returns `true` when a row existed
/// (so the caller can decide whether to also delete the S3 object) and `false`
/// when the user simply had no avatar — the API still treats that as success.
pub async fn clear_avatar(pool: &PgPool, account_id: Uuid) -> Result<bool, sqlx::Error> {
    let result = sqlx::query("DELETE FROM avatars WHERE account_id = $1")
        .bind(account_id)
        .execute(pool)
        .await?;
    Ok(result.rows_affected() > 0)
}

/// Avatar reference for one account: its storage key plus an `uploaded_at`
/// version (epoch seconds) used to cache-bust the served image URL.
pub async fn avatar_ref(
    pool: &PgPool,
    account_id: Uuid,
) -> Result<Option<(String, i64)>, sqlx::Error> {
    let row: Option<(String, i64)> = sqlx::query_as(
        "SELECT s3_key, EXTRACT(EPOCH FROM uploaded_at)::bigint FROM avatars WHERE account_id = $1",
    )
    .bind(account_id)
    .fetch_optional(pool)
    .await?;
    Ok(row)
}

/// Nick + avatar (key, version) for the given accounts (showcase authors etc.).
/// Missing ids are simply absent from the result.
pub async fn public_profiles(
    pool: &PgPool,
    ids: &[Uuid],
) -> Result<Vec<(Uuid, String, Option<String>, Option<i64>)>, sqlx::Error> {
    sqlx::query_as(
        "SELECT a.id, a.display_nick, av.s3_key, EXTRACT(EPOCH FROM av.uploaded_at)::bigint
         FROM accounts a
         LEFT JOIN avatars av ON av.account_id = a.id
         WHERE a.id = ANY($1)",
    )
    .bind(ids)
    .fetch_all(pool)
    .await
}

/// How many accounts existed before `created_at`; 0 means the very first one.
pub async fn account_rank(
    pool: &PgPool,
    created_at: chrono::DateTime<chrono::Utc>,
) -> Result<i64, sqlx::Error> {
    sqlx::query_scalar("SELECT count(*) FROM accounts WHERE created_at < $1")
        .bind(created_at)
        .fetch_one(pool)
        .await
}

#[cfg(test)]
mod tests {
    use super::*;

    async fn test_pool() -> PgPool {
        let url = std::env::var("DATABASE_URL")
            .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
        let pool = PgPool::connect(&url).await.expect("connect");
        sqlx::migrate!("./migrations")
            .run(&pool)
            .await
            .expect("migrate");
        pool
    }

    #[tokio::test]
    async fn create_then_find_by_email_round_trips() {
        let pool = test_pool().await;
        let email = format!("test-{}@example.com", Uuid::new_v4());

        let created = create(&pool, &email, "hash123", "TestNick").await.unwrap();
        assert_eq!(created.email, email);
        assert_eq!(created.role, "user");
        assert!(created.can_publish_addons);

        let found = find_by_email(&pool, &email)
            .await
            .unwrap()
            .expect("must exist");
        assert_eq!(found.id, created.id);

        sqlx::query("DELETE FROM accounts WHERE id = $1")
            .bind(created.id)
            .execute(&pool)
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn find_by_email_returns_none_for_missing() {
        let pool = test_pool().await;
        let result = find_by_email(&pool, "does-not-exist@example.com")
            .await
            .unwrap();
        assert!(result.is_none());
    }

    #[tokio::test]
    async fn email_lookup_is_case_insensitive_and_stored_lowercase() {
        let pool = test_pool().await;
        let tag = Uuid::new_v4();
        let mixed = format!("MiXeD-{tag}@Example.COM");

        let created = create(&pool, &mixed, "hash123", "Nick").await.unwrap();
        assert_eq!(created.email, mixed.to_lowercase());

        let found = find_by_email(&pool, &mixed.to_uppercase())
            .await
            .unwrap()
            .expect("lookup must ignore case");
        assert_eq!(found.id, created.id);

        sqlx::query("DELETE FROM accounts WHERE id = $1")
            .bind(created.id)
            .execute(&pool)
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn duplicate_email_differing_only_by_case_is_rejected() {
        let pool = test_pool().await;
        let tag = Uuid::new_v4();
        let first = create(&pool, &format!("dup-{tag}@example.com"), "h", "A")
            .await
            .unwrap();

        let second = create(&pool, &format!("DUP-{tag}@EXAMPLE.com"), "h", "B").await;
        let err = second.expect_err("case-variant duplicate must violate the unique index");
        match err {
            sqlx::Error::Database(db) => {
                assert_eq!(db.constraint(), Some("accounts_email_lower_idx"));
            }
            other => panic!("expected a database unique violation, got {other:?}"),
        }

        sqlx::query("DELETE FROM accounts WHERE id = $1")
            .bind(first.id)
            .execute(&pool)
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn set_avatar_inserts_then_updates_in_place() {
        let pool = test_pool().await;
        let created = create(
            &pool,
            &format!("av-{}@example.com", Uuid::new_v4()),
            "h",
            "N",
        )
        .await
        .unwrap();

        set_avatar(&pool, created.id, "avatars/one.png")
            .await
            .unwrap();
        set_avatar(&pool, created.id, "avatars/two.png")
            .await
            .unwrap();

        let rows: Vec<(String,)> =
            sqlx::query_as("SELECT s3_key FROM avatars WHERE account_id = $1")
                .bind(created.id)
                .fetch_all(&pool)
                .await
                .unwrap();
        assert_eq!(rows, vec![("avatars/two.png".to_string(),)]);

        sqlx::query("DELETE FROM accounts WHERE id = $1")
            .bind(created.id)
            .execute(&pool)
            .await
            .unwrap();
    }
}
