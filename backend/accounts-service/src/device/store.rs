use dashmap::DashMap;
use std::sync::Arc;
use std::time::{Duration, Instant};
use uuid::Uuid;

#[derive(Clone)]
pub struct DeviceCodeEntry {
    pub user_code: String,
    pub confirmed_account_id: Option<Uuid>,
    pub expires_at: Instant,
}

#[derive(Clone, Default)]
pub struct DeviceStore {
    by_device_code: Arc<DashMap<String, DeviceCodeEntry>>,
}

#[derive(Debug, PartialEq, Eq)]
pub enum PollResult {
    Unknown,
    Pending,
    Confirmed(Uuid),
}

pub(crate) const TTL: Duration = Duration::from_secs(600);
// /device/code is unauthenticated, so the store must be bounded or anyone can
// grow process memory without limit. Expired entries are purged on every
// create, so the cap only bites under sustained abuse.
const MAX_PENDING: usize = 10_000;

fn random_user_code() -> String {
    use rand::RngExt;
    const ALPHABET: &[u8] = b"ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no O/0/I/1 confusion
    let mut rng = rand::rng();
    let mut part = |n: usize| -> String {
        (0..n).map(|_| ALPHABET[rng.random_range(0..ALPHABET.len())] as char).collect()
    };
    format!("{}-{}", part(4), part(4))
}

impl DeviceStore {
    /// Returns `(device_code, user_code)`, or `None` when the store is full.
    pub fn create(&self) -> Option<(String, String)> {
        let now = Instant::now();
        self.by_device_code.retain(|_, entry| entry.expires_at > now);
        if self.by_device_code.len() >= MAX_PENDING {
            return None;
        }
        let device_code = Uuid::new_v4().to_string();
        let user_code = random_user_code();
        self.by_device_code.insert(
            device_code.clone(),
            DeviceCodeEntry {
                user_code: user_code.clone(),
                confirmed_account_id: None,
                expires_at: now + TTL,
            },
        );
        Some((device_code, user_code))
    }

    /// Returns true if a matching, unexpired entry was found and confirmed.
    /// The user types this code by hand, so it is trimmed and upper-cased.
    pub fn confirm(&self, user_code: &str, account_id: Uuid) -> bool {
        let wanted = user_code.trim().to_uppercase();
        let now = Instant::now();
        for mut entry in self.by_device_code.iter_mut() {
            // First confirm wins: an already-confirmed code cannot be
            // re-bound to a different account before the device collects it.
            if entry.user_code == wanted
                && entry.expires_at > now
                && entry.confirmed_account_id.is_none()
            {
                entry.confirmed_account_id = Some(account_id);
                return true;
            }
        }
        false
    }

    /// A confirmed entry is CONSUMED by the first poll that sees it: the
    /// token can be collected exactly once, replaying the same device_code
    /// afterwards yields `Unknown`. Expired entries are dropped and Unknown.
    pub fn poll(&self, device_code: &str) -> PollResult {
        let now = Instant::now();
        let removed = self.by_device_code.remove_if(device_code, |_, entry| {
            entry.confirmed_account_id.is_some() || entry.expires_at <= now
        });
        if let Some((_, entry)) = removed {
            return match entry.confirmed_account_id {
                Some(id) if entry.expires_at > now => PollResult::Confirmed(id),
                _ => PollResult::Unknown,
            };
        }
        if self.by_device_code.contains_key(device_code) {
            PollResult::Pending
        } else {
            PollResult::Unknown
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn create_returns_distinct_codes() {
        let store = DeviceStore::default();
        let (dc1, uc1) = store.create().unwrap();
        let (dc2, uc2) = store.create().unwrap();
        assert_ne!(dc1, dc2);
        assert_ne!(uc1, uc2);
    }

    #[test]
    fn confirm_then_poll_returns_account_id() {
        let store = DeviceStore::default();
        let (device_code, user_code) = store.create().unwrap();
        let account_id = Uuid::new_v4();

        assert!(store.confirm(&user_code, account_id));
        assert_eq!(store.poll(&device_code), PollResult::Confirmed(account_id));
    }

    #[test]
    fn confirmed_code_can_only_be_collected_once() {
        let store = DeviceStore::default();
        let (device_code, user_code) = store.create().unwrap();
        assert!(store.confirm(&user_code, Uuid::new_v4()));

        assert!(matches!(store.poll(&device_code), PollResult::Confirmed(_)));
        assert_eq!(store.poll(&device_code), PollResult::Unknown, "replay must fail");
    }

    #[test]
    fn poll_before_confirm_is_pending_and_repeatable() {
        let store = DeviceStore::default();
        let (device_code, _user_code) = store.create().unwrap();
        assert_eq!(store.poll(&device_code), PollResult::Pending);
        assert_eq!(store.poll(&device_code), PollResult::Pending);
    }

    #[test]
    fn poll_unknown_code_is_unknown() {
        let store = DeviceStore::default();
        assert_eq!(store.poll("does-not-exist"), PollResult::Unknown);
    }

    #[test]
    fn confirm_unknown_user_code_returns_false() {
        let store = DeviceStore::default();
        assert!(!store.confirm("ZZZZ-ZZZZ", Uuid::new_v4()));
    }

    #[test]
    fn second_confirm_cannot_overwrite_the_first_account() {
        let store = DeviceStore::default();
        let (device_code, user_code) = store.create().unwrap();
        let first = Uuid::new_v4();
        let attacker = Uuid::new_v4();

        assert!(store.confirm(&user_code, first));
        assert!(!store.confirm(&user_code, attacker), "re-confirm must be refused");
        assert_eq!(store.poll(&device_code), PollResult::Confirmed(first));
    }

    #[test]
    fn confirm_accepts_lowercase_and_surrounding_whitespace() {
        let store = DeviceStore::default();
        let (device_code, user_code) = store.create().unwrap();
        let typed = format!("  {}  ", user_code.to_lowercase());
        assert!(store.confirm(&typed, Uuid::new_v4()));
        assert!(matches!(store.poll(&device_code), PollResult::Confirmed(_)));
    }

    #[test]
    fn expired_entries_are_purged_and_store_is_bounded() {
        let store = DeviceStore::default();
        for _ in 0..MAX_PENDING {
            assert!(store.create().is_some());
        }
        assert!(store.create().is_none(), "store must refuse beyond MAX_PENDING");

        // Force everything to be expired; the next create purges and succeeds.
        for mut entry in store.by_device_code.iter_mut() {
            entry.expires_at = Instant::now() - Duration::from_secs(1);
        }
        assert!(store.create().is_some());
        assert_eq!(store.by_device_code.len(), 1);
    }
}
