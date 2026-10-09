use accounts_service::{build_app, config::Config};

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info")),
        )
        .init();
    dotenvy::dotenv().ok();
    let cfg = Config::from_env()?;
    cfg.validate()?;

    let pool = sqlx::postgres::PgPoolOptions::new()
        .max_connections(10)
        .connect(&cfg.database_url)
        .await?;

    sqlx::migrate!("./migrations").run(&pool).await?;

    let http = tokio::net::TcpListener::bind(("0.0.0.0", cfg.port)).await?;
    let grpc_addr = std::net::SocketAddr::from(([0, 0, 0, 0], cfg.grpc_port));
    tracing::info!(
        "accounts-service listening on {} (http) and {} (grpc)",
        cfg.port,
        grpc_addr
    );

    let grpc = tonic::transport::Server::builder()
        .add_service(accounts_service::grpc::server(
            pool.clone(),
            cfg.avatar_base_url(),
            &cfg.internal_key,
        ))
        .serve(grpc_addr);
    let app = build_app(pool, &cfg);

    tokio::try_join!(
        async { axum::serve(http, app).await.map_err(anyhow::Error::from) },
        async { grpc.await.map_err(anyhow::Error::from) },
    )?;
    Ok(())
}
