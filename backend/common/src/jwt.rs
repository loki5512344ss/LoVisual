use axum::http::{HeaderMap, header::AUTHORIZATION};
use jsonwebtoken::{Algorithm, DecodingKey, EncodingKey, Header, Validation, decode, encode};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// Distinguishes token purposes so a token kind can never be replayed as
/// another. Only access tokens are JWTs today; refresh/device tokens are
/// opaque secrets. The claim stays so future kinds remain distinguishable.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum TokenType {
    Access,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub sub: String,
    pub exp: usize,
    pub token_type: TokenType,
}

fn issue(account_id: Uuid, secret: &str, token_type: TokenType, ttl_seconds: i64) -> String {
    let exp = (chrono::Utc::now() + chrono::Duration::seconds(ttl_seconds)).timestamp() as usize;
    let claims = Claims {
        sub: account_id.to_string(),
        exp,
        token_type,
    };
    encode(
        &Header::new(Algorithm::HS256),
        &claims,
        &EncodingKey::from_secret(secret.as_bytes()),
    )
    .expect("encoding a well-formed Claims struct cannot fail")
}

pub fn issue_access_token(account_id: Uuid, secret: &str) -> String {
    issue(account_id, secret, TokenType::Access, 15 * 60)
}

/// Returns the claims only if the signature, expiry AND token type all match.
/// HS256 is pinned explicitly (no algorithm confusion) and expiry leeway is 0.
pub fn verify_token(token: &str, secret: &str, expected: TokenType) -> Option<Claims> {
    let mut validation = Validation::new(Algorithm::HS256);
    validation.leeway = 0;
    validation.set_required_spec_claims(&["exp", "sub"]);
    let claims = decode::<Claims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &validation,
    )
    .ok()?
    .claims;
    (claims.token_type == expected).then_some(claims)
}

/// The raw token from `Authorization: Bearer <token>`, if the header is
/// present, valid ASCII, uses the Bearer scheme and is non-empty.
pub fn bearer_token(headers: &HeaderMap) -> Option<&str> {
    let value = headers.get(AUTHORIZATION)?.to_str().ok()?;
    let token = value.strip_prefix("Bearer ")?.trim();
    (!token.is_empty()).then_some(token)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn access_token_round_trips() {
        let id = Uuid::new_v4();
        let token = issue_access_token(id, "test-secret");
        let claims = verify_token(&token, "test-secret", TokenType::Access).expect("should decode");
        assert_eq!(claims.sub, id.to_string());
        assert_eq!(claims.token_type, TokenType::Access);
    }

    #[test]
    fn wrong_secret_fails_verify() {
        let token = issue_access_token(Uuid::new_v4(), "test-secret");
        assert!(verify_token(&token, "other-secret", TokenType::Access).is_none());
    }

    #[test]
    fn garbage_token_fails_verify() {
        assert!(verify_token("not.a.jwt", "test-secret", TokenType::Access).is_none());
    }

    #[test]
    fn expired_token_fails_verify() {
        let token = issue(Uuid::new_v4(), "test-secret", TokenType::Access, -10);
        assert!(verify_token(&token, "test-secret", TokenType::Access).is_none());
    }

    #[test]
    fn token_signed_with_other_algorithm_is_rejected() {
        let claims = Claims {
            sub: Uuid::new_v4().to_string(),
            exp: (chrono::Utc::now().timestamp() + 600) as usize,
            token_type: TokenType::Access,
        };
        let hs512 = encode(
            &Header::new(Algorithm::HS512),
            &claims,
            &EncodingKey::from_secret(b"test-secret"),
        )
        .unwrap();
        assert!(verify_token(&hs512, "test-secret", TokenType::Access).is_none());
    }

    #[test]
    fn bearer_token_extracts_the_token() {
        let mut h = HeaderMap::new();
        h.insert(AUTHORIZATION, "Bearer abc.def".parse().unwrap());
        assert_eq!(bearer_token(&h), Some("abc.def"));
    }

    #[test]
    fn bearer_token_rejects_missing_wrong_scheme_and_empty() {
        let mut h = HeaderMap::new();
        assert_eq!(bearer_token(&h), None);
        h.insert(AUTHORIZATION, "Basic abc".parse().unwrap());
        assert_eq!(bearer_token(&h), None);
        h.insert(AUTHORIZATION, "Bearer   ".parse().unwrap());
        assert_eq!(bearer_token(&h), None);
    }

    #[test]
    fn bearer_token_passes_opaque_value_through() {
        let mut h = HeaderMap::new();
        h.insert(AUTHORIZATION, "Bearer not.a.jwt".parse().unwrap());
        assert_eq!(bearer_token(&h), Some("not.a.jwt"));
    }
}
