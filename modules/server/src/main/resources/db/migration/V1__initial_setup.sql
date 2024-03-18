-- USERS
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  hashed_password TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS recovery_tokens (
  email TEXT PRIMARY KEY,
  token TEXT NOT NULL,
  expiration BIGINT NOT NULL
);

-- TRAINERS

CREATE TABLE IF NOT EXISTS trainers (
  id BIGSERIAL PRIMARY KEY,
  name TEXT UNIQUE NOT NULL,
  description TEXT NOT NULL,
  url TEXT UNIQUE NOT NULL,
  image TEXT
);

-- PROGRAMS

CREATE TYPE payment_type AS ENUM('LifetimeAccess', 'Subscription', 'SubscriptionOrLifetimeAccess');

CREATE TABLE IF NOT EXISTS programs (
  id BIGSERIAL PRIMARY KEY,
  slug TEXT UNIQUE NOT NULL,
  name TEXT UNIQUE NOT NULL,
  url TEXT UNIQUE NOT NULL,
  trainer_id BIGSERIAL NOT NULL REFERENCES trainers(id),
  trainer_name TEXT NOT NULL,
  payment_type payment_type NOT NULL,
  image TEXT,
  tags TEXT[]
);

-- REVIEWS

CREATE TYPE metric_score AS ENUM('Poor', 'Fair', 'Good', 'Great', 'Amazing');

CREATE TABLE IF NOT EXISTS reviews (
  id BIGSERIAL PRIMARY KEY,
  program_id BIGSERIAL NOT NULL REFERENCES programs(id),
  program_slug TEXT NOT NULL REFERENCES programs(slug),
  user_id BIGINT NOT NULL REFERENCES users(id),
  value metric_score NOT NULL,
  quality metric_score NOT NULL,
  content metric_score NOT NULL,
  user_experience metric_score NOT NULL,
  accessibility metric_score NOT NULL,
  support metric_score NOT NULL,
  would_recommend metric_score NOT NULL,
  review TEXT NOT NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated = now(); 
    RETURN NEW; 
END
$$ language 'plpgsql';

CREATE TRIGGER update_updated_column_before_update
BEFORE UPDATE ON reviews
FOR EACH ROW
EXECUTE FUNCTION update_modified_column();

CREATE TABLE IF NOT EXISTS invites (
  id BIGSERIAL PRIMARY KEY,
  username TEXT NOT NULL,
  program_id BIGINT NOT NULL REFERENCES programs(id),
  n_invites INT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS review_summaries (
  program_id BIGINT NOT NULL PRIMARY KEY,
  contents TEXT,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
  