-- CREATE TABLE routers
-- (
--     id            UUID PRIMARY KEY,
--     serial_number TEXT UNIQUE NOT NULL,
--     ip_address    INET,
--     last_seen_at  TIMESTAMP,
--     created_at    TIMESTAMP DEFAULT now()
-- );

CREATE TABLE commands
(
    id           UUID PRIMARY KEY,
    router_id    UUID, -- REFERENCES routers (id),
    command_type TEXT NOT NULL,
    payload      JSONB,
    status       TEXT NOT NULL DEFAULT 'PENDING',
    sent_at      TIMESTAMP,
    acked_at     TIMESTAMP,
    created_at   TIMESTAMP     DEFAULT now()
);

-- INSERT INTO routers (id, serial_number, ip_address)
-- VALUES ('550e8400-e29b-41d4-a716-446655440000', 'SN-12345', '192.168.1.1');
