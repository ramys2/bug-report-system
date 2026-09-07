CREATE TABLE user_account (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email_address VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    archived_at DATETIME(6),
    CONSTRAINT chk_user_account_role
        CHECK (role IN ('REPORTER', 'DEVELOPER', 'ADMIN'))
);

CREATE TABLE software_project (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    archived_at DATETIME(6)
);

CREATE TABLE component (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    responsible_user_id CHAR(36) NOT NULL,
    archived_at DATETIME(6),
    CONSTRAINT fk_component_responsible_user
        FOREIGN KEY (responsible_user_id) REFERENCES user_account (id)
);

CREATE TABLE resolution (
    id CHAR(36) PRIMARY KEY,
    description TEXT,
    resolved_at DATETIME(6) NOT NULL,
    fixed_version VARCHAR(100),
    commit_url VARCHAR(2048)
);

CREATE TABLE bug_report (
    id CHAR(36) PRIMARY KEY,
    reporter_id CHAR(36) NOT NULL,
    assignee_id CHAR(36),
    project_id CHAR(36) NOT NULL,
    component_id CHAR(36) NOT NULL,
    resolution_id CHAR(36) UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    steps_to_reproduce TEXT,
    actual_behavior TEXT,
    expected_behavior TEXT,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT chk_bug_report_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_bug_report_status
        CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'NEEDS_INFORMATION',
            'REVIEWING', 'REJECTED', 'CLOSED')),
    CONSTRAINT fk_bug_report_reporter
        FOREIGN KEY (reporter_id) REFERENCES user_account (id),
    CONSTRAINT fk_bug_report_assignee
        FOREIGN KEY (assignee_id) REFERENCES user_account (id),
    CONSTRAINT fk_bug_report_project
        FOREIGN KEY (project_id) REFERENCES software_project (id),
    CONSTRAINT fk_bug_report_component
        FOREIGN KEY (component_id) REFERENCES component (id),
    CONSTRAINT fk_bug_report_resolution
        FOREIGN KEY (resolution_id) REFERENCES resolution (id)
);

CREATE TABLE comment (
    id CHAR(36) PRIMARY KEY,
    bug_report_id CHAR(36) NOT NULL,
    author_id CHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_comment_bug_report
        FOREIGN KEY (bug_report_id) REFERENCES bug_report (id),
    CONSTRAINT fk_comment_author
        FOREIGN KEY (author_id) REFERENCES user_account (id)
);
