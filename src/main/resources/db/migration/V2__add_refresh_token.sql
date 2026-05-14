ALTER TABLE user_sessions
  ADD COLUMN refresh_token VARCHAR(36) NOT NULL DEFAULT '' AFTER token,
  ADD COLUMN refresh_expires_at DATETIME NOT NULL DEFAULT NOW() AFTER expires_at;

ALTER TABLE user_sessions
  ADD UNIQUE INDEX uq_refresh_token (refresh_token);