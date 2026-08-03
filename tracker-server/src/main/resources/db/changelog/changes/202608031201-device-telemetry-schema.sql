--liquibase formatted sql

--changeset workpulsetracker:202608031201-device-telemetry-schema

CREATE TABLE device (
    id            BIGSERIAL PRIMARY KEY,
    worker_id     BIGINT       NOT NULL,
    hardware_id   VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255),
    last_seen_at  TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    CONSTRAINT fk_device_worker
        FOREIGN KEY (worker_id) REFERENCES worker (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_device_worker_hardware_id ON device (worker_id, hardware_id);

CREATE INDEX ix_device_worker_id ON device (worker_id);

CREATE TABLE app_catalog (
    id             BIGSERIAL PRIMARY KEY,
    app_identifier VARCHAR(512) NOT NULL,
    display_name   VARCHAR(512) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX ux_app_catalog_app_identifier ON app_catalog (LOWER(app_identifier));

CREATE TABLE app_runtime_stat (
    id               BIGSERIAL PRIMARY KEY,
    worker_id        BIGINT       NOT NULL,
    device_id        BIGINT       NOT NULL,
    app_identifier   VARCHAR(512) NOT NULL,
    total_seconds    BIGINT       NOT NULL DEFAULT 0,
    last_agent_value BIGINT       NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_app_runtime_stat_worker
        FOREIGN KEY (worker_id) REFERENCES worker (id) ON DELETE CASCADE,
    CONSTRAINT fk_app_runtime_stat_device
        FOREIGN KEY (device_id) REFERENCES device (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_app_runtime_stat_worker_device_app
    ON app_runtime_stat (worker_id, device_id, LOWER(app_identifier));

CREATE INDEX ix_app_runtime_stat_worker_id ON app_runtime_stat (worker_id);
