CREATE TABLE IF NOT EXISTS shops (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    avatar_url VARCHAR(500),
    seller_id BIGINT NOT NULL UNIQUE,  -- ← Убрали REFERENCES users
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shops_seller_id ON shops(seller_id);
