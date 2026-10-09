#[tokio::main]
async fn main() -> anyhow::Result<()> {
    dotenvy::dotenv().ok();
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info")),
        )
        .init();
    let cfg = chat_service::config::Config::from_env()?;
    cfg.validate()?;
    let (app, state) = chat_service::build_app(&cfg);
    chat_service::presence::spawn_purger(state);
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", cfg.port)).await?;
    tracing::info!("chat-service listening on {}", cfg.port);
    axum::serve(listener, app).await?;
    Ok(())
}
