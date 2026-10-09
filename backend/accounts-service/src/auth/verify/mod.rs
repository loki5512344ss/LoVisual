pub mod handlers;

use sqlx::PgPool;
use uuid::Uuid;

use crate::auth::tokens::{EmailTokenPurpose, consume_email_token, issue_email_token};

/// Verification links live a day: long enough to find the mail, short
/// enough that a leaked link dies before it matters.
const TTL_MINUTES: i64 = 24 * 60;

pub const TOKEN_PREFIX: &str = "lvev_";

/// Same base64url shape as every other opaque token, own prefix so junk is
/// filtered before the database and a reset link can never be mistaken for
/// a verification link by a user pasting it into the wrong page.
pub fn is_well_formed_token(token: &str) -> bool {
    token.len() == TOKEN_PREFIX.len() + 43 && token.starts_with(TOKEN_PREFIX)
}

pub async fn issue_verify_token(pool: &PgPool, account_id: Uuid) -> Result<String, sqlx::Error> {
    issue_email_token(
        pool,
        account_id,
        EmailTokenPurpose::EmailVerify,
        TOKEN_PREFIX,
        TTL_MINUTES,
    )
    .await
}

pub async fn consume_verify_token(pool: &PgPool, token: &str) -> Result<Option<Uuid>, sqlx::Error> {
    consume_email_token(pool, token, EmailTokenPurpose::EmailVerify).await
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::auth::tokens::new_opaque_token;

    #[test]
    fn well_formed_token_shape_matches_opaque_generator() {
        let token = new_opaque_token(TOKEN_PREFIX);
        assert!(is_well_formed_token(&token));
        assert!(!is_well_formed_token(&format!("{token}x")));
        assert!(!is_well_formed_token("lvev_short"));
        // A reset token is never a valid verification token by shape either.
        assert!(!is_well_formed_token(&format!("lvpr_{}", "a".repeat(43))));
    }
}
