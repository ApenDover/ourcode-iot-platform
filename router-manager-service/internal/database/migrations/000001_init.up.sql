CREATE TABLE routers
(
    id            UUID PRIMARY KEY,
    serial_number TEXT UNIQUE NOT NULL,
    ip_address    INET,
    last_seen_at  TIMESTAMP,
    created_at    TIMESTAMP DEFAULT now()
);

CREATE TABLE commands
(
    id           UUID PRIMARY KEY,
    router_id    UUID REFERENCES routers (id),
    command_type TEXT NOT NULL,
    payload      JSONB,
    status       TEXT NOT NULL DEFAULT 'PENDING',
    sent_at      TIMESTAMP,
    acked_at     TIMESTAMP,
    created_at   TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_commands_router_status_created
    ON commands(router_id, status);


