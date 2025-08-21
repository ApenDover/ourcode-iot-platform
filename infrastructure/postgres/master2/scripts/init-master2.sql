CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'strongpassword';
SELECT * FROM pg_create_physical_replication_slot('replica2_slot')
WHERE NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = 'replica2_slot');
