use axum_extra::extract::cookie::{Cookie, SameSite};
use base64::{Engine, engine::general_purpose::URL_SAFE_NO_PAD};
use rand::RngExt;
use sha2::{Digest, Sha256};
use sqlx::PgPool;
use uuid::Uuid;

pub const REFRESH_COOKIE: &str = "lv_refresh";

/// 256-bit random token: nothing to brute-force, so a slow hash would only
/// add latency to every refresh — plain SHA-256 for storage is enough.
pub fn new_opaque_token(prefix: &str) -> String {
    let mut bytes = [0u8; 32];
    rand::rng().fill(&mut bytes);
    format!("{prefix}{}", URL_SAFE_NO_PAD.encode(bytes))
}

pub fn hash_token(token: &str) -> String {
    hex::encode(Sha256::digest(token.as_bytes()))
}

pub async fn store_refresh(pool: &PgPool, account_id: Uuid) -> Result<String, sqlx::Error> {
    let token = new_opaque_token("lvr_");
    sqlx::query(
        "INSERT INTO refresh_tokens (account_id, token_hash, expires_at)
         VALUES ($1, $2, now() + interval '30 days')",
    )
    .bind(account_id)
    .bind(hash_token(&token))
    .execute(pool)
    .await?;
    Ok(token)
}

pub enum RotateOutcome {
    Rotated { account_id: Uuid, new_token: String },
    Invalid,
}

pub async fn rotate_refresh(pool: &PgPool, token: &str) -> Result<RotateOutcome, sqlx::Error> {
    let mut tx = pool.begin().await?;
    let row: Option<(Uuid, bool, bool)> = sqlx::query_as(
        "SELECT account_id, revoked_at IS NOT NULL, expires_at <= now()
         FROM refresh_tokens WHERE token_hash = $1 FOR UPDATE",
    )
    .bind(hash_token(token))
    .fetch_optional(&mut *tx)
    .await?;

    let outcome = match row {
        None => RotateOutcome::Invalid,
        Some((account_id, true, _)) => {
            // Reuse of a rotated token: someone else holds a copy. Kill all sessions.
            sqlx::query(
                "UPDATE refresh_tokens SET revoked_at = now()
                 WHERE account_id = $1 AND revoked_at IS NULL",
            )
            .bind(account_id)
            .execute(&mut *tx)
            .await?;
            RotateOutcome::Invalid
        }
        Some((_, false, true)) => RotateOutcome::Invalid,
        Some((account_id, false, false)) => {
            sqlx::query("UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = $1")
                .bind(hash_token(token))
                .execute(&mut *tx)
                .await?;
            let new_token = new_opaque_token("lvr_");
            sqlx::query(
                "INSERT INTO refresh_tokens (account_id, token_hash, expires_at)
                 VALUES ($1, $2, now() + interval '30 days')",
            )
            .bind(account_id)
            .bind(hash_token(&new_token))
            .execute(&mut *tx)
            .await?;
            RotateOutcome::Rotated { account_id, new_token }
        }
    };
    tx.commit().await?;
    Ok(outcome)
}

pub async fn revoke_refresh(pool: &PgPool, token: &str) -> Result<(), sqlx::Error> {
    sqlx::query(
        "UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = $1 AND revoked_at IS NULL",
    )
    .bind(hash_token(token))
    .execute(pool)
    .await?;
    Ok(())
}

/// The refresh token never touches JS: httpOnly, Strict, scoped to /auth,
/// so an XSS on the site cannot exfiltrate it.
pub fn refresh_cookie(token: String, secure: bool) -> Cookie<'static> {
    Cookie::build((REFRESH_COOKIE, token))
        .http_only(true)
        .secure(secure)
        .same_site(SameSite::Strict)
        .path("/auth")
        .max_age(time::Duration::days(30))
        .build()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn opaque_tokens_are_prefixed_unique_and_long() {
        let a = new_opaque_token("lvr_");
        let b = new_opaque_token("lvr_");
        assert!(a.starts_with("lvr_"));
        assert_eq!(a.len(), 4 + 43);
        assert_ne!(a, b);
    }

    #[test]
    fn hash_is_stable_hex_sha256() {
        assert_eq!(hash_token("x"), hash_token("x"));
        assert_eq!(hash_token("x").len(), 64);
        assert_ne!(hash_token("x"), hash_token("y"));
    }

    #[test]
    fn refresh_cookie_has_the_hardened_attributes() {
        let cookie = refresh_cookie("t".into(), true);
        assert_eq!(cookie.name(), REFRESH_COOKIE);
        assert_eq!(cookie.http_only(), Some(true));
        assert_eq!(cookie.secure(), Some(true));
        assert_eq!(cookie.same_site(), Some(SameSite::Strict));
        assert_eq!(cookie.path(), Some("/auth"));
    }
}
