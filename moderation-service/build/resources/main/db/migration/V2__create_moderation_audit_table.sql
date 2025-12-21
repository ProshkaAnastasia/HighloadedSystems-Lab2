CREATE TABLE IF NOT EXISTS moderation_audit (
    id BIGSERIAL PRIMARY KEY,
    action_id BIGINT NOT NULL REFERENCES moderation_actions(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    moderator_id BIGINT NOT NULL,
    old_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_audit_action FOREIGN KEY(action_id) REFERENCES moderation_actions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_moderation_audit_product_id ON moderation_audit(product_id);
CREATE INDEX IF NOT EXISTS idx_moderation_audit_moderator_id ON moderation_audit(moderator_id);
CREATE INDEX IF NOT EXISTS idx_moderation_audit_created_at ON moderation_audit(created_at);
