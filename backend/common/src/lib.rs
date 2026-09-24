pub mod internal;
pub mod jwt;

pub mod pb {
    pub mod accounts {
        tonic::include_proto!("accounts.v1");
    }
}
