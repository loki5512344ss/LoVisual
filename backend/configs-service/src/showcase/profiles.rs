use common::internal::GrpcKeyAttach;
use common::pb::accounts::{
    GetPublicProfilesRequest, accounts_internal_client::AccountsInternalClient,
};
use serde::Serialize;
use std::{collections::HashMap, future::Future, pin::Pin};
use tonic::{
    service::interceptor::InterceptedService,
    transport::{Channel, Endpoint},
};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize)]
pub struct Author {
    pub nick: String,
    pub avatar_url: Option<String>,
}

pub type ProfilesFuture<'a> = Pin<Box<dyn Future<Output = HashMap<Uuid, Author>> + Send + 'a>>;

pub trait ProfileSource: Send + Sync + 'static {
    fn profiles<'a>(&'a self, ids: &'a [Uuid]) -> ProfilesFuture<'a>;
}

pub struct GrpcProfiles {
    client: AccountsInternalClient<InterceptedService<Channel, GrpcKeyAttach>>,
}

impl GrpcProfiles {
    pub fn connect_lazy(url: &str, internal_key: &str) -> anyhow::Result<Self> {
        let channel = Endpoint::from_shared(url.to_owned())?
            .timeout(std::time::Duration::from_secs(3))
            .connect_lazy();
        Ok(GrpcProfiles {
            client: AccountsInternalClient::with_interceptor(
                channel,
                GrpcKeyAttach::new(internal_key)?,
            ),
        })
    }
}

impl ProfileSource for GrpcProfiles {
    fn profiles<'a>(&'a self, ids: &'a [Uuid]) -> ProfilesFuture<'a> {
        Box::pin(async move {
            let request = GetPublicProfilesRequest {
                account_ids: ids.iter().map(Uuid::to_string).collect(),
            };
            match self.client.clone().get_public_profiles(request).await {
                Ok(reply) => reply
                    .into_inner()
                    .profiles
                    .into_iter()
                    .filter_map(|p| {
                        let id = Uuid::parse_str(&p.account_id).ok()?;
                        let avatar_url = (!p.avatar_url.is_empty()).then_some(p.avatar_url);
                        Some((
                            id,
                            Author {
                                nick: p.display_nick,
                                avatar_url,
                            },
                        ))
                    })
                    .collect(),
                Err(status) => {
                    // Showcase must stay browsable when accounts-service is down.
                    tracing::warn!("GetPublicProfiles failed: {status}");
                    HashMap::new()
                }
            }
        })
    }
}
