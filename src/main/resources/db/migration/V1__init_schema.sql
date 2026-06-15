-- ─────────────────────────────────────────────────────────────────────────────
-- V1 — Initial EDAP schema
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS teams (
    id          BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    description VARCHAR(500),
    slack_channel VARCHAR(255),
    email       VARCHAR(200),
    created_at  TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_team_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS components (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    type            VARCHAR(100)    NOT NULL,
    owner           VARCHAR(200),
    team_id         BIGINT,
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    description     VARCHAR(1000),
    repository_url  VARCHAR(500),
    metadata        JSON,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_component_name   (name),
    INDEX idx_component_team   (team_id),
    INDEX idx_component_status (status),
    INDEX idx_component_type   (type),
    CONSTRAINT fk_component_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_users (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100)  NOT NULL,
    email         VARCHAR(200),
    password_hash VARCHAR(255)  NOT NULL,
    enabled       TINYINT(1)    NOT NULL DEFAULT 1,
    created_at    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT      NOT NULL,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Seed data ───────────────────────────────────────────────────────────────
-- Passwords are bcrypt hashes (cost 12).
-- admin / Admin@123
-- engineer / Engineer@123
-- viewer  / Viewer@123

INSERT INTO app_users (username, email, password_hash, enabled) VALUES
('admin',    'admin@edap.internal',    '$2a$12$K7tyKYCYEQFmZzV5VLNmCeYE4ckz9t1n2IqKPCgJPlqy1g0vAaRLm', 1),
('engineer', 'engineer@edap.internal', '$2a$12$3HvuNtgEFM3GXbrLCOIKHOG5y.K5/FkjYn8XKzfDQpBxFjKcDj2tW', 1),
('viewer',   'viewer@edap.internal',   '$2a$12$lh7MBs2nR/UE9JT.i1lCc.rW3nqMdULVxuaQhxvdFLGp0NG5jt4nS', 1);

INSERT INTO user_roles (user_id, role) VALUES
(1, 'ROLE_ADMIN'),
(1, 'ROLE_ENGINEER'),
(1, 'ROLE_VIEWER'),
(2, 'ROLE_ENGINEER'),
(2, 'ROLE_VIEWER'),
(3, 'ROLE_VIEWER');

-- Seed teams
INSERT INTO teams (name, description, slack_channel, email) VALUES
('platform',   'Core platform engineering team',        '#platform-eng',   'platform@edap.internal'),
('data',       'Data engineering and pipelines team',   '#data-eng',       'data@edap.internal'),
('frontend',   'Frontend and mobile engineering team',  '#frontend-eng',   'frontend@edap.internal'),
('security',   'Security and compliance team',          '#security-eng',   'security@edap.internal');

-- Seed components
INSERT INTO components (name, type, owner, team_id, status, description, repository_url, metadata) VALUES
('api-gateway',          'GATEWAY',  'alice@edap.internal', 1, 'ACTIVE',
 'Primary API gateway for all inbound traffic',
 'https://github.com/edap/api-gateway',
 '{"language":"Java","runtime":"JVM17","sla":"99.99%"}'),

('user-service',         'SERVICE',  'bob@edap.internal',   1, 'ACTIVE',
 'Manages user accounts and authentication',
 'https://github.com/edap/user-service',
 '{"language":"Java","runtime":"JVM17","sla":"99.9%"}'),

('component-registry',   'SERVICE',  'carol@edap.internal', 1, 'ACTIVE',
 'Core engineering data access platform registry service',
 'https://github.com/edap/component-registry',
 '{"language":"Java","runtime":"JVM17","sla":"99.95%"}'),

('postgres-primary',     'DATABASE', 'dave@edap.internal',  2, 'ACTIVE',
 'Primary PostgreSQL cluster',
 NULL,
 '{"engine":"PostgreSQL 15","tier":"prod","storage":"2TB"}'),

('kafka-cluster',        'QUEUE',    'eve@edap.internal',   2, 'ACTIVE',
 'Central Kafka event streaming cluster',
 NULL,
 '{"version":"3.6","partitions":"120","replication":"3"}'),

('analytics-pipeline',   'WORKER',   'frank@edap.internal', 2, 'ACTIVE',
 'Batch + streaming analytics pipeline',
 'https://github.com/edap/analytics-pipeline',
 '{"language":"Python","runtime":"3.11","schedule":"hourly"}'),

('web-app',              'FRONTEND', 'grace@edap.internal', 3, 'ACTIVE',
 'React single-page application',
 'https://github.com/edap/web-app',
 '{"framework":"React 18","node":"20"}'),

('redis-cache',          'CACHE',    'henry@edap.internal', 1, 'ACTIVE',
 'Distributed Redis cache layer',
 NULL,
 '{"version":"7.2","mode":"cluster","maxmemory":"16gb"}'),

('notification-service', 'SERVICE',  'irene@edap.internal', 1, 'ACTIVE',
 'Email and push notification delivery service',
 'https://github.com/edap/notification-service',
 '{"language":"Go","runtime":"1.21"}'),

('auth-lib',             'LIBRARY',  'bob@edap.internal',   4, 'ACTIVE',
 'Shared authentication and authorisation library',
 'https://github.com/edap/auth-lib',
 '{"language":"Java","version":"2.4.1"}');
