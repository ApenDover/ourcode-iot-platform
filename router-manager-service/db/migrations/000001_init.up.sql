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
    created_at   TIMESTAMP     DEFAULT now()
);

CREATE INDEX idx_commands_status ON commands(status);

CREATE INDEX idx_commands_id_status ON commands(id, status);

CREATE INDEX idx_commands_status_sent_at ON commands(status, sent_at);

CREATE INDEX idx_commands_status_router ON commands(status, router_id)
    INCLUDE (command_type, payload, created_at);

CREATE INDEX idx_commands_router_status ON commands(router_id, status);


