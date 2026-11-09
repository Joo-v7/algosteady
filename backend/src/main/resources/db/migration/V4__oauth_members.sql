CREATE TABLE member_account (
    id UUID PRIMARY KEY
  , provider VARCHAR(255) NOT NULL
  , provider_subject VARCHAR(255) NOT NULL
  , email VARCHAR(255)
  , name VARCHAR(255)
  , created_at TIMESTAMP WITH TIME ZONE NOT NULL
  , last_login_at TIMESTAMP WITH TIME ZONE NOT NULL
  , UNIQUE (provider, provider_subject)
);
