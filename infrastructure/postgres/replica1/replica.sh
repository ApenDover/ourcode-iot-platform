#!/bin/bash
set -e

echo "Waiting for master to be ready..."
until pg_isready -h postgres1 -p 5432; do
  sleep 2
done

echo "Master is ready, initializing replica..."

# Удаляем старые данные
rm -rf "$PGDATA"/*

# Получаем копию данных с мастера
PGPASSWORD=strongpassword pg_basebackup -h postgres1 -D "$PGDATA" -U replicator -v -P --wal-method=stream -R -S replica1_slot

# Важно: ключ -R сам создаёт primary_conninfo и standby.signal
# Ключ -C -S создаёт репликационный слот, если его нет (но можно и руками)

chown -R postgres:postgres "$PGDATA"

echo "Replica initialized. Starting PostgreSQL..."
exec docker-entrypoint.sh postgres
