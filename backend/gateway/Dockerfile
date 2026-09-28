# Build context must be backend/ (the workspace root).
FROM rust:1-slim AS builder
RUN apt-get update && apt-get install -y --no-install-recommends \
    protobuf-compiler pkg-config libssl-dev ca-certificates \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY Cargo.toml Cargo.lock ./
COPY common ./common
COPY accounts-service ./accounts-service
COPY gateway ./gateway
COPY configs-service ./configs-service
RUN cargo build --release -p gateway

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates libssl3 \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /build/target/release/gateway /usr/local/bin/gateway
ENTRYPOINT ["/usr/local/bin/gateway"]
