CREATE TABLE t_device
(
    id          UUID        NOT NULL,
    device_id   VARCHAR(26) NOT NULL UNIQUE,
    device_type TEXT,
    created_at  TIMESTAMPTZ,
    meta        TEXT,
    version     VARCHAR(26) NOT NULL DEFAULT '0',
    STATUS      VARCHAR(26) NOT NULL DEFAULT 'READY',
    CONSTRAINT  device_uuid_pk PRIMARY KEY (id)
);
