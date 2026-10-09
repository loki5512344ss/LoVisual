use super::payload::MAX_WANT;
use std::{
    collections::HashMap,
    sync::Mutex,
    time::{Duration, Instant},
};
use uuid::Uuid;

/// A published payload stops being relayed after this long without a refresh.
pub const ENTRY_TTL: Duration = Duration::from_secs(6);
/// A Minecraft uuid stays claimed by an account this long after its last publish.
pub const CLAIM_TTL: Duration = Duration::from_secs(600);

#[derive(Debug, PartialEq, Eq)]
pub enum PublishError {
    /// The uuid is currently claimed by a different account.
    Claimed,
}

#[derive(Debug, PartialEq, Eq)]
pub struct Seen {
    pub mc: Uuid,
    pub gui: Option<String>,
    pub age_ms: u64,
}

struct Entry {
    gui: Option<String>,
    updated: Instant,
}

struct Claim {
    account: Uuid,
    seen: Instant,
}

#[derive(Default)]
struct Inner {
    entries: HashMap<(String, Uuid), Entry>,
    claims: HashMap<Uuid, Claim>,
    by_account: HashMap<Uuid, Uuid>,
}

/// In-memory presence state. A uuid is bound to one account at a time so a
/// client cannot impersonate somebody else's menu; an account owns one uuid at
/// a time (switching alts releases the previous claim).
#[derive(Default)]
pub struct Store {
    inner: Mutex<Inner>,
}

impl Store {
    pub fn publish(
        &self,
        account: Uuid,
        server: &str,
        mc: Uuid,
        gui: Option<String>,
        now: Instant,
    ) -> Result<(), PublishError> {
        let mut inner = self.inner.lock().expect("presence lock");
        if let Some(claim) = inner.claims.get(&mc)
            && claim.account != account
            && now.duration_since(claim.seen) < CLAIM_TTL
        {
            return Err(PublishError::Claimed);
        }
        if let Some(previous) = inner.by_account.insert(account, mc)
            && previous != mc
        {
            inner.claims.remove(&previous);
            inner.entries.retain(|(_, id), _| *id != previous);
        }
        inner.claims.insert(mc, Claim { account, seen: now });
        inner
            .entries
            .insert((server.to_owned(), mc), Entry { gui, updated: now });
        Ok(())
    }

    /// Fresh entries for the requested uuids on this server (bounded).
    pub fn lookup(&self, server: &str, want: &[Uuid], now: Instant) -> Vec<Seen> {
        let inner = self.inner.lock().expect("presence lock");
        want.iter()
            .take(MAX_WANT)
            .filter_map(|mc| {
                let entry = inner.entries.get(&(server.to_owned(), *mc))?;
                let age = now.duration_since(entry.updated);
                (age < ENTRY_TTL).then(|| Seen {
                    mc: *mc,
                    gui: entry.gui.clone(),
                    age_ms: age.as_millis() as u64,
                })
            })
            .collect()
    }

    pub fn purge(&self, now: Instant) {
        let mut inner = self.inner.lock().expect("presence lock");
        inner
            .entries
            .retain(|_, e| now.duration_since(e.updated) < ENTRY_TTL);
        let expired: Vec<Uuid> = inner
            .claims
            .iter()
            .filter(|(_, c)| now.duration_since(c.seen) >= CLAIM_TTL)
            .map(|(mc, _)| *mc)
            .collect();
        for mc in expired {
            if let Some(claim) = inner.claims.remove(&mc) {
                inner.by_account.remove(&claim.account);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn ids() -> (Uuid, Uuid, Uuid, Uuid) {
        (
            Uuid::from_u128(1),
            Uuid::from_u128(2),
            Uuid::from_u128(10),
            Uuid::from_u128(20),
        )
    }

    #[test]
    fn published_entry_is_visible_then_expires() {
        let (acc, _, mc, _) = ids();
        let store = Store::default();
        let t0 = Instant::now();
        store
            .publish(acc, "srv", mc, Some("AAAA".into()), t0)
            .unwrap();
        let seen = store.lookup("srv", &[mc], t0 + Duration::from_millis(500));
        assert_eq!(seen.len(), 1);
        assert_eq!(seen[0].gui.as_deref(), Some("AAAA"));
        assert_eq!(seen[0].age_ms, 500);
        assert!(store.lookup("srv", &[mc], t0 + ENTRY_TTL).is_empty());
    }

    #[test]
    fn entries_are_scoped_to_the_server() {
        let (acc, _, mc, _) = ids();
        let store = Store::default();
        let t0 = Instant::now();
        store.publish(acc, "a", mc, None, t0).unwrap();
        assert!(store.lookup("b", &[mc], t0).is_empty());
        assert_eq!(store.lookup("a", &[mc], t0).len(), 1);
    }

    #[test]
    fn other_account_cannot_take_a_claimed_uuid() {
        let (acc, other, mc, _) = ids();
        let store = Store::default();
        let t0 = Instant::now();
        store.publish(acc, "s", mc, None, t0).unwrap();
        assert_eq!(
            store.publish(other, "s", mc, None, t0 + Duration::from_secs(1)),
            Err(PublishError::Claimed)
        );
        // after the claim goes idle it can be taken over
        assert!(store.publish(other, "s", mc, None, t0 + CLAIM_TTL).is_ok());
    }

    #[test]
    fn switching_uuid_releases_the_previous_claim() {
        let (acc, other, mc1, mc2) = ids();
        let store = Store::default();
        let t0 = Instant::now();
        store.publish(acc, "s", mc1, None, t0).unwrap();
        store.publish(acc, "s", mc2, None, t0).unwrap();
        assert!(store.lookup("s", &[mc1], t0).is_empty());
        assert!(store.publish(other, "s", mc1, None, t0).is_ok());
    }

    #[test]
    fn lookup_is_bounded() {
        let (acc, _, mc, _) = ids();
        let store = Store::default();
        let t0 = Instant::now();
        store.publish(acc, "s", mc, None, t0).unwrap();
        let mut want: Vec<Uuid> = (100..100 + MAX_WANT as u128).map(Uuid::from_u128).collect();
        want.push(mc);
        assert!(store.lookup("s", &want, t0).is_empty());
    }

    #[test]
    fn purge_drops_stale_state() {
        let (acc, _, mc, _) = ids();
        let store = Store::default();
        let t0 = Instant::now();
        store.publish(acc, "s", mc, None, t0).unwrap();
        store.purge(t0 + CLAIM_TTL);
        assert!(
            store
                .publish(Uuid::from_u128(2), "s", mc, None, t0 + CLAIM_TTL)
                .is_ok()
        );
    }
}
