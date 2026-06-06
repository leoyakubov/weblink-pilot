CREATE TABLE app_social_identities (
  id BIGSERIAL PRIMARY KEY,
  provider VARCHAR(32) NOT NULL,
  provider_user_id VARCHAR(128) NOT NULL,
  provider_login VARCHAR(128),
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  last_login_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_social_identity_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
  CONSTRAINT uk_social_identity_provider_user UNIQUE (provider, provider_user_id),
  CONSTRAINT uk_social_identity_provider_account UNIQUE (provider, user_id)
);

CREATE INDEX idx_social_identity_user_id ON app_social_identities (user_id);
