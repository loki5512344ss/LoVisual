use argon2::{Argon2, PasswordHasher, PasswordVerifier};
use argon2::password_hash::phc::PasswordHash;

pub fn hash_password(plain: &str) -> Result<String, argon2::password_hash::Error> {
    let argon2 = Argon2::default();
    Ok(argon2.hash_password(plain.as_bytes())?.to_string())
}

pub fn verify_password(plain: &str, hash: &str) -> bool {
    let Ok(parsed) = PasswordHash::new(hash) else { return false };
    Argon2::default()
        .verify_password(plain.as_bytes(), &parsed)
        .is_ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn hash_then_verify_succeeds() {
        let hash = hash_password("correct-horse-battery-staple").unwrap();
        assert!(verify_password("correct-horse-battery-staple", &hash));
    }

    #[test]
    fn wrong_password_fails_verify() {
        let hash = hash_password("correct-horse-battery-staple").unwrap();
        assert!(!verify_password("wrong-password", &hash));
    }

    #[test]
    fn hash_is_not_the_plain_password() {
        let hash = hash_password("secret123").unwrap();
        assert_ne!(hash, "secret123");
    }
}
