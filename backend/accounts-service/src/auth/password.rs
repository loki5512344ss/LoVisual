use argon2::password_hash::phc::PasswordHash;
// `as _`: the trait must be in scope for `hash_password`, but the name
// belongs to the `PasswordHasher` struct below.
use argon2::{Algorithm, Argon2, Params, PasswordHasher as _, PasswordVerifier, Version};
use std::sync::Arc;
use tokio::sync::Semaphore;

// Explicit Argon2id parameters instead of `Argon2::default()`, so a
// dependency bump can never silently weaken the cost (the library default
// is the OWASP *minimum*: m=19 MiB, t=2). 64 MiB / t=3 / p=1 is
// deliberately above that floor.
const M_COST_KIB: u32 = 64 * 1024;
const T_COST: u32 = 3;
const P_COST: u32 = 1;

// Argon2 processes the whole input, so an unbounded password is a cheap
// CPU/memory DoS vector on both register and login. bcrypt-style 72-byte
// truncation is not a concern for Argon2; this is purely an abuse cap.
pub const MAX_PASSWORD_BYTES: usize = 256;

fn hasher() -> Result<Argon2<'static>, argon2::password_hash::Error> {
    let params = Params::new(M_COST_KIB, T_COST, P_COST, None)
        .map_err(|_| argon2::password_hash::Error::ParamsInvalid)?;
    Ok(Argon2::new(Algorithm::Argon2id, Version::V0x13, params))
}

pub fn hash_password(plain: &str) -> Result<String, argon2::password_hash::Error> {
    if plain.len() > MAX_PASSWORD_BYTES {
        return Err(argon2::password_hash::Error::PasswordInvalid);
    }
    Ok(hasher()?.hash_password(plain.as_bytes())?.to_string())
}

pub fn verify_password(plain: &str, hash: &str) -> bool {
    if plain.len() > MAX_PASSWORD_BYTES {
        return false;
    }
    let Ok(parsed) = PasswordHash::new(hash) else {
        return false;
    };
    // Parameters are read from the stored PHC string, so hashes created
    // under older/lower settings still verify after a cost increase.
    Argon2::default()
        .verify_password(plain.as_bytes(), &parsed)
        .is_ok()
}

/// Argon2 (64 MiB, ~100ms) is CPU/memory heavy; without a cap, concurrent
/// register/login requests could starve the tokio worker pool. Sized to the
/// number of CPUs so hashing never oversubscribes them.
#[derive(Clone)]
pub struct PasswordHasher {
    permits: Arc<Semaphore>,
}

impl Default for PasswordHasher {
    fn default() -> Self {
        Self::new()
    }
}

impl PasswordHasher {
    pub fn new() -> Self {
        let permits = std::thread::available_parallelism()
            .map(|n| n.get())
            .unwrap_or(2);
        PasswordHasher { permits: Arc::new(Semaphore::new(permits)) }
    }

    /// Runs Argon2 hashing off the async workers, bounded by the permit count.
    pub async fn hash(&self, plain: String) -> Result<String, anyhow::Error> {
        let _permit = self.permits.acquire().await?;
        let inner = tokio::task::spawn_blocking(move || hash_password(&plain))
            .await
            .map_err(|e| anyhow::anyhow!(e))??;
        Ok(inner)
    }

    /// Runs Argon2 verification off the async workers, bounded by the permit
    /// count. Used for both real and dummy (timing-equalizer) verification.
    pub async fn verify(&self, plain: String, hash: String) -> Result<bool, anyhow::Error> {
        let _permit = self.permits.acquire().await?;
        tokio::task::spawn_blocking(move || verify_password(&plain, &hash))
            .await
            .map_err(|e| anyhow::anyhow!(e))
    }
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

    #[test]
    fn hash_uses_pinned_argon2id_params() {
        let hash = hash_password("secret123").unwrap();
        assert!(
            hash.starts_with("$argon2id$v=19$m=65536,t=3,p=1$"),
            "unexpected PHC prefix: {hash}"
        );
    }

    #[test]
    fn same_password_hashes_differently_each_time() {
        let a = hash_password("secret123").unwrap();
        let b = hash_password("secret123").unwrap();
        assert_ne!(a, b, "salt must be random per hash");
    }

    #[test]
    fn password_length_cap_boundary() {
        let at_cap = "a".repeat(MAX_PASSWORD_BYTES);
        let hash = hash_password(&at_cap).expect("exactly the cap must be accepted");
        assert!(verify_password(&at_cap, &hash));

        let over_cap = "a".repeat(MAX_PASSWORD_BYTES + 1);
        assert!(hash_password(&over_cap).is_err(), "cap+1 must be rejected");
        assert!(!verify_password(&over_cap, &hash));
    }

    #[test]
    fn garbage_hash_string_fails_verify_without_panicking() {
        assert!(!verify_password("whatever", "not-a-phc-string"));
    }
}
