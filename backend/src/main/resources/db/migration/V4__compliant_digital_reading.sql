CREATE TABLE ebook_resources (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(32) NOT NULL UNIQUE,
    rights_status VARCHAR(30) NOT NULL,
    source_name VARCHAR(100) NOT NULL,
    source_url VARCHAR(500) NOT NULL,
    source_page_pattern VARCHAR(255) NOT NULL,
    license_name VARCHAR(100) NOT NULL,
    license_url VARCHAR(500) NOT NULL,
    jurisdiction VARCHAR(100) NOT NULL,
    chapter_count INTEGER NOT NULL,
    author_death_year INTEGER NOT NULL,
    first_publication_year INTEGER NOT NULL,
    rights_evidence VARCHAR(2000) NOT NULL,
    content_notice VARCHAR(1000) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ebook_book FOREIGN KEY (isbn) REFERENCES books(isbn) ON DELETE CASCADE,
    CONSTRAINT chk_ebook_rights_status CHECK (
        rights_status IN ('PUBLIC_DOMAIN_VERIFIED', 'LICENSED_VERIFIED', 'PREVIEW_ONLY', 'UNVERIFIED')
    ),
    CONSTRAINT chk_ebook_chapter_count CHECK (chapter_count > 0),
    CONSTRAINT chk_ebook_publish_gate CHECK (
        published = FALSE OR (
            rights_status IN ('PUBLIC_DOMAIN_VERIFIED', 'LICENSED_VERIFIED')
            AND LENGTH(TRIM(source_name)) > 0
            AND LENGTH(TRIM(source_url)) > 0
            AND LENGTH(TRIM(license_name)) > 0
            AND LENGTH(TRIM(license_url)) > 0
            AND LENGTH(TRIM(rights_evidence)) > 0
            AND LENGTH(TRIM(content_notice)) > 0
        )
    )
);

CREATE TABLE reading_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    resource_id BIGINT NOT NULL,
    chapter_number INTEGER NOT NULL,
    scroll_percent INTEGER NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reading_progress_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_progress_resource FOREIGN KEY (resource_id) REFERENCES ebook_resources(id) ON DELETE CASCADE,
    CONSTRAINT uk_reading_progress_user_resource UNIQUE (user_id, resource_id),
    CONSTRAINT chk_reading_progress_chapter CHECK (chapter_number >= 1),
    CONSTRAINT chk_reading_progress_scroll CHECK (scroll_percent BETWEEN 0 AND 100)
);

CREATE INDEX idx_ebook_published ON ebook_resources(published, isbn);
CREATE INDEX idx_reading_progress_user_time ON reading_progress(user_id, updated_at DESC);
