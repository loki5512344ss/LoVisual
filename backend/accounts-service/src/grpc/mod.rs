use common::internal::GrpcKeyCheck;
use common::pb::accounts::accounts_internal_server::{AccountsInternal, AccountsInternalServer};
use common::pb::accounts::{
    AuthenticateDeviceReply, AuthenticateDeviceRequest, GetPublicProfilesReply,
    GetPublicProfilesRequest, PublicProfile,
};
use sqlx::PgPool;
use tonic::{Request, Response, Status, service::interceptor::InterceptedService};
use uuid::Uuid;

const MAX_PROFILE_IDS: usize = 100;

pub struct AccountsGrpc {
    pool: PgPool,
    avatar_base_url: String,
}

impl AccountsGrpc {
    pub fn new(pool: PgPool, avatar_base_url: String) -> Self {
        AccountsGrpc {
            pool,
            avatar_base_url,
        }
    }
}

/// Builds the internal `AccountsInternal` gRPC service, guarded by
/// `GrpcKeyCheck` so only callers holding the shared internal key (i.e. the
/// gateway) can reach it.
pub fn server(
    pool: PgPool,
    avatar_base_url: String,
    internal_key: &str,
) -> InterceptedService<AccountsInternalServer<AccountsGrpc>, GrpcKeyCheck> {
    AccountsInternalServer::with_interceptor(
        AccountsGrpc::new(pool, avatar_base_url),
        GrpcKeyCheck::new(internal_key),
    )
}

#[tonic::async_trait]
impl AccountsInternal for AccountsGrpc {
    async fn authenticate_device(
        &self,
        request: Request<AuthenticateDeviceRequest>,
    ) -> Result<Response<AuthenticateDeviceReply>, Status> {
        let token = request.into_inner().device_token;
        match crate::device::links::authenticate(&self.pool, &token).await {
            Ok(Some((account_id, email_verified))) => Ok(Response::new(AuthenticateDeviceReply {
                account_id: account_id.to_string(),
                email_verified,
            })),
            Ok(None) => Err(Status::unauthenticated("unknown or revoked device token")),
            Err(err) => {
                tracing::error!("authenticate_device: {err:?}");
                Err(Status::internal("internal error"))
            }
        }
    }

    async fn get_public_profiles(
        &self,
        request: Request<GetPublicProfilesRequest>,
    ) -> Result<Response<GetPublicProfilesReply>, Status> {
        let raw = request.into_inner().account_ids;
        if raw.len() > MAX_PROFILE_IDS {
            return Err(Status::invalid_argument("at most 100 account ids per call"));
        }
        let ids: Vec<Uuid> = raw.iter().filter_map(|s| Uuid::parse_str(s).ok()).collect();
        let rows = crate::accounts::repo::public_profiles(&self.pool, &ids)
            .await
            .map_err(|err| {
                tracing::error!("get_public_profiles: {err:?}");
                Status::internal("internal error")
            })?;
        let profiles = rows
            .into_iter()
            .map(|(id, nick, key, version)| PublicProfile {
                account_id: id.to_string(),
                display_nick: nick,
                avatar_url: match (key, version) {
                    (Some(k), Some(v)) => format!("{}/{k}?v={v}", self.avatar_base_url),
                    _ => String::new(),
                },
            })
            .collect();
        Ok(Response::new(GetPublicProfilesReply { profiles }))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use common::internal::GrpcKeyAttach;
    use common::pb::accounts::accounts_internal_client::AccountsInternalClient;

    const KEY: &str = "internal-key-internal-key-internal!!";
    const CDN: &str = "http://cdn/avatars";

    async fn pool() -> PgPool {
        let url = std::env::var("DATABASE_URL")
            .unwrap_or_else(|_| "postgres://lovisual:lovisual@localhost:5432/accounts_db".into());
        let pool = PgPool::connect(&url).await.expect("connect");
        sqlx::migrate!("./migrations")
            .run(&pool)
            .await
            .expect("migrate");
        pool
    }

    #[tokio::test]
    async fn authenticate_device_over_grpc() {
        let pool = pool().await;
        let account = crate::accounts::repo::create(
            &pool,
            &format!("g-{}@example.com", Uuid::new_v4()),
            "x",
            "Grpc",
        )
        .await
        .unwrap();
        let token = crate::device::links::create(&pool, account.id)
            .await
            .unwrap();

        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let addr = listener.local_addr().unwrap();
        tokio::spawn(
            tonic::transport::Server::builder()
                .add_service(server(pool.clone(), CDN.into(), KEY))
                .serve_with_incoming(tokio_stream::wrappers::TcpListenerStream::new(listener)),
        );
        let channel = tonic::transport::Endpoint::from_shared(format!("http://{addr}"))
            .unwrap()
            .connect_lazy();
        let mut client = AccountsInternalClient::with_interceptor(
            channel.clone(),
            GrpcKeyAttach::new(KEY).unwrap(),
        );

        let reply = client
            .authenticate_device(AuthenticateDeviceRequest {
                device_token: token,
            })
            .await
            .unwrap()
            .into_inner();
        assert_eq!(reply.account_id, account.id.to_string());

        let err = client
            .authenticate_device(AuthenticateDeviceRequest {
                device_token: "lvd_unknown".into(),
            })
            .await
            .unwrap_err();
        assert_eq!(err.code(), tonic::Code::Unauthenticated);

        let mut no_key = AccountsInternalClient::new(channel);
        let err = no_key
            .authenticate_device(AuthenticateDeviceRequest {
                device_token: "x".into(),
            })
            .await
            .unwrap_err();
        assert_eq!(err.code(), tonic::Code::PermissionDenied);

        sqlx::query("DELETE FROM accounts WHERE id = $1")
            .bind(account.id)
            .execute(&pool)
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn public_profiles_over_grpc() {
        let pool = pool().await;
        let a = crate::accounts::repo::create(
            &pool,
            &format!("p-{}@example.com", Uuid::new_v4()),
            "x",
            "Alice",
        )
        .await
        .unwrap();
        let svc = AccountsGrpc::new(pool.clone(), CDN.into());

        let reply = svc
            .get_public_profiles(tonic::Request::new(GetPublicProfilesRequest {
                account_ids: vec![
                    a.id.to_string(),
                    Uuid::new_v4().to_string(),
                    "garbage".into(),
                ],
            }))
            .await
            .unwrap()
            .into_inner();
        assert_eq!(reply.profiles.len(), 1);
        assert_eq!(reply.profiles[0].display_nick, "Alice");
        assert_eq!(reply.profiles[0].avatar_url, "");

        let too_many: Vec<String> = (0..101).map(|_| Uuid::new_v4().to_string()).collect();
        let err = svc
            .get_public_profiles(tonic::Request::new(GetPublicProfilesRequest {
                account_ids: too_many,
            }))
            .await
            .unwrap_err();
        assert_eq!(err.code(), tonic::Code::InvalidArgument);

        sqlx::query("DELETE FROM accounts WHERE id = $1")
            .bind(a.id)
            .execute(&pool)
            .await
            .unwrap();
    }
}
