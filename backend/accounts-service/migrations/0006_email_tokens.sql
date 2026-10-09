-- Email verification + shared single-use tokens.
--
-- One table for both token purposes (email_verify / password_reset): the
-- semantics are identical (hashed opaque token, short TTL, single use,
-- issuing a new one supersedes the pending one), so 0005's dedicated
-- password_reset_tokens table is folded into it. Pending reset links (30
-- minute TTL) are carried over instead of silently invalidated.
--
-- Existing accounts are marked verified at migration time: they signed up
-- before verification existed, and locking them out of device linking would
-- break every linked mod on deploy.

ALTER TABLE accounts ADD COLUMN email_verified_at TIMESTAMPTZ;

UPDATE accounts SET email_verified_at = now();

CREATE TABLE email_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    purpose TEXT NOT NULL CHECK (purpose IN ('email_verify', 'password_reset')),
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_tokens_account_id ON email_tokens(account_id);

INSERT INTO email_tokens (account_id, purpose, token_hash, expires_at, used_at, created_at)
SELECT account_id, 'password_reset', token_hash, expires_at, used_at, created_at
FROM password_reset_tokens;

DROP TABLE password_reset_tokens;
