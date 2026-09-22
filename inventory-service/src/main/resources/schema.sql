CREATE TABLE IF NOT EXISTS products (
    product_id  UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    stock       INT NOT NULL,
    updated_at  TIMESTAMP
);
