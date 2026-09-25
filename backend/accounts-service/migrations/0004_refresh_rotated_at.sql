-- Tracks when a refresh token was revoked specifically *by rotation* (as
-- opposed to logout or reuse-detection), so a short grace window can
-- tolerate two concurrent refreshes of the same token (e.g. two tabs)
-- without treating the second one as token theft.
ALTER TABLE refresh_tokens ADD COLUMN rotated_at TIMESTAMPTZ;

-- Used by rotate_refresh to decide whether a just-rotated token is still
-- within the grace window.
CREATE INDEX idx_refresh_tokens_rotated_at ON refresh_tokens(rotated_at)
    WHERE rotated_at IS NOT NULL;

-- Used by the accounts::repo "early" badge (count of accounts created
-- before a given account's created_at).
CREATE INDEX IF NOT EXISTS idx_accounts_created_at ON accounts(created_at);
