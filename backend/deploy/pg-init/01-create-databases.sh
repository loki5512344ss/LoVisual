#!/bin/sh
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    SELECT 'CREATE DATABASE accounts_db OWNER lovisual' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'accounts_db')\gexec
    SELECT 'CREATE DATABASE configs_db OWNER lovisual' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'configs_db')\gexec
EOSQL
