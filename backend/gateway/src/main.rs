use gateway::identity::device::GrpcDevices;
use std::sync::Arc;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    dotenvy::dotenv().ok();
    tracing_subscriber::fmt::init();
    let cfg = gateway::config::Config::from_env()?;
    cfg.validate()?;
    let devices = Arc::new(GrpcDevices::connect_lazy(
        &cfg.accounts_grpc_url,
        &cfg.internal_key,
    )?);
    let app = gateway::build_app(&cfg, devices);
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", cfg.port)).await?;
    tracing::info!("gateway listening on {}", cfg.port);
    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<std::net::SocketAddr>(),
    )
    .await?;
    Ok(())
}
