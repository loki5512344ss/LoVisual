use aws_sdk_s3::Client;
use aws_sdk_s3::primitives::ByteStream;

#[derive(Clone)]
pub struct S3Storage {
    client: Client,
    bucket: String,
}

impl S3Storage {
    /// Builds the S3 client synchronously (no I/O happens here — connections
    /// are made lazily on first request).
    pub fn from_config(endpoint: &str, access_key: &str, secret_key: &str, bucket: String) -> Self {
        let creds =
            aws_sdk_s3::config::Credentials::new(access_key, secret_key, None, None, "static");
        let config = aws_sdk_s3::config::Builder::new()
            .endpoint_url(endpoint)
            .credentials_provider(creds)
            .region(aws_sdk_s3::config::Region::new("us-east-1"))
            .force_path_style(true)
            .behavior_version(aws_sdk_s3::config::BehaviorVersion::latest())
            .build();
        S3Storage {
            client: Client::from_conf(config),
            bucket,
        }
    }

    pub async fn put(&self, key: &str, bytes: Vec<u8>, content_type: &str) -> anyhow::Result<()> {
        self.client
            .put_object()
            .bucket(&self.bucket)
            .key(key)
            .body(ByteStream::from(bytes))
            .content_type(content_type)
            .send()
            .await?;
        Ok(())
    }

    /// Reads a stored object back. Used to serve avatars through the service so
    /// MinIO itself never has to be exposed to browsers.
    pub async fn get(&self, key: &str) -> anyhow::Result<Vec<u8>> {
        let out = self
            .client
            .get_object()
            .bucket(&self.bucket)
            .key(key)
            .send()
            .await?;
        let aggregated = out.body.collect().await?;
        Ok(aggregated.into_bytes().to_vec())
    }

    /// Deletes the object at `key`. MinIO's DeleteObject is idempotent — deleting
    /// a missing key returns Ok. Callers on the app layer are expected to run
    /// this only after a matching DB row existed, so a stray 404 here is silent.
    pub async fn delete(&self, key: &str) -> anyhow::Result<()> {
        self.client
            .delete_object()
            .bucket(&self.bucket)
            .key(key)
            .send()
            .await?;
        Ok(())
    }
}
