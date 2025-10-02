CREATE KEYSPACE IF NOT EXISTS iot_service
    WITH replication = {
        'class': 'SimpleStrategy',
        'replication_factor': '1'
        };

CREATE TABLE IF NOT EXISTS iot_service.device_events
(
    device_id text,
    timestamp bigint,
    event_id  uuid,
    payload   text,
    type      text,
    PRIMARY KEY ((device_id), timestamp, event_id)
);

INSERT INTO iot_service.device_events (device_id, event_id, timestamp, payload, type)
VALUES ('device-1', 550e8400-e29b-41d4-a716-446655440000, toTimestamp(now()), '{"temp":22.5,"unit":"C"}', 'TEMPERATURE');

INSERT INTO iot_service.device_events (device_id, event_id, timestamp, payload, type)
VALUES ('device-1', 550e8400-e29b-41d4-a716-446655440001, toTimestamp(now()), '{"status":"online"}', 'STATUS');

INSERT INTO iot_service.device_events (device_id, event_id, timestamp, payload, type)
VALUES ('device-2', 550e8400-e29b-41d4-a716-446655440002, 1759425954036, '{"error":"low_battery"}', 'HUMIDITY');

INSERT INTO iot_service.device_events (device_id, event_id, timestamp, payload, type)
VALUES ('device-2', 550e8400-e29b-41d4-a716-446655440003, 1759425954536, '{"temp":18.7,"unit":"C"}', 'TEMPERATURE');

INSERT INTO iot_service.device_events (device_id, event_id, timestamp, payload, type)
VALUES ('device-2', 550e8400-e29b-41d4-a716-446655440003, 1759425954936, '{"temp":18.7,"unit":"C"}', 'TEMPERATURE');

INSERT INTO iot_service.device_events (device_id, event_id, timestamp, payload, type)
VALUES ('device-3', 550e8400-e29b-41d4-a716-446655440004, toTimestamp(now()), '{"status":"offline"}', 'STATUS');
