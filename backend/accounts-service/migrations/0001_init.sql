CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Hardening: revoke implicit PUBLIC access to this schema. This Postgres
-- instance hosts multiple services' databases (accounts_db/configs_db/
-- chat_db per TODO.md Фаза 10) on one cluster; the app role connects as
-- its own owner and needs no PUBLIC grant to function.
REVOKE ALL ON SCHEMA public FROM PUBLIC;

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- No plain UNIQUE on email: Postgres TEXT comparison is case-sensitive,
    -- so "User@x.com" and "user@x.com" would be treated as different rows,
    -- letting one person hold two accounts under what looks like the same
    -- email (account-confusion / signup-limit-bypass class of bug). A
    -- unique index on lower(email) enforces uniqueness case-insensitively;
    -- application code (accounts::repo) must look up/insert by
    -- `email.to_lowercase()` for this index to actually get hit.
    email TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    display_nick TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user',
    can_publish_addons BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX accounts_email_lower_idx ON accounts (lower(email));

CREATE TABLE avatars (
    account_id UUID PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    s3_key TEXT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE device_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    device_token_hash TEXT NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen TIMESTAMPTZ
);

CREATE INDEX idx_device_links_account_id ON device_links(account_id);
