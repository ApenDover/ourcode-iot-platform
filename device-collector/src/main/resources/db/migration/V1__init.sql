CREATE TABLE t_device
(
    id          UUID        NOT NULL,
    device_id   VARCHAR(26) NOT NULL UNIQUE,
    device_type TEXT,
    created_at  TIMESTAMPTZ,
    meta        TEXT,
    CONSTRAINT device_uuid_pk PRIMARY KEY (id)
);
