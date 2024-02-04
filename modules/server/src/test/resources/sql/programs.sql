CREATE TYPE program_type AS ENUM('LifetimeAccess', 'Subscription', 'SubscriptionOrLifetimeAccess');

CREATE TABLE IF NOT EXISTS programs (
  id BIGSERIAL PRIMARY KEY,
  slug TEXT UNIQUE NOT NULL,
  name TEXT UNIQUE NOT NULL,
  url TEXT UNIQUE NOT NULL,
  trainer TEXT NOT NULL,
  program_type program_type NOT NULL,
  image TEXT,
  tags TEXT[]
);
