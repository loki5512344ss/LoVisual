# One Dockerfile for every backend service. Build context must be backend/
# (the workspace root). Pick a service with `target:` in docker-compose.
#
# Why one file: all four services share one `builder` stage, so the dependency
# tree (aws-sdk, sqlx, tonic, ...) is compiled once per deploy instead of once
# per service. The BuildKit cache mounts keep the cargo registry and target/
# between deploys, so a source change recompiles only the crates that changed.

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
COPY chat-service ./chat-service
# target/ lives in a cache mount, so the binaries are copied out in the same RUN.
RUN --mount=type=cache,target=/usr/local/cargo/registry,sharing=locked \
    --mount=type=cache,target=/usr/local/cargo/git,sharing=locked \
    --mount=type=cache,target=/build/target \
    cargo build --release --locked \
        -p accounts-service -p gateway -p configs-service -p chat-service \
    && mkdir /out \
    && cp target/release/accounts-service target/release/gateway \
          target/release/configs-service target/release/chat-service /out/

FROM debian:trixie-slim AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates libssl3 \
    && rm -rf /var/lib/apt/lists/*

FROM runtime AS accounts-service
COPY --from=builder /out/accounts-service /usr/local/bin/accounts-service
ENTRYPOINT ["/usr/local/bin/accounts-service"]

FROM runtime AS gateway
COPY --from=builder /out/gateway /usr/local/bin/gateway
ENTRYPOINT ["/usr/local/bin/gateway"]

FROM runtime AS configs-service
COPY --from=builder /out/configs-service /usr/local/bin/configs-service
ENTRYPOINT ["/usr/local/bin/configs-service"]

FROM runtime AS chat-service
COPY --from=builder /out/chat-service /usr/local/bin/chat-service
ENTRYPOINT ["/usr/local/bin/chat-service"]
