CREATE TYPE metric_score AS ENUM('Poor', 'Fair', 'Good', 'Great', 'Amazing');

CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    program_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    value metric_score NOT NULL,
    quality metric_score NOT NULL,
    content metric_score NOT NULL,
    user_experience metric_score NOT NULL,
    accessibility metric_score NOT NULL,
    support metric_score NOT NULL,
    would_recommend metric_score NOT NULL,
    review TEXT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT now(),
    updated TIMESTAMP NOT NULL DEFAULT now()
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