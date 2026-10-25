CREATE TABLE support_request (
    id CHAR(36) PRIMARY KEY,
    requester_id CHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL,
    requested_role VARCHAR(32),
    description TEXT,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6),
    -- Only pending role changes participate in the unique key below. For other rows
    -- NULL allows multiple general requests and multiple resolved requests to coexist.
    pending_requested_role VARCHAR(32) GENERATED ALWAYS AS (
        CASE WHEN type = 'ROLE_CHANGE' AND status = 'PENDING' THEN requested_role ELSE NULL END
    ) PERSISTENT,
    CONSTRAINT fk_support_request_requester
        FOREIGN KEY (requester_id) REFERENCES user_account (id),
    CONSTRAINT chk_support_request_type
        CHECK (type IN ('ROLE_CHANGE', 'OTHER')),
    CONSTRAINT chk_support_request_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_support_request_details
        CHECK (
            (type = 'ROLE_CHANGE' AND requested_role IS NOT NULL
                AND requested_role IN ('REPORTER', 'DEVELOPER'))
            OR (type = 'OTHER' AND requested_role IS NULL
                AND description IS NOT NULL AND CHAR_LENGTH(TRIM(description)) > 0)
        ),
    CONSTRAINT uq_support_request_pending_role UNIQUE (requester_id, pending_requested_role)
);
