-- Device tokens are looked up by hash on every mod request (via gateway gRPC).
CREATE UNIQUE INDEX device_links_token_hash_idx ON device_links (device_token_hash);
