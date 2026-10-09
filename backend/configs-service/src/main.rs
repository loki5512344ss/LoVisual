#[tokio::main]
async fn main() -> anyhow::Result<()> {
    dotenvy::dotenv().ok();
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info")),
        )
        .init();
    let cfg = configs_service::config::Config::from_env()?;
    cfg.validate()?;
    let pool = sqlx::PgPool::connect(&cfg.database_url).await?;
    sqlx::migrate!("./migrations").run(&pool).await?;
    let profiles = std::sync::Arc::new(
        configs_service::showcase::profiles::GrpcProfiles::connect_lazy(
            &cfg.accounts_grpc_url,
            &cfg.internal_key,
        )?,
    );
    let app = configs_service::build_app(pool, &cfg, profiles);
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", cfg.port)).await?;
    tracing::info!("configs-service listening on {}", cfg.port);
    axum::serve(listener, app).await?;
    Ok(())
}
