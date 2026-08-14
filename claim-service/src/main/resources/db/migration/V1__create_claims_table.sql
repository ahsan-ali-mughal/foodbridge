CREATE TABLE claims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id VARCHAR(36) NOT NULL,
    ngo_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    claimed_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uq_claims_listing_id UNIQUE (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_claims_ngo_id ON claims (ngo_id);
CREATE INDEX idx_claims_status ON claims (status);
