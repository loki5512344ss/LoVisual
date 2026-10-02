//! GUI presence: lets LoVisual users see which client menu nearby LoVisual
//! users have open. State is in-memory and short-lived (a few seconds), never
//! persisted; see `backend/chat-service/PLAN.md`.

pub mod handlers;
pub mod payload;
pub mod store;

use std::{sync::Arc, time::{Duration, Instant}};
use store::Store;

pub fn spawn_purger(store: Arc<Store>) {
    tokio::spawn(async move {
        let mut tick = tokio::time::interval(Duration::from_secs(30));
        loop {
            tick.tick().await;
            store.purge(Instant::now());
        }
    });
}
