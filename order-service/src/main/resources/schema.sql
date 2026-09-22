CREATE TABLE IF NOT EXISTS orders (
    order_id   UUID PRIMARY KEY,
    user_id    VARCHAR(255) NOT NULL,
    product_id UUID NOT NULL,
    quantity   INT NOT NULL,
    status     VARCHAR(50) NOT NULL,
    trace_id   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_history (
    history_id  UUID PRIMARY KEY,
    order_id    UUID NOT NULL,
    from_status VARCHAR(50),
    to_status   VARCHAR(50) NOT NULL,
    changed_at  TIMESTAMP NOT NULL,
    trace_id    VARCHAR(255) NOT NULL
);
