CREATE TABLE users (
    user_id VARCHAR(64) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(32),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE books (
    isbn VARCHAR(32) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    publisher VARCHAR(255),
    category VARCHAR(100),
    total_count INTEGER NOT NULL DEFAULT 0,
    borrowed_count INTEGER NOT NULL DEFAULT 0,
    location VARCHAR(100),
    keywords VARCHAR(500),
    description VARCHAR(2000),
    borrowable BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_books_counts CHECK (
        total_count >= 0 AND borrowed_count >= 0 AND borrowed_count <= total_count
    )
);

CREATE TABLE borrow_records (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    isbn VARCHAR(32) NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) NOT NULL,
    overdue_warning BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_borrow_book FOREIGN KEY (isbn) REFERENCES books(isbn),
    CONSTRAINT chk_borrow_status CHECK (status IN ('BORROWED', 'RETURNED'))
);

CREATE TABLE wish_list (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    isbn VARCHAR(32) NOT NULL,
    added_date DATE NOT NULL,
    CONSTRAINT fk_wish_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_wish_book FOREIGN KEY (isbn) REFERENCES books(isbn),
    CONSTRAINT uk_wish_user_book UNIQUE (user_id, isbn)
);

CREATE TABLE borrow_relation (
    id BIGSERIAL PRIMARY KEY,
    book_a_isbn VARCHAR(32) NOT NULL,
    book_b_isbn VARCHAR(32) NOT NULL,
    relation_count INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_relation_book_a FOREIGN KEY (book_a_isbn) REFERENCES books(isbn),
    CONSTRAINT fk_relation_book_b FOREIGN KEY (book_b_isbn) REFERENCES books(isbn),
    CONSTRAINT uk_relation_books UNIQUE (book_a_isbn, book_b_isbn)
);

CREATE TABLE chat_history (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    user_question VARCHAR(2000) NOT NULL,
    ai_response VARCHAR(5000),
    response_type VARCHAR(50),
    related_book_title VARCHAR(255),
    related_book_author VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE operation_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(64),
    user_role VARCHAR(32),
    operation VARCHAR(255),
    action VARCHAR(255),
    detail VARCHAR(2000)
);

CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_books_category ON books(category);
CREATE INDEX idx_borrow_user_status ON borrow_records(user_id, status);
CREATE INDEX idx_borrow_isbn_status ON borrow_records(isbn, status);
CREATE INDEX idx_borrow_due_status ON borrow_records(due_date, status);
CREATE INDEX idx_chat_user_created ON chat_history(user_id, created_at DESC);
CREATE INDEX idx_chat_user_session ON chat_history(user_id, session_id, created_at);
CREATE INDEX idx_logs_timestamp ON operation_logs(timestamp DESC);
