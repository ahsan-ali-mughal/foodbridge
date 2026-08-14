CREATE TABLE pickup_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id BIGINT NOT NULL,
    listing_id VARCHAR(36) NOT NULL,
    volunteer_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMP NULL,
    picked_up_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    failure_reason VARCHAR(255) NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uq_pickup_tasks_claim_id UNIQUE (claim_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pickup_tasks_volunteer_id ON pickup_tasks (volunteer_id);
CREATE INDEX idx_pickup_tasks_status ON pickup_tasks (status);
