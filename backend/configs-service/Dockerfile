# Build context must be backend/ (the workspace root).
FROM rust:1-trixie AS builder
RUN apt-get update && apt-get install -y --no-install-recommends \
    protobuf-compiler pkg-config libssl-dev ca-certificates \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY Cargo.toml Cargo.lock ./
COPY common ./common
COPY accounts-service ./accounts-service
COPY gateway ./gateway
COPY configs-service ./configs-service
RUN cargo build --release -p configs-service

FROM debian:trixie-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates libssl3 \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /build/target/release/configs-service /usr/local/bin/configs-service
ENTRYPOINT ["/usr/local/bin/configs-service"]
