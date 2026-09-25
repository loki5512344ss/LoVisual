use common::internal::GrpcKeyAttach;
use common::pb::accounts::{
    AuthenticateDeviceRequest, accounts_internal_client::AccountsInternalClient,
};
use std::{future::Future, pin::Pin};
use tonic::{
    Code,
    service::interceptor::InterceptedService,
    transport::{Channel, Endpoint},
};
use uuid::Uuid;

pub enum DeviceAuth {
    Valid(Uuid),
    Invalid,
    /// accounts-service unreachable — the gateway answers 503, not 401,
    /// so the mod doesn't wrongly forget its token.
    Unavailable,
}

pub type DeviceAuthFuture<'a> = Pin<Box<dyn Future<Output = DeviceAuth> + Send + 'a>>;

pub trait DeviceAuthenticator: Send + Sync + 'static {
    fn authenticate<'a>(&'a self, token: &'a str) -> DeviceAuthFuture<'a>;
}

pub struct GrpcDevices {
    client: AccountsInternalClient<InterceptedService<Channel, GrpcKeyAttach>>,
}

impl GrpcDevices {
    /// Lazy: the gateway boots even if accounts-service is still starting.
    pub fn connect_lazy(url: &str, internal_key: &str) -> anyhow::Result<Self> {
        let channel = Endpoint::from_shared(url.to_owned())?
            .timeout(std::time::Duration::from_secs(5))
            .connect_lazy();
        Ok(GrpcDevices {
            client: AccountsInternalClient::with_interceptor(
                channel,
                GrpcKeyAttach::new(internal_key)?,
            ),
        })
    }
}

impl DeviceAuthenticator for GrpcDevices {
    fn authenticate<'a>(&'a self, token: &'a str) -> DeviceAuthFuture<'a> {
        Box::pin(async move {
            let mut client = self.client.clone();
            match client
                .authenticate_device(AuthenticateDeviceRequest {
                    device_token: token.to_owned(),
                })
                .await
            {
                Ok(reply) => Uuid::parse_str(&reply.into_inner().account_id)
                    .map_or(DeviceAuth::Invalid, DeviceAuth::Valid),
                Err(status) if status.code() == Code::Unauthenticated => DeviceAuth::Invalid,
                Err(status) => {
                    tracing::warn!("AuthenticateDevice failed: {status}");
                    DeviceAuth::Unavailable
                }
            }
        })
    }
}
