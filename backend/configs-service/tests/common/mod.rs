#![allow(dead_code)]

use axum_test::TestServer;
use configs_service::config::Config;
use uuid::Uuid;

#[allow(unused_imports)] // used by slots/showcase tests from Task 2 on
pub use ::common::internal::ACCOUNT_ID_HEADER;

pub const TEST_INTERNAL_KEY: &str = "internal-key-internal-key-internal!!";

pub async fn test_pool() -> sqlx::PgPool {
    let url = std::env::var("CONFIGS_DATABASE_URL")
        .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/configs_db".into());
    let pool = sqlx::PgPool::connect(&url)
        .await
        .expect("connect to configs_db");
    sqlx::migrate!("./migrations")
        .run(&pool)
        .await
        .expect("run migrations");
    pool
}

pub fn test_config() -> Config {
    Config {
        database_url: String::new(),
        port: 0,
        internal_key: TEST_INTERNAL_KEY.into(),
        accounts_grpc_url: String::new(),
    }
}

pub fn test_server(app: axum::Router) -> TestServer {
    let mut server = TestServer::new(app);
    server.add_header(::common::internal::INTERNAL_KEY_HEADER, TEST_INTERNAL_KEY);
    server
}

/// Accounts live in another service; here an account is just a fresh UUID.
pub fn new_account() -> Uuid {
    Uuid::new_v4()
}

use configs_service::showcase::profiles::{Author, ProfileSource, ProfilesFuture};
use std::collections::HashMap;
use std::sync::Arc;

/// Every account is "Author-<first 4 chars of id>".
pub struct FakeProfiles;

impl ProfileSource for FakeProfiles {
    fn profiles<'a>(&'a self, ids: &'a [Uuid]) -> ProfilesFuture<'a> {
        Box::pin(async move {
            ids.iter()
                .map(|id| {
                    (
                        *id,
                        Author {
                            nick: format!("Author-{}", &id.to_string()[..4]),
                            avatar_url: None,
                        },
                    )
                })
                .collect::<HashMap<_, _>>()
        })
    }
}

pub fn no_profiles() -> Arc<dyn ProfileSource> {
    Arc::new(FakeProfiles)
}
