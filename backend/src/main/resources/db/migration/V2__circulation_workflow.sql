ALTER TABLE users DROP CONSTRAINT chk_users_role;
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'LIBRARIAN', 'ADMIN'));

ALTER TABLE borrow_records ADD COLUMN renew_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE borrow_records ADD CONSTRAINT chk_borrow_renew_count CHECK (renew_count >= 0);

CREATE TABLE book_reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    isbn VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reserved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified_at TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_reservation_book FOREIGN KEY (isbn) REFERENCES books(isbn),
    CONSTRAINT chk_reservation_status CHECK (status IN ('WAITING', 'NOTIFIED', 'COMPLETED', 'CANCELLED', 'EXPIRED'))
);

CREATE TABLE user_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    business_key VARCHAR(160),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT uk_notification_business_key UNIQUE (user_id, business_key)
);

CREATE TABLE agent_action_drafts (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    isbn VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    CONSTRAINT fk_agent_action_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_agent_action_book FOREIGN KEY (isbn) REFERENCES books(isbn),
    CONSTRAINT chk_agent_action_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_agent_action_type CHECK (action_type IN ('RESERVE_BOOK', 'CANCEL_RESERVATION', 'ADD_WISHLIST', 'RENEW_BORROW'))
);

CREATE INDEX idx_reservation_book_status_time ON book_reservations(isbn, status, reserved_at);
CREATE UNIQUE INDEX uk_active_borrow_user_book ON borrow_records(user_id, isbn) WHERE status = 'BORROWED';
CREATE INDEX idx_reservation_user_time ON book_reservations(user_id, reserved_at DESC);
CREATE INDEX idx_notification_user_read_time ON user_notifications(user_id, is_read, created_at DESC);
CREATE INDEX idx_agent_action_user_status_time ON agent_action_drafts(user_id, status, created_at DESC);
