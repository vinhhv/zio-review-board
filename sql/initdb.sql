CREATE DATABASE reviewboard;
\c reviewboard;

-- USERS
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  hashed_password TEXT NOT NULL
);

INSERT INTO users(email, hashed_password) VALUES('vince@misterjvm.com', '1000:522A8F8FEF27AC8CDE10F23F4BDC76634594E3B24F17F743:119E61377766DFD35F63D48AFF3E4EEB3CDB1B503BFB137F');

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

INSERT INTO trainers(name, description, url, image) VALUES('Paul J. Fabritz', 'Popular NBA trainer', 'https://pjfperformance.net', NULL);
INSERT INTO trainers(name, description, url, image) VALUES('Tyler Relph', 'Austin, TX NBA trainer', 'https://www.tylerrelphtraining.com/', NULL);

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

INSERT INTO programs(slug, name, url, trainer_id, trainer_name, payment_type, image, tags) VALUES('pjf-performance-unranked-academy', 'Unranked Academy', 'https://www.pjfperformance.com/unranked-basketball-academy/', 1, 'Paul J. Fabritz', 'Subscription', NULL, '{Strength,Shooting,Ball Handling,Agility}');
INSERT INTO programs(slug, name, url, trainer_id, trainer_name, payment_type, image, tags) VALUES('pjf-performance-the-vert-code', 'The Vert Code', 'https://www.pjfperformance.com/the-vert-code/', 1, 'Paul J. Fabritz', 'LifetimeAccess', NULL, '{Vertical}');
INSERT INTO programs(slug, name, url, trainer_id, trainer_name, payment_type, image, tags) VALUES('tyler-relph-basketball-academy', 'Tyler Relph Basketball Academy', 'https://www.trbasketballacademy.com/', 2, 'Tyler Relph', 'Subscription', NULL, '{Shooting, Ball Handling}');

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

INSERT INTO reviews(program_id, program_slug, user_id, value, quality, content, user_experience, accessibility, support, would_recommend, review) VALUES(1, 'pjf-performance-unranked-academy', 1, 'Amazing', 'Amazing', 'Amazing', 'Amazing', 'Amazing', 'Amazing', 'Amazing', 'Absolutely the best program out there for all hoopers!');

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

INSERT INTO invites(username, program_id, n_invites, active) VALUES ('vinh@misterjvm.com', 1, 10, true);

CREATE TABLE IF NOT EXISTS review_summaries (
  program_id BIGINT NOT NULL PRIMARY KEY,
  contents TEXT,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
  