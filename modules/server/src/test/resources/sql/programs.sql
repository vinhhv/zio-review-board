CREATE TYPE payment_type AS ENUM('LifetimeAccess', 'Subscription', 'SubscriptionOrLifetimeAccess');

CREATE TABLE IF NOT EXISTS programs (
  id BIGSERIAL PRIMARY KEY,
  slug TEXT UNIQUE NOT NULL,
  name TEXT UNIQUE NOT NULL,
  url TEXT UNIQUE NOT NULL,
  trainer_id BIGINT NOT NULL,
  payment_type payment_type NOT NULL,
  image TEXT,
  tags TEXT[]
);
