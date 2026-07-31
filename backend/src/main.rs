//! LoVisual backend server entry point.

mod routes;
mod server;

use std::net::SocketAddr;

use anyhow::Context as _;

use server::config::Config;
use server::serve;

fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "lovisual_backend=info,tower_http=info".into()),
        )
        .init();

    let config = Config::from_env().context("failed to load config")?;
    let addr: SocketAddr = config.listen_addr();
    tracing::info!(%addr, "starting lovisual backend");

    tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .context("failed to build tokio runtime")?
        .block_on(serve(config))
}
