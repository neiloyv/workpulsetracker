--liquibase formatted sql

--changeset workpulsetracker:202608031200-init-schema

CREATE TABLE organization (
    id         BIGSERIAL PRIMARY KEY,
    type       VARCHAR(32)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(32)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE subscription (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL,
    plan            VARCHAR(64)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    starts_at       TIMESTAMPTZ  NOT NULL,
    ends_at         TIMESTAMPTZ,
    max_persons     INT          NOT NULL,
    max_branches    INT          NOT NULL,
    CONSTRAINT fk_subscription_organization
        FOREIGN KEY (organization_id) REFERENCES organization (id) ON DELETE CASCADE
);

CREATE TABLE branch (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL,
    name            VARCHAR(255) NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_branch_organization
        FOREIGN KEY (organization_id) REFERENCES organization (id) ON DELETE CASCADE
);

CREATE TABLE department (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL,
    branch_id       BIGINT       NOT NULL,
    name            VARCHAR(255) NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_department_organization
        FOREIGN KEY (organization_id) REFERENCES organization (id) ON DELETE CASCADE,
    CONSTRAINT fk_department_branch
        FOREIGN KEY (branch_id) REFERENCES branch (id) ON DELETE CASCADE
);

CREATE TABLE worker (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT       NOT NULL,
    branch_id         BIGINT       NOT NULL,
    department_id     BIGINT       NOT NULL,
    display_name      VARCHAR(255) NOT NULL,
    email             VARCHAR(320) NOT NULL,
    access_key        VARCHAR(128) NOT NULL,
    access_key_prefix VARCHAR(32)  NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    agent_installed   BOOLEAN      NOT NULL DEFAULT FALSE,
    agent_version     VARCHAR(32),
    created_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_worker_organization
        FOREIGN KEY (organization_id) REFERENCES organization (id) ON DELETE CASCADE,
    CONSTRAINT fk_worker_branch
        FOREIGN KEY (branch_id) REFERENCES branch (id),
    CONSTRAINT fk_worker_department
        FOREIGN KEY (department_id) REFERENCES department (id)
);

CREATE UNIQUE INDEX ux_worker_email_org ON worker (organization_id, LOWER(email));

CREATE TABLE user_account (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL,
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    worker_id       BIGINT,
    status          VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_user_account_organization
        FOREIGN KEY (organization_id) REFERENCES organization (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_account_worker
        FOREIGN KEY (worker_id) REFERENCES worker (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX ux_user_account_email ON user_account (LOWER(email));

CREATE TABLE platform_admin (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX ux_platform_admin_email ON platform_admin (LOWER(email));

CREATE TABLE organization_setting (
    organization_id BIGINT       NOT NULL,
    setting_key     VARCHAR(128) NOT NULL,
    setting_value   TEXT         NOT NULL,
    CONSTRAINT pk_organization_setting PRIMARY KEY (organization_id, setting_key),
    CONSTRAINT fk_organization_setting_organization
        FOREIGN KEY (organization_id) REFERENCES organization (id) ON DELETE CASCADE
);

CREATE TABLE activity_sample (
    id            BIGSERIAL PRIMARY KEY,
    worker_id     BIGINT       NOT NULL,
    app_name      VARCHAR(255) NOT NULL,
    seconds       BIGINT       NOT NULL,
    idle          BOOLEAN      NOT NULL DEFAULT FALSE,
    activity_date DATE         NOT NULL,
    CONSTRAINT fk_activity_sample_worker
        FOREIGN KEY (worker_id) REFERENCES worker (id) ON DELETE CASCADE
);

CREATE INDEX ix_activity_sample_worker_date ON activity_sample (worker_id, activity_date);
