ALTER TABLE agent_action_drafts DROP CONSTRAINT fk_agent_action_book;
ALTER TABLE agent_action_drafts DROP CONSTRAINT chk_agent_action_type;
ALTER TABLE agent_action_drafts ADD COLUMN payload_json TEXT;
ALTER TABLE agent_action_drafts ADD CONSTRAINT chk_agent_action_type CHECK (
    action_type IN (
        'RESERVE_BOOK', 'CANCEL_RESERVATION', 'ADD_WISHLIST', 'RENEW_BORROW',
        'ADD_BOOK', 'INCREASE_STOCK', 'REDUCE_STOCK', 'DISABLE_BOOK', 'ENABLE_BOOK', 'DELETE_BOOK'
    )
);

CREATE TABLE agent_catalog_proposals (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(64) NOT NULL,
    queries_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_catalog_proposal_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT chk_catalog_proposal_status CHECK (status IN ('PENDING', 'USED', 'EXPIRED'))
);

CREATE INDEX idx_catalog_proposal_user_status_time
    ON agent_catalog_proposals(user_id, status, created_at DESC);
