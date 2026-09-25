CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- account_id has no FK: accounts live in accounts_db (separate database).
CREATE TABLE config_slots (
    account_id UUID NOT NULL,
    slot_index SMALLINT NOT NULL CHECK (slot_index BETWEEN 1 AND 4),
    name TEXT NOT NULL,
    data JSONB NOT NULL,
    share_code TEXT NOT NULL UNIQUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, slot_index)
);

CREATE TABLE showcase_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    slot_index SMALLINT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    copies_count INTEGER NOT NULL DEFAULT 0,
    -- set by admin moderation (Подсистема 3); public queries always filter it
    hidden BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (account_id, slot_index),
    FOREIGN KEY (account_id, slot_index)
        REFERENCES config_slots (account_id, slot_index) ON DELETE CASCADE
);

CREATE INDEX idx_showcase_new ON showcase_listings (published_at DESC) WHERE NOT hidden;
CREATE INDEX idx_showcase_popular ON showcase_listings (copies_count DESC, published_at DESC) WHERE NOT hidden;
